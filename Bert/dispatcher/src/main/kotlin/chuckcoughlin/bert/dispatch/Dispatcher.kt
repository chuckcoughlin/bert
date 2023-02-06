/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.PathConstants
import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.controller.SocketController
import chuckcoughlin.bert.common.controller.SocketStateChangeEvent
import chuckcoughlin.bert.common.message.*
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.common.util.LoggerUtility
import chuckcoughlin.bert.common.util.ShutdownHook
import chuckcoughlin.bert.control.controller.QueueName
import chuckcoughlin.bert.control.message.InternalMessageHolder
import chuckcoughlin.bert.control.solver.Solver
import chuckcoughlin.bert.motor.controller.MotorGroupController
import chuckcoughlin.bert.sql.db.Database
import java.io.IOException
import java.nio.file.Paths
import java.time.LocalDate
import java.time.Month
import java.time.Period
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.contracts.InvocationKind

/**
 * The Dispatcher is the core compute hub of the application. It's job is to accept requests from
 * the various peripheral controllers, distribute them to the motor manager channels and post the results.
 * For complicated requests it may invoke the services of the "Solver" and insert
 * internal intermediate requests.
 *
 * For the peripheral controllers, the dispatcher presents itself as a controller, but different
 * channels apply to the different peripherals.
 */
class Dispatcher(m: RobotModel, s: Solver, mgc: MotorGroupController) : Controller {
    private val WEIGHT = 0.5 // weighting to give previous in EWMA
    private val model: RobotModel
    private var commandController: Controller
    private var terminalController: Controller
    private var internalController: Controller
    private var motorGroupController: Controller
    private val busy: Condition
    private var currentRequest: MessageBottle
    private val lock: Lock
    private val solver: Solver
    private var cadence = 1000 // msecs
    private var cycleCount = 0 // messages processed
    private var cycleTime = 0.0 // msecs,    EWMA
    private var dutyCycle = 0.0 // fraction, EWMA

    /**
     * Constructor:
     * @param m the server model
     */
    init {
        model = m
        lock = ReentrantLock()
        busy = lock.newCondition()
        motorGroupController = mgc
        solver = s
        name = model.getProperty(ConfigurationConstants.PROPERTY_ROBOT_NAME, "Bert")
        val cadenceString: String = model.getProperty(ConfigurationConstants.PROPERTY_CADENCE, "1000") // ~msecs
        try {
            cadence = cadenceString.toInt()
        }
        catch (nfe: NumberFormatException) {
            LOGGER.warning(String.format("%s.constructor: Cadence must be an integer (%s)", CLSS, nfe.localizedMessage))
        }
        LOGGER.info(String.format("%s: started with cadence %d msecs", CLSS, cadence))
    }

    /**
     * The server creates controllers for the Terminal and Command sockets, and a controller
     * for repeating requests (on a timer). The motor group controller has its own model and is already
     * instantiated at this point..
     *
     * The internal controller is used for those instances where multiple or repeating messages are
     * required for a single user request.
     */
    fun createControllers() {
        val sockets: Map<String, Int> = model.getSockets()
        val walker = sockets.keys.iterator()
        while (walker.hasNext()) {
            val key = walker.next()
            val type: ControllerType = ControllerType.valueOf(model.getHandlerTypes().get(key))
            val port = sockets[key]!!
            if (type.equals(ControllerType.COMMAND)) {
                commandController = SocketController(this, type.name(), port)
                commandController.addChangeListener(this)
                LOGGER.info(String.format("%s: created command controller", CLSS))
            }
            else if (type.equals(ControllerType.TERMINAL)) {
                terminalController = SocketController(this, type.name, port)
                terminalController.addChangeListener(this)
                LOGGER.info(String.format("%s: created terminal controller", CLSS))
            }
        }
        internalController = InternalController(this)
    }

    val controllerName: String
        get() = model.getProperty(ConfigurationConstants.PROPERTY_CONTROLLER_NAME, "launcher")

    /**
     * On startup, initiate a short message sequence to bring the robot into a sane state.
     */
    fun startup() {
        if (commandController != null) commandController.start()
        if (terminalController != null) terminalController.start()
        if (internalController != null) internalController.start()
        if (motorGroupController != null) {
            LOGGER.info(String.format("%s.execute: starting motorGroupController", CLSS))
            motorGroupController.initialize()
            motorGroupController.start()

            // Set the speed to "normal" rate. Delay to all startup to complete
            var msg = MessageBottle(RequestType.SET_POSE)
            msg.setProperty(PropertyType.POSE_NAME, BottleConstants.POSE_NORMAL_SPEED)
            msg.assignSource(ControllerType.BITBUCKET.name())
            var holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            holder.setDelay(500) // 1/2 sec delay
            internalController.receiveRequest(holder)
            // Read all the joint positions, one controller at a time
            msg = MessageBottle(RequestType.LIST_MOTOR_PROPERTY)
            msg.setProperty(PropertyType.PROPERTY_NAME, JointProperty.POSITION.name())
            msg.setProperty(PropertyType.CONTROLLER_NAME, BottleConstants.CONTROLLER_LOWER)
            msg.assignSource(ControllerType.BITBUCKET.name())
            holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            holder.setDelay(1000) // 1 sec delay
            internalController.receiveRequest(holder)
            msg = MessageBottle(RequestType.LIST_MOTOR_PROPERTY)
            msg.setProperty(PropertyType.PROPERTY_NAME, JointProperty.POSITION.name())
            msg.setProperty(PropertyType.CONTROLLER_NAME, BottleConstants.CONTROLLER_UPPER)
            msg.assignSource(ControllerType.BITBUCKET.name())
            holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            holder.setDelay(1000) // 1 sec delay
            internalController.receiveRequest(holder)
            // Bring any joints that are outside sane limits into compliance
            msg = MessageBottle(RequestType.INITIALIZE_JOINTS)
            msg.assignSource(ControllerType.BITBUCKET.name())
            holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            holder.setDelay(2000) // 2 sec delay
            internalController.receiveRequest(holder)
        }
        LOGGER.info(String.format("%s.execute: startup complete.", CLSS))
    }

    fun shutdown() {
        motorGroupController.stop()
        LOGGER.info(String.format("%s.shutdown: Shutting down 1 ...", CLSS))
        if (commandController != null) commandController.stop()
        LOGGER.info(String.format("%s.shutdown: Shutting down 2 ...", CLSS))
        if (terminalController != null) terminalController.stop()
        LOGGER.info(String.format("%s.shutdown: Shutting down 3 ...", CLSS))
        if (internalController != null) internalController.stop()
        LOGGER.info(String.format("%s.shutdown: Shutting down 4 ...", CLSS))
        if (motorGroupController != null) motorGroupController.stop()
        LOGGER.info(String.format("%s.shutdown: complete.", CLSS))
    }

    /**
     * Loop forever processing whatever arrives from the various controllers. Each request is
     * handled atomically. There is no interleaving.
     */
    override fun run() {
        //LOGGER.info(String.format("%s: Starting run loop ...",CLSS));
        try {
            while (true) {
                lock.lock()
                try {
                    LOGGER.info(String.format("%s: Entering wait for cycle %d ...", CLSS, cycleCount))
                    busy.await()
                    val startCycle = System.currentTimeMillis()
                    LOGGER.info(String.format("%s: Cycle %d ...", CLSS, cycleCount))
                    if (currentRequest == null) break // Causes shutdown
                    if (currentRequest != null) {
                        // "internal" requests are those that need to be queued on the internal controller
                        if (isInternalRequest(currentRequest)) {
                            val response: MessageBottle = handleInternalRequest(currentRequest)
                            handleResponse(response)
                        } else if (isLocalRequest(currentRequest)) {
                            // Handle local request -create response
                            val response: MessageBottle = handleLocalRequest(currentRequest)
                            handleResponse(response)
                        } else {
                            // Handle motor request. The controller forwards response here via "handleResponse".
                            motorGroupController.processRequest(currentRequest)
                        }
                    }
                    LOGGER.info(String.format("%s: Cycle %d complete.", CLSS, cycleCount))
                    cycleCount++
                    val endCycle = System.currentTimeMillis()
                    val elapsed = endCycle - startCycle
                    cycleTime = exponentiallyWeightedMovingAverage(cycleTime, elapsed.toDouble())
                    dutyCycle = exponentiallyWeightedMovingAverage(dutyCycle, elapsed.toDouble() / cadence)
                }
                catch (ie: InterruptedException) {
                }
                finally {
                    lock.unlock()
                }
            }
        }
        catch (ex: Exception) {
            ex.printStackTrace()
        }
        finally {
            shutdown()
        }
        System.exit(0)
    }

    /**
     * We've gotten a request. It may have come from either a socket
     * or the timer. Signal the busy lock, so main loop proceeds.
     */
    @Synchronized
    fun handleRequest(request: MessageBottle) {
        lock.lock()
        LOGGER.info(String.format(
                "%s.handleRequest: Processing %s from %s",
                CLSS,
                request.fetchRequestType().name(),
                request.fetchSource()
            )
        )
        try {
            currentRequest = request
            busy.signal()
        } finally {
            lock.unlock()
        }
    }

    /**
     * This is called by "sub-controllers" - Internal and MotorGroup. Forward the response on to
     * the appropriate socket controller for transmission to the original source of the request.
     * Note: Notifications are broadcast to all live controllers.
     */
    fun handleResponse(response: MessageBottle) {
        val source: String = response.fetchSource()
        LOGGER.info(
            java.lang.String.format(
                "%s.handleResponse: Received response %s for %s",
                CLSS,
                response.fetchRequestType().name(),
                source
            )
        )
        if (source.equals(ControllerType.COMMAND.name(), ignoreCase = true)) {
            commandController.receiveResponse(response)
        } else if (source.equals(ControllerType.TERMINAL.name(), ignoreCase = true)) {
            terminalController.receiveResponse(response)
        } else if (source.equals(ControllerType.DISPATCHER.name(), ignoreCase = true)) {
            commandController.receiveResponse(response)
            terminalController.receiveResponse(response)
        } else if (source.equals(ControllerType.INTERNAL.name(), ignoreCase = true)) {
            internalController.receiveResponse(response)
        } else if (source.equals(ControllerType.BITBUCKET.name(), ignoreCase = true)) {
            // Do nothing, end of the line
        } else {
            LOGGER.warning(String.format("%s.handleResponse: Unknown destination - %s, ignored", CLSS, source))
        }
    }

    // The response is simply the request. A generic acknowledgement will be relayed to the user.
    // 1) Freezing a joint requires getting the motor position first to update the internal status dictionary
    private fun handleInternalRequest(request: MessageBottle): MessageBottle {
        // Read the current motor positions, then freeze.
        val properties: Map<String, String> = request.getProperties()
        // Entire robot
        if (request.fetchRequestType().equals(RequestType.COMMAND) &&
            properties[BottleConstants.COMMAND_NAME].equals(BottleConstants.COMMAND_FREEZE, ignoreCase = true)
        ) {
            val msg = MessageBottle(RequestType.LIST_MOTOR_PROPERTY)
            msg.setProperty(PropertyType.PROPERTY_NAME, JointProperty.POSITION.name())
            msg.assignSource(ControllerType.BITBUCKET.name())
            var holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            internalController.receiveRequest(holder)
            holder = InternalMessageHolder(request, QueueName.GLOBAL)
            holder.setDelay(1000) // 1 sec delay
            internalController.receiveRequest(holder)
        } else if (request.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY) &&
            properties[PropertyType.PROPERTY_NAME].equals(JointProperty.STATE.name(), ignoreCase = true) &&
            properties[JointProperty.STATE.name()].equals(BottleConstants.ON_VALUE, ignoreCase = true)
        ) {
            val msg = MessageBottle(RequestType.LIST_MOTOR_PROPERTY)
            msg.setProperty(PropertyType.PROPERTY_NAME, JointProperty.POSITION.name())
            msg.setProperty(
                BottleConstants.LIMB_NAME,
                request.getProperty(BottleConstants.LIMB_NAME, InvocationKind.UNKNOWN.name())
            )
            msg.assignSource(ControllerType.BITBUCKET.name())
            var holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            internalController.receiveRequest(holder)
            holder = InternalMessageHolder(request, QueueName.GLOBAL)
            holder.setDelay(500) // 1/2 sec delay
            internalController.receiveRequest(holder)
        } else if (request.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY) &&
            properties[PropertyType.PROPERTY_NAME].equals(JointProperty.STATE.name(), ignoreCase = true) &&
            properties[JointProperty.STATE.name()].equals(BottleConstants.ON_VALUE, ignoreCase = true)
        ) {
            val msg = MessageBottle(RequestType.GET_MOTOR_PROPERTY)
            msg.setProperty(PropertyType.PROPERTY_NAME, JointProperty.POSITION.name())
            msg.setProperty(
                BottleConstants.JOINT_NAME,
                request.getProperty(BottleConstants.JOINT_NAME, InvocationKind.UNKNOWN.name())
            )
            msg.assignSource(ControllerType.BITBUCKET.name())
            var holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            internalController.receiveRequest(holder)
            holder = InternalMessageHolder(request, QueueName.GLOBAL)
            holder.setDelay(250) // 1/4 sec delay
            internalController.receiveRequest(holder)
        }
        return request
    }

    // Create a response for a request that can be handled immediately. The response is simply the original request
    // with some text to return back to the user. 
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        // The following two requests simply use the current positions of the motors, whatever they are
        if (request.type.equals(RequestType.GET_APPENDAGE_LOCATION)) {
            solver.setTreeState() // Forces new calculations
            val appendageName: String = request.getProperty(BottleConstants.APPENDAGE_NAME, InvocationKind.UNKNOWN.name())
            val xyz: DoubleArray = solver.getPosition(Appendage.valueOf(appendageName))
            val text = String.format(
                "%s is located at %0.2f %0.2f %0.2f meters",
                appendageName.lowercase(Locale.getDefault()),
                xyz[0],
                xyz[1],
                xyz[2]
            )
            request.assignText(text)
        }
        else if (request.fetchRequestType().equals(RequestType.GET_JOINT_LOCATION)) {
            solver.setTreeState()
            val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, InvocationKind.UNKNOWN.name())
            // Choose any one of the links attached to the joint, get its parent
            val joint: Joint = Joint.valueOf(jointName.uppercase(Locale.getDefault()))
            val xyz: DoubleArray = solver.getPosition(joint)
            val text = String.format(
                "The center of joint %s is located at %0.2f %0.2f %0.2f meters",
                jointName.lowercase(Locale.getDefault()),
                xyz[0],
                xyz[1],
                xyz[2]
            )
            request.text = text
        }
        else if (request.fetchRequestType().equals(RequestType.GET_METRIC)) {
            val metric: MetricType = MetricType.valueOf(request.getProperty(BottleConstants.METRIC_NAME, "NAME"))
            var text = ""
            when (metric) {
                MetricType.AGE -> {
                    val today = LocalDate.now()
                    val birthday: LocalDate = LocalDate.of(2019, Month.JANUARY, 1)
                    val p = Period.between(birthday, today)
                    text = "I am " + p.years + " years, " + p.months +
                            " months, and " + p.days + " days old"
                }

                MetricType.CADENCE -> text = "The cadence is " + cadence + " milliseconds"
                MetricType.CYCLECOUNT -> text = "I've processed " + cycleCount + " requests"
                MetricType.CYCLETIME -> text = "The average cycle time is " + cycleTime.toInt() + " milliseconds"
                MetricType.DUTYCYCLE -> text = "My average duty cycle is " + (100.0 * dutyCycle).toInt() + " percent"
                HEIGHT -> text = "My height when standing is 83 centimeters"
                MetricType.MITTENS -> text = selectRandomText(mittenPhrases)
                NAME -> text = "My name is $name"
            }
            request.text = text
        }
        else if (request.type.equals(RequestType.COMMAND)) {
            val command: String = request.getProperty(BottleConstants.COMMAND_NAME, "NONE")
            LOGGER.warning(String.format("%s.handleLocalRequest: command=%s", CLSS, command))
            if (command.equals(BottleConstants.COMMAND_HALT, ignoreCase = true)) {
                System.exit(0) // Rely on ShutdownHandler cleanup connections
            }
            else if (command.equals(BottleConstants.COMMAND_SHUTDOWN, ignoreCase = true)) {
                try {
                    val rt = Runtime.getRuntime()
                    rt.exec("sudo poweroff")
                } catch (ioe: IOException) {
                    LOGGER.warning(
                        String.format(
                            "%s.createResponseForLocalRequest: Powerdown error (%s)",
                            CLSS,
                            ioe.message
                        )
                    )
                }
            } else {
                val msg = String.format("Unrecognized command: %s", command)
                request.assignError(msg)
            }
        }
        else if (request.type.equals(RequestType.MAP_POSE)) {
            val commandName: String = request.getProperty(BottleConstants.COMMAND_NAME, "")
            val poseName: String = request.getProperty(PropertyType.POSE_NAME, "")
            if (!commandName.isEmpty() && !poseName.isEmpty()) {
                Database.getInstance().mapCommandToPose(commandName, poseName)
            }
            else {
                request.assignError("I could not map because either command or pose is empty")
            }
        }
        else if (request.type.equals(RequestType.SAVE_POSE)) {
            var poseName: String = request.getProperty(PropertyType.POSE_NAME, "")
            if (!poseName.isEmpty()) {
                Database.getInstance().saveJointPositionsForPose(model.getMotors(), poseName)
            } else {
                poseName = Database.getInstance().saveJointPositionsAsNewPose(model.getMotors())
                request.assignText("I saved the pose as $poseName")
            }
        }
        return request
    }

    private fun exponentiallyWeightedMovingAverage(
        currentValue: Double,
        previousValue: Double
    ): Double {
        return (1.0 - WEIGHT) * currentValue + WEIGHT * previousValue
    }

    // These are complex requests that require that several messages be created and processed
    // on the internal controller. Categories include:
    //   1) Anything that sets "torque enable" to true. This action requires that we read
    //      and save (in memory) current motor positions.
    private fun isInternalRequest(request: MessageBottle): Boolean {
        // Never send a request launched by the internal controller back to it. That would be an infinite loop
        if (request.fetchSource().equalsIgnoreCase(ControllerType.INTERNAL.name())) return false
        val properties: Map<String, String> = request.getProperties()
        if (request.type.equals(RequestType.COMMAND) &&
            properties[BottleConstants.COMMAND_NAME].equals(BottleConstants.COMMAND_FREEZE, ignoreCase = true)
        ) {
            return true
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY) &&
            properties[PropertyType.PROPERTY_NAME].equals(JointProperty.STATE.name(), ignoreCase = true) &&
            properties[JointProperty.STATE.name()].equals(BottleConstants.ON_VALUE, ignoreCase = true)
        ) {
            return true
        }
        else if (request.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY) &&
            properties[PropertyType.PROPERTY_NAME].equals(JointProperty.STATE.name(), ignoreCase = true) &&
            properties[JointProperty.STATE.name()].equals(BottleConstants.ON_VALUE, ignoreCase = true)
        ) {
            return true
        }
        return false
    }

    // Local requests are those that can be handled immediately without forwarding to the motor controllers.
    private fun isLocalRequest(request: MessageBottle): Boolean {
        if (request.type.equals(RequestType.GET_APPENDAGE_LOCATION) ||
            request.type.equals(RequestType.GET_JOINT_LOCATION) ||
            request.type.equals(RequestType.GET_METRIC) ||
            request.type.equals(RequestType.MAP_POSE) ||
            request.type.equals(RequestType.SAVE_POSE)
        ) {
            return true
        }
        else if (request.type.equals(RequestType.COMMAND)) {
            val properties: Map<String, String> = request.getProperties()
            val cmd = properties[BottleConstants.COMMAND_NAME]
            return if (cmd.equals(BottleConstants.COMMAND_FREEZE, ignoreCase = true) ||
                cmd.equals(BottleConstants.COMMAND_RELAX, ignoreCase = true)
            ) {
                false
            } else {
                true
            }
        }
        else if (request.type.equals(RequestType.NOTIFICATION)) {
            request.assignSource(ControllerType.DISPATCHER.name()) // Setup to broadcast
            return true
        }
        return false
    }

    // Inform both controllers that we've started ...
    private fun reportStartup(sourceName: String) {
        val startMessage = MessageBottle()
        startMessage.assignRequestType(RequestType.NOTIFICATION)
        startMessage.assignText(selectRandomText(startPhrases))
        LOGGER.info(String.format("%s: Bert is ready ... (to %s)", CLSS, sourceName))
        startMessage.assignSource(sourceName)
        handleResponse(startMessage)
    }

    /**
     * Select a random startup phrase from the list.
     * @return the selected phrase.
     */
    private fun selectRandomText(phrases: Array<String>): String {
        val rand = Math.random()
        val index = (rand * phrases.size).toInt()
        return phrases[index]
    }

    // =============================== SocketStateChangeListener ============================================
    fun stateChanged(event: SocketStateChangeEvent) {
        if (event.getState().equals(SocketStateChangeEvent.READY)) {
            reportStartup(event.getName())
        }
    }

    private const val CLSS = "Dispatcher"
    private const val USAGE = "Usage: launcher <robot_root>"

    // Phrases to choose from ...
    private val mittenPhrases = arrayOf(
        "My hands cut easily",
        "My hands are cold",
        "Mittens are stylish"
    )
    private val startPhrases = arrayOf(
        "Bert is ready",
        "At your command",
        "I'm listening",
        "Speak your wishes"
    )
    private val LOGGER = Logger.getLogger(CLSS)
    private val LOG_ROOT = CLSS.lowercase(Locale.getDefault())

    // ==================================== Main =================================================
    /**
     * Entry point for the launcher application that receives commands, processes
     * them through the serial interfaces to the motors and returns results.
     *
     * Usage: Usage: dispatch <robot_root>
     *
     * @param args command-line arguments
     */
    fun main(args: Array<String>) {
        // Make sure there is command-line argument
        if (args.size < 1) {
            println(USAGE)
            System.exit(1)
        }

        // Analyze command-line argument to obtain the robot root directory.
        val arg = args[0]
        val path = Paths.get(arg)
        PathConstants.setHome(path)
        // Setup logging to use only a file appender to our logging directory
        LoggerUtility.configureRootLogger(LOG_ROOT)
        val model = RobotModel(PathConstants.CONFIG_PATH)
        model.populate() // Analyze the xml for motors and motor groups
        Database.startup(PathConstants.DB_PATH)
        val mgc = MotorGroupController(model)
        val solver = Solver()
        solver.configure(model.getMotors(), PathConstants.URDF_PATH)
        val runner = Dispatcher(model, solver, mgc)
        mgc.setResponseHandler(runner)
        runner.createControllers()
        Runtime.getRuntime().addShutdownHook(Thread(ShutdownHook(runner)))
        runner.startup()
        runner.start()
    }
}