/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.command.Command
import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.controller.SocketStateChangeEvent
import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MetricType
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.control.model.QueueName
import chuckcoughlin.bert.control.solver.Solver
import chuckcoughlin.bert.motor.controller.MotorGroupController
import chuckcoughlin.bert.sql.db.Database
import chuckcoughlin.bert.term.controller.Terminal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.io.IOException
import java.time.LocalDate
import java.time.Month
import java.time.Period
import java.util.*
import java.util.logging.Logger
/**
 * The Dispatcher is the core compute hub of the application. It's job is to accept requests from
 * the various peripheral controllers, distribute them to the motor manager channels and post the results.
 * For complicated requests it may invoke the services of the "Solver" and insert
 * internal intermediate requests.
 *
 * For the peripheral controllers, the dispatcher presents itself as a parent controller, using ifferent
 * channels to communicate with the different peripherals.
 */
class Dispatcher(s:Solver) : Controller {
    private val WEIGHT = 0.5 // weighting to give previous in EWMA
    // Communication channels
    private val commandRequestChannel      : Channel<MessageBottle>    // Commands from Bluetooth
    private val commandResponseChannel     : Channel<MessageBottle>
    private val internalRequestChannel     : Channel<MessageBottle>    // Internal (i.e. local)  controller
    private val internalResponseChannel    : Channel<MessageBottle>
    private val mgcRequestChannel          : Channel<MessageBottle>    // Motor group controller
    private val mgcResponseChannel         : Channel<MessageBottle>
    private val terminalRequestChannel     : Channel<MessageBottle>    // Commands from stdin/stdout
    private val terminalResponseChannel    : Channel<MessageBottle>
    // Controllers
    private val commandController   : Controller
    private var internalController  : InternalController
    private val motorGroupController: MotorGroupController
    private var terminalController: Terminal

    private val scope = MainScope() // Uses Dispatchers.Main
    private var running:Boolean
    private val name: String
    private val solver: Solver = s
    private var cadence = 1000 // msecs
    private var cycleCount = 0 // messages processed
    private var cycleTime = 0.0 // msecs,    EWMA
    private var dutyCycle = 0.0 // fraction, EWMA

    /**
     * On startup, initiate a short message sequence to bring the robot into a sane state.
     * The init{} block takes care of controller creation
     */
    override suspend fun start()  {
        if( !running ) {
            running = true
            commandController.start()
            terminalController.start()
            internalController.start()
            // Motor Group Controller
            LOGGER.info(String.format("%s.execute: starting motorGroupController", CLSS))
            motorGroupController.start()

            // Set the speed to "normal" rate. Delay to all startup to complete
            var msg = MessageBottle(RequestType.SET_POSE)
            msg.pose = BottleConstants.POSE_NORMAL_SPEED
            msg.source = ControllerType.BITBUCKET.name
            var holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            holder.delay = 500 // 1/2 sec delay
            internalController.receiveRequest(holder)
            // Read all the joint positions, one controller at a time
            msg = MessageBottle(RequestType.LIST_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.POSITION
            msg.controller =  BottleConstants.CONTROLLER_LOWER
            msg.source = ControllerType.BITBUCKET.name
            holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            holder.delay = 1000 // 1 sec delay
            internalController.receiveRequest(holder)
            msg = MessageBottle(RequestType.LIST_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.POSITION
            msg.controller = BottleConstants.CONTROLLER_UPPER
            msg.source = ControllerType.BITBUCKET.name
            holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            holder.delay = 1000 // 1 sec delay
            internalController.receiveRequest(holder)
            // Bring any joints that are outside sane limits into compliance
            msg = MessageBottle(RequestType.INITIALIZE_JOINTS)
            msg.source = ControllerType.BITBUCKET.name
            holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            holder.delay = 2000 // 2 sec delay
            internalController.receiveRequest(holder)
            // Inform the terminal and command controllers that we're running
            reportStartup(ControllerType.DISPATCHER.name)
            // =================== Dispatch incoming messages and send to proper receivers =========================
            runBlocking<Unit> {
                launch {
                    Dispatchers.IO
                    while (running) {
                        LOGGER.info(String.format("%s: Entering select for cycle %d ...", CLSS, cycleCount))
                        select<Unit> {
                            // Reply to the original requestor when we get a result from the motor controller
                            mgcResponseChannel.onReceive() {     // Handle a serial response
                                replyToSource(it)
                            }
                            // When we get a response from the internal controller, dispatch the original request.
                            internalResponseChannel.onReceive() {// The internal controller has completed
                                dispatchRequest(it)
                            }
                            // The Bluetooth response channel contains requests that originate on the connected app
                            commandResponseChannel.onReceive() {
                                dispatchRequest(it)
                            }
                            // The Terminal response channel contains requests from commands typed at the terminal
                            terminalResponseChannel.onReceive() {
                                dispatchRequest(it)
                            }
                        }
                    }
                }
            }
            LOGGER.info(String.format("%s.start: startup complete.", CLSS))
        }
        else {
            LOGGER.warning(String.format("%s.start: Attempted to start, but Dispatcher is already running.", CLSS))
        }
    }

    override suspend fun stop() {
        if( running ) {
            running = false
            motorGroupController.stop()
            LOGGER.info(String.format("%s.stop: Shutting down motors ...", CLSS))
            commandController.stop()
            LOGGER.info(String.format("%s.stop: Shutting down bluetooth connection ...", CLSS))
            terminalController.stop()
            LOGGER.info(String.format("%s.stop: Shutting down termina ...", CLSS))
            internalController.stop()
            LOGGER.info(String.format("%s.stop: Shutting down dispatcher ...", CLSS))
            commandRequestChannel.close()
            commandResponseChannel.close()
            internalRequestChannel.close()
            internalResponseChannel.close()
            mgcRequestChannel.close()
            mgcResponseChannel.close()
            terminalRequestChannel.close()
            terminalResponseChannel.close()
            LOGGER.info(String.format("%s.stop: complete.", CLSS))
        }
    }



    // ========================================= Helper Methods =======================================
    /**
     * Analyze an incoming message and determine what to do with it. Ultimately it will be placed in
     * the appropriate response channel.
     */
    private suspend fun dispatchRequest(msg : MessageBottle) {
        //LOGGER.info(String.format("%s: Starting run loop ...",CLSS));
        val startCycle = System.currentTimeMillis()
        LOGGER.info(String.format("%s: Cycle %d ...", CLSS, cycleCount))
        // "internal" requests are those that need to be queued on the internal controller
        if (isInternalRequest(msg)) {
            val response: MessageBottle = handleInternalRequest(msg)
            internalRequestChannel.send(response)
        }
        else if (isLocalRequest(msg)) {
            // Handle local request -create response
            val response: MessageBottle = handleLocalRequest(msg)
            replyToSource(response)
        }
        else {
            // Handle motor request. The controller forwards response here via "handleResponse".

        }
    }

    // The response is simply the request. A generic acknowledgement will be relayed to the user.
    // 1) Freezing a joint requires getting the motor position first to update the internal status dictionary
    private fun handleInternalRequest(request: MessageBottle): MessageBottle {
        // Entire robot
        if (request.type.equals(RequestType.COMMAND) &&
            request.command.equals(CommandType.FREEZE)) {
            val msg = MessageBottle(RequestType.LIST_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.POSITION
            msg.source = ControllerType.BITBUCKET.name
            var holder = InternalMessageHolder(msg, QueueName.GLOBAL)
            internalController.receiveRequest(holder)
            holder = InternalMessageHolder(request, QueueName.GLOBAL)
            holder.delay =1000 // 1 sec delay
            internalController.receiveRequest(holder)
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY) &&
            request.jointDynamicProperty.equals(JointDynamicProperty.STATE)  ) {
            val walker = request.getJointValueIterator()
            for( jpv in walker ) {
                val value = jpv.value
                if( value.equals(BottleConstants.ON_VALUE) ) {
                    val msg = MessageBottle(RequestType.LIST_MOTOR_PROPERTY)
                    msg.jointDynamicProperty = JointDynamicProperty.POSITION
                    msg.limb = request.limb
                    msg.source = ControllerType.BITBUCKET.name
                    var holder = InternalMessageHolder(msg, QueueName.GLOBAL)
                    internalController.receiveRequest(holder)
                    holder = InternalMessageHolder(request, QueueName.GLOBAL)
                    holder.delay = 500 // 1/2 sec delay
                    internalController.receiveRequest(holder)
                }
            }

        }
        else if (request.type.equals(RequestType.SET_MOTOR_PROPERTY) &&
                 request.jointDynamicProperty.equals(JointDynamicProperty.STATE) ) {
            val walker = request.getJointValueIterator()
            for( jpv in walker ) {
                val value = jpv.value
                if( value.equals(BottleConstants.ON_VALUE) ) {
                    val msg = MessageBottle(RequestType.GET_MOTOR_PROPERTY)
                    msg.jointDynamicProperty = JointDynamicProperty.POSITION
                    msg.joint = request.joint
                    msg.source = ControllerType.BITBUCKET.name
                    var holder = InternalMessageHolder(msg, QueueName.GLOBAL)
                    internalController.receiveRequest(holder)
                    holder = InternalMessageHolder(request, QueueName.GLOBAL)
                    holder.delay = 250 // 1/4 sec delay
                    internalController.receiveRequest(holder)
                }
            }
        }
        return request
    }

    // Create a response for a request that can be handled immediately. The response is simply the original request
    // with some text to return back to the user. 
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        // The following two requests simply use the current positions of the motors, whatever they are
        if (request.type.equals(RequestType.GET_APPENDAGE_LOCATION)) {
            solver.setTreeState() // Forces new calculations
            val appendage = request.appendage
            val xyz: DoubleArray = solver.getPosition(appendage)
            val text = String.format(
                "%s is located at %0.2f %0.2f %0.2f meters",
                appendage.name.lowercase(Locale.getDefault()),xyz[0], xyz[1],xyz[2])
            request.text = text
        }
        else if (request.type.equals(RequestType.GET_JOINT_LOCATION)) {
            solver.setTreeState()
            val joint = request.joint
            val xyz: DoubleArray = solver.getPosition(joint)
            val text = String.format(
                "The center of joint %s is located at %0.2f %0.2f %0.2f meters",
                joint.name,xyz[0],xyz[1], xyz[2])
            request.text = text
        }
        else if (request.type.equals(RequestType.GET_METRIC)) {
            val metric: MetricType = request.metric
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
                MetricType.HEIGHT -> text = "My height when standing is 83 centimeters"
                MetricType.MITTENS -> text = selectRandomText(mittenPhrases)
                MetricType.NAME -> text = "My name is $name"
            }
            request.text = text
        }
        else if (request.type.equals(RequestType.COMMAND)) {
            val command = request.command
            LOGGER.warning(String.format("%s.handleLocalRequest: command=%s", CLSS, command.name))
            if( command.equals(CommandType.HALT) ) {
                System.exit(0) // Rely on ShutdownHandler cleanup connections
            }
            else if (command.equals(CommandType.SHUTDOWN) ) {
                try {
                    val rt = Runtime.getRuntime()
                    rt.exec("sudo poweroff")
                }
                catch (ioe: IOException) {
                    LOGGER.warning(
                        String.format( "%s.createResponseForLocalRequest: Powerdown error (%s)",
                            CLSS,ioe.message))
                }
            }
            else {
                val msg = String.format("Unrecognized command: %s", command)
                request.error = msg
            }
        }
        else if (request.type.equals(RequestType.MAP_POSE)) {
            val command = request.command
            val poseName = request.pose
            if (!command.equals(CommandType.NONE) && !poseName.equals(BottleConstants.NO_POSE)) {
                Database.mapCommandToPose(command.name, poseName)
            }
            else {
                request.error = "I could not map because either command or pose is empty"
            }
        }
        else if (request.type.equals(RequestType.SAVE_POSE)) {
            var poseName: String = request.pose
            if (!poseName.equals(BottleConstants.NO_POSE)) {
                Database.saveJointPositionsForPose(RobotModel.motors, poseName)
            }
            else {
                poseName = Database.saveJointPositionsAsNewPose(RobotModel.motors)
                request.text = "I saved the pose as $poseName"
            }
        }
        return request
    }

    private fun exponentiallyWeightedMovingAverage(currentValue: Double, previousValue: Double): Double {
        return (1.0 - WEIGHT) * currentValue + WEIGHT * previousValue
    }

    // These are complex requests that require that several messages be created and processed
    // on the internal controller. Categories include:
    //   1) Anything that sets "torque enable" to true. This action requires that we read
    //      and save (in memory) current motor positions.
    private fun isInternalRequest(request: MessageBottle): Boolean {
        // Never send a request launched by the internal controller back to it. That would be an infinite loop
        if( request.source.equals(ControllerType.INTERNAL.name ) ) return false
        if (request.type.equals(RequestType.COMMAND) &&
            request.command.equals(CommandType.FREEZE) ) {
            return true
        }
        else if( request.type.equals(RequestType.SET_LIMB_PROPERTY) &&
                 request.jointDynamicProperty.equals(JointDynamicProperty.STATE)  ) {
            val walker = request.getJointValueIterator()
            for( jpv in walker ) {
                if( jpv.value.equals(BottleConstants.ON_VALUE )) return true
            }
            return false
        }
        else if (request.type.equals(RequestType.SET_MOTOR_PROPERTY) &&
                 request.jointDynamicProperty.equals(JointDynamicProperty.STATE) ) {
            val walker = request.getJointValueIterator()
            for( jpv in walker ) {
                if( jpv.value.equals(BottleConstants.ON_VALUE )) return true
            }
            return false
        }
        return false
    }

    // Local requests are those that can be handled immediately without forwarding to the motor controllers.
    private fun isLocalRequest(request: MessageBottle): Boolean {
        if (request.type.equals(RequestType.GET_APPENDAGE_LOCATION) ||
            request.type.equals(RequestType.GET_JOINT_LOCATION) ||
            request.type.equals(RequestType.GET_METRIC) ||
            request.type.equals(RequestType.MAP_POSE) ||
            request.type.equals(RequestType.SAVE_POSE)) {
            return true
        }
        else if (request.type.equals(RequestType.COMMAND)) {
            val cmd = request.command
            return if (cmd.equals(CommandType.FREEZE) ||
                cmd.equals(CommandType.RELAX) ) {
                false
            }
            else {
                true
            }
        }
        else if( request.type == RequestType.NOTIFICATION ) {
            request.source = ControllerType.DISPATCHER.name
            return true
        }
        return false
    }
    /**
     * The sub-controller has posted a response meant to be returned to the controller
     * that had originated the request.
     */
    private suspend fun replyToSource(response: MessageBottle) {
        val source: String = response.source
        LOGGER.info(String.format("%s.replyToSource: Received response %s for %s",
            CLSS,response.type.name,source))
        if (source.equals(ControllerType.COMMAND.name, ignoreCase = true)) {
            commandResponseChannel.send(response)
        }
        else if (source.equals(ControllerType.TERMINAL.name, ignoreCase = true)) {
            terminalResponseChannel.send(response)
        }
        else {
            // There should be no routings to Dispatcher, Internal or MotorController
            LOGGER.warning(String.format("%s.handleResponse: Unknown destination - %s, ignored", CLSS, source))
        }
    }

    // Report to both bluetooth and stdio controllers that we're running  ...
    private suspend fun reportStartup(sourceName: String) {
        val startMessage = MessageBottle(RequestType.NOTIFICATION)
        startMessage.text = selectRandomText(startPhrases)
        LOGGER.info(String.format("%s: Bert is ready ... (to %s)", CLSS, sourceName))
        startMessage.source = sourceName
        commandResponseChannel.send(startMessage)
        terminalResponseChannel.send(startMessage)
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
    suspend fun stateChanged(event: SocketStateChangeEvent) {
        if (event.state.equals(SocketStateChangeEvent.READY)) {
            reportStartup(event.name!!)
        }
    }

    // Phrases to choose from ... (randomly)
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

    private val CLSS = "Dispatcher"
    private val LOGGER = Logger.getLogger(CLSS)
    private val LOG_ROOT = CLSS.lowercase(Locale.getDefault())
    override val controllerName = CLSS
    override val controllerType = ControllerType.DISPATCHER
    /**
     * The dispatcher creates all controllers and communitaion channels for the application. Request/response
     * naming is from the point of view of the Dispatcher.
     *    Command - bluetooth connection to the tablet
     *    Internal - where multiple or repeating messages are required for a single user request.
     *    MotorController - make serial requests to the motors
     *    Terminal - communicate directly with the user console
    */
    init {
        commandRequestChannel = Channel<MessageBottle>()
        commandResponseChannel= Channel<MessageBottle>()
        internalRequestChannel  = Channel<MessageBottle>()
        internalResponseChannel = Channel<MessageBottle>()
        mgcRequestChannel  = Channel<MessageBottle>()
        mgcResponseChannel = Channel<MessageBottle>()
        terminalRequestChannel  = Channel<MessageBottle>()
        terminalResponseChannel = Channel<MessageBottle>()

        commandController    = Command(this,commandRequestChannel,commandResponseChannel)
        internalController    = InternalController(this,internalRequestChannel,internalResponseChannel)
        motorGroupController = MotorGroupController(this,mgcRequestChannel,mgcResponseChannel)
        terminalController    = Terminal(this,terminalRequestChannel,terminalResponseChannel)

        running = false
        name = RobotModel.getProperty(ConfigurationConstants.PROPERTY_ROBOT_NAME)
        val cadenceString: String = RobotModel.getProperty(ConfigurationConstants.PROPERTY_CADENCE, "1000") // ~msecs
        try {
            cadence = cadenceString.toInt()
        }
        catch (nfe: NumberFormatException) {
            LOGGER.warning(String.format("%s.constructor: Cadence must be an integer (%s)", CLSS, nfe.localizedMessage))
        }
        LOGGER.info(String.format("%s: started with cadence %d msecs", CLSS, cadence))
    }

}