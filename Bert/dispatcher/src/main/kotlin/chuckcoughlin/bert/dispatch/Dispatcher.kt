/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.command.Command
import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MetricType
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDefinitionProperty
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.common.model.Solver
import chuckcoughlin.bert.common.model.URDFModel
import chuckcoughlin.bert.motor.controller.MotorGroupController
import chuckcoughlin.bert.sql.db.Database
import chuckcoughlin.bert.term.controller.Terminal
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.Month
import java.time.Period
import java.util.*
import java.util.logging.Logger
/**
 * The Dispatcher is the distribution hub of the application. Its' job is to accept requests from
 * the various peripheral controllers, distribute them to the motor manager channels and post the results.
 * For complicated requests it may invoke the services of the "Solver" and insert
 * internal intermediate requests.
 *
 * For each peripheral controller, the dispatcher uses a pair of communication channels
 * to send and receive message objects.
 */
@DelicateCoroutinesApi
class Dispatcher() : Controller {
    // Communication channels
    private val commandRequestChannel      : Channel<MessageBottle>    // Commands from network (wifi)
    private val commandResponseChannel     : Channel<MessageBottle>    // Response to network (wifi)
    private val fromInternalController     : Channel<MessageBottle>    // Internal (i.e. local)  controller
    private val toInternalController       : Channel<MessageBottle>
    private val mgcRequestChannel          : Channel<MessageBottle>    // Motor group controller
    private val mgcResponseChannel         : Channel<MessageBottle>
    private val stdinChannel               : Channel<MessageBottle>    // Requests from stdin
    private val stdoutChannel              : Channel<MessageBottle>    // Responses to stdout
    // Controllers
    private val commandController   : Command
    private var internalController  : InternalController
    private val motorGroupController: MotorGroupController
    private var terminalController: Terminal

    private val scope = GlobalScope // For long-running coroutines
    private var running:Boolean
    private val name: String
    private var cadence = 1000 // msecs
    private var cycleCount= 0   // messages processed
    private var cycleTime = 0.0 // msecs,    EWMA
    private var dutyCycle = 0.0 // fraction, EWMA

    /**
     * On startup, initiate a short message sequence to bring the robot into a sane state.
     * The init{} block takes care of controller creation, but we start them here.
     */
    @DelicateCoroutinesApi
    override suspend fun execute()  {
        if(DEBUG) LOGGER.info(String.format("%s.execute: startup ...", CLSS))
        if( !running ) {
            running = true
            if( RobotModel.useNetwork) {
                commandController.execute()
            }
            if( RobotModel.useTerminal) {
                terminalController.execute()
            }
            internalController.execute()
            motorGroupController.execute()

            // =================== Dispatch incoming messages and send to proper receivers =========================
            // Initiate the startup sequence. Obtain current positions and guarantee a sane state.
            if(DEBUG) LOGGER.info(String.format("%s.execute: Launching startup sequence ...", CLSS))
            scope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Default) {
                    initialize()
                    LOGGER.info(String.format("%s.execute: initialization complete", CLSS))
                    // Inform the terminal and command controllers that we're running
                    reportStartup()
                }
            }
            // Loop forever handling command requests -----------
            runBlocking {
                if(DEBUG) LOGGER.info(String.format("%s.execute: Launching receive message co-routine ...", CLSS))
                while (running) {
                    val startCycle = System.currentTimeMillis()
                    if(DEBUG) LOGGER.info(String.format("%s.execute: Entering select for cycle %d ...", CLSS, cycleCount))
                    select<Unit> {
                        // Reply to the original requester when we get a result from the motor controller
                        mgcResponseChannel.onReceive {     // Handle a serial response
                            if(DEBUG) LOGGER.info(String.format("%s.execute: bgcResponseChannel receive %s(%s) from %s",
                                CLSS, it.type.name,it.text,it.source))
                            replyToSource(it)
                        }
                        // When we get a response from the internal controller, dispatch the original request.
                        fromInternalController.onReceive { // The internal controller has completed
                            if(DEBUG) LOGGER.info(String.format("%s.execute: fromInternalController receive %s(%s) from %s",
                                CLSS, it.type.name,it.text,it.source))
                            dispatchInternalResponse(it)
                        }
                        // The Command response channel contains requests that originate on the connected app (tablet)
                        commandRequestChannel.onReceive {
                            if(DEBUG) LOGGER.info(String.format("%s.execute: commandRequestChannel receive %s(%s) from %s",
                                CLSS, it.type.name,it.text,it.source))
                            dispatchCommandResponse(it)
                        }
                        // The Terminal stdin channel contains requests typed at the terminal
                        stdinChannel.onReceive {
                            if(DEBUG) LOGGER.info(String.format("%s.execute: stdinChannel receive %s(%s) from %s",
                                CLSS, it.type.name,it.text,it.source))
                            dispatchCommandResponse(it)
                        }
                    }
                    cycleCount = cycleCount + 1
                    val endCycle=System.currentTimeMillis()
                    val elapsed=endCycle - startCycle
                    cycleTime=exponentiallyWeightedMovingAverage(cycleTime, elapsed.toDouble())
                    dutyCycle=exponentiallyWeightedMovingAverage(dutyCycle, elapsed.toDouble() / cadence)
                }
                LOGGER.info(String.format("%s.execute: execution complete.", CLSS))
            }
        }
        else {
            LOGGER.warning(String.format("%s.execute: Attempted to start, but Dispatcher is already running.", CLSS))
        }
    }

    // Send preliminary messages to ensure a sane starting configuration
    suspend fun initialize() {
        if(DEBUG) LOGGER.info(String.format("%s.initialize: sending messages to establish sanity", CLSS))
        // Set the speed to "normal" rate.
        var msg = MessageBottle(RequestType.SET_POSE)
        msg.pose = ConfigurationConstants.POSE_NORMAL_SPEED
        msg.source = ControllerType.BITBUCKET.name
        msg.control.delay = 500                // 1/2 sec delay
        toInternalController.send(msg)

        // Read all the joint positions (using both controllers). This fills our
        // internal buffers with the current positions.
        msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
        msg.jointDynamicProperty = JointDynamicProperty.ANGLE
        msg.source = ControllerType.BITBUCKET.name
        msg.control.delay = 1000 // 1 sec delay
        toInternalController.send(msg)

        // Bring any joints that are outside sane limits into compliance
        msg = MessageBottle(RequestType.INITIALIZE_JOINTS)
        msg.source = ControllerType.BITBUCKET.name
        msg.control.delay = 2000 // 2 sec delay
        toInternalController.send(msg)
    }

    /**
     * Stop the entire application. This is run by the main() once the
     * execute() method returns.
     * Note: The logger seems to have no effect within the shutdown handler,
     *       thus the use of println.
     * Note: We have attempted to close channels, but the keep getting exceptions
     *       even though we test for already closed before calling close.
     */
    override suspend fun shutdown() {
        if(DEBUG) println(String.format("%s.shutdown: running = %s.", CLSS,if(running) "TRUE" else "FALSE"))
        if( running ) {
            try {
                motorGroupController.shutdown()
                if(DEBUG) println(String.format("%s.shutdown: motors ...", CLSS))
                commandController.shutdown()
                if(DEBUG) println(String.format("%s.shutdown: network connection ...", CLSS))
                terminalController.shutdown()
                if(DEBUG) println(String.format("%s.shutdown: terminal ...", CLSS))
                internalController.shutdown()
                if(DEBUG) println(String.format("%s.shutdown: internal controller ...", CLSS))
                Database.shutdown()
            }
            catch (e: Exception) {
                println(String.format("\n%s: ERROR in exit %s", CLSS, e.localizedMessage))
                e.printStackTrace()
            }
            running = false
        }
        println(String.format("%s.shutdown: complete.", CLSS))
    }
    // ========================================= Helper Methods =======================================
    /**
     * Analyze an incoming message from the command (wifi) or terminal channels. Some requests
     * are handled immediately. Any motor requests are first passed to the internal controller to
     * handle delay or conflict issues.
     */
    private suspend fun dispatchCommandResponse(msg : MessageBottle) {
        if(DEBUG) LOGGER.info(String.format("%s.dispatchCommandResponse %s from %s",CLSS,msg.type.name,msg.source))
        // "internal" requests are those that need to be queued on the internal controller
        if( isInternalRequest(msg) ) {
            val response: MessageBottle = handleInternalRequest(msg)
            toInternalController.send(response)
        }
        else if(isLocalRequest(msg)) {
            // Handle local request -create response unless type set to NONE
            val response: MessageBottle = handleLocalRequest(msg)
            if( !response.type.equals(RequestType.NONE) &&
                !response.type.equals(RequestType.HANGUP ) ) replyToSource(response)
        }
        else if(isMotorRequest(msg)) {
            toInternalController.send(msg)
        }
        else {
            LOGGER.info(String.format("%s.dispatchCommandResponse %s from %s is unhandled",
                    CLSS,msg.type.name,msg.source))
            msg.error = String.format("internal error, %s message is unhandled in dispatcher",msg.type.name)
            replyToSource(msg)
        }
    }

    /**
     * Analyze messages coming from the internal controller. All delay and conflict issues have been
     * resolved. Forward messages on to the motor controller. The source, presumeably has not been
     * altered from the command requests.
     */
    private suspend fun dispatchInternalResponse(msg : MessageBottle) {
        if(DEBUG) LOGGER.info(String.format("%s.dispatchInternalResponse %s from %s",CLSS,msg.type.name,msg.source))
        // "internal" requests are those that need to be queued on the internal controller
        if(isMotorRequest(msg)) {
            mgcRequestChannel.send(msg)
        }
        else if(msg.type.equals(RequestType.HEARTBEAT)) {
            // Do nothing
        }
        else {
            LOGGER.info(String.format("%s.dispatchInternalResponse %s from %s is unhandled",
                    CLSS,msg.type.name,msg.source))
            msg.error = String.format("internal error, %s message is unhandled in dispatcher",msg.type.name)
            replyToSource(msg)
        }
    }

    // The response is simply the request. A generic acknowledgement will be relayed to the user.
    // 1) Freezing a joint requires getting the motor position first to update the internal status dictionary
    private suspend fun handleInternalRequest(request: MessageBottle): MessageBottle {
        // Entire robot
        if(request.type==RequestType.COMMAND &&
            request.command.equals(CommandType.FREEZE)) {
            var msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.ANGLE
            msg.source = ControllerType.BITBUCKET.name
            toInternalController.send(request)

            msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.ANGLE
            msg.source = ControllerType.BITBUCKET.name
            msg.control.delay =1000 // 1 sec delay
            toInternalController.send(msg)
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY) &&
            request.jointDynamicProperty.equals(JointDynamicProperty.STATE)  ) {
            val walker = request.getJointValueIterator()
            for( jpv in walker ) {
                val value = jpv.value
                if( value.equals(BottleConstants.ON_VALUE) ) {
                    var msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
                    msg.jointDynamicProperty = JointDynamicProperty.ANGLE
                    msg.limb = request.limb
                    msg.source = ControllerType.BITBUCKET.name
                    toInternalController.send(msg)

                    msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
                    msg.jointDynamicProperty = JointDynamicProperty.ANGLE
                    msg.limb = request.limb
                    msg.source = ControllerType.BITBUCKET.name
                    msg.control.delay = 500 // 1/2 sec delay
                    toInternalController.send(msg)
                }
            }
        }
        else if (request.type.equals(RequestType.SET_MOTOR_PROPERTY) &&
            request.jointDynamicProperty.equals(JointDynamicProperty.STATE) ) {
            val walker = request.getJointValueIterator()
            for( jpv in walker ) {
                val value = jpv.value
                if( value.equals(BottleConstants.ON_VALUE) ) {
                    var msg = MessageBottle(RequestType.GET_MOTOR_PROPERTY)
                    msg.jointDynamicProperty = JointDynamicProperty.ANGLE
                    msg.joint = request.joint
                    msg.source = ControllerType.BITBUCKET.name
                    toInternalController.send(msg)

                    msg = MessageBottle(RequestType.GET_MOTOR_PROPERTY)
                    msg.jointDynamicProperty = JointDynamicProperty.ANGLE
                    msg.joint = request.joint
                    msg.source = ControllerType.BITBUCKET.name
                    msg.control.delay = 250 // 1/4 sec delay
                    toInternalController.send(msg)
                }
            }
        }
        return request
    }

    // Create a response for a request that can be handled immediately, that is without
    // reference to the motors. The response is simply the original request
    // with altered text to return to the user.
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        if (request.type.equals(RequestType.COMMAND)) {
            val command = request.command
            LOGGER.info(String.format("%s.handleLocalRequest: command=%s", CLSS, command.name))
            if( command.equals(CommandType.HALT) ) {
                request.type = RequestType.NONE  // Suppress a response
                System.exit(0) // Rely on ShutdownHandler
            }
            else if (command.equals(CommandType.SHUTDOWN) ) {
                try {
                    val commands = arrayOf("sudo poweroff")
                    val rt = Runtime.getRuntime()
                    rt.exec(commands)
                }
                catch (ioe: IOException) {LOGGER.warning(String.format( "%s.handleLocalRequest: Powerdown error (%s)",
                            CLSS,ioe.message))
                }
            }
        }
        // The following two requests simply use the current positions of the motors, whatever they are
        else if (request.type.equals(RequestType.GET_APPENDAGE_LOCATION)) {
            LOGGER.info(String.format("%s.handleLocalRequest: text=%s", CLSS, request.text))
            Solver.setTreeState() // Forces new calculations
            val appendage = request.appendage
            val xyz: DoubleArray = Solver.getLocation(appendage)
            val text = String.format(
                "%s is located at %0.2f %0.2f %0.2f meters",
                appendage.name.lowercase(Locale.getDefault()),xyz[0], xyz[1],xyz[2])
            request.text = text
        }
        // The location in physical coordinates from the center of the robot.
        else if (request.type.equals(RequestType.GET_JOINT_LOCATION)) {
            LOGGER.info(String.format("%s.handleLocalRequest: text=%s", CLSS, request.text))
            Solver.setTreeState()
            val joint = request.joint
            val xyz: DoubleArray = Solver.getLocation(joint)
            val text = String.format(
                "The center of joint %s is located at %0.2f %0.2f %0.2f meters",
                joint.name,xyz[0],xyz[1], xyz[2])
            request.text = text
        }
        else if (request.type.equals(RequestType.GET_METRIC)) {
            LOGGER.info(String.format("%s.handleLocalRequest: metric=%s", CLSS, request.metric))
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
                MetricType.CADENCE -> text = "The cadence is "+cadence+" milliseconds"
                MetricType.CYCLECOUNT -> text = "I've processed "+cycleCount+" requests"
                MetricType.CYCLETIME -> text = "The average cycle time is "+cycleTime.toInt()+" milliseconds"
                MetricType.DUTYCYCLE -> text = "My average duty cycle is "+(100.0 * dutyCycle).toInt()+" percent"
                MetricType.HEIGHT -> text = "My height when standing is 83 centimeters"
                MetricType.MITTENS -> text = selectRandomText(mittenPhrases)
                MetricType.NAME -> text = "My name is $name"
                else -> request.error = String.format("I can't get the value of %s",metric.name)
            }
            request.text = text
        }
        // THese are the definition properties
        else if( request.type == RequestType.GET_MOTOR_PROPERTY &&
            request.jointDynamicProperty == JointDynamicProperty.NONE )  {
            val joint = request.joint
            val mc = RobotModel.motorsByJoint[joint]!!
            if(request.jointDefinitionProperty == JointDefinitionProperty.ID) {
                request.text = String.format("The id of my %s is %d",Joint.toText(joint),mc.id)
            }
            else if(request.jointDefinitionProperty == JointDefinitionProperty.OFFSET) {
                request.text = String.format("The angular offset of my %s is %d",Joint.toText(joint),mc.offset)
            }
            else if(request.jointDefinitionProperty == JointDefinitionProperty.ORIENTATION) {
                request.text = String.format("The orientation of my %s is %s",Joint.toText(joint),
                              if(mc.isDirect) "direct" else "indirect")
            }
            else if(request.jointDefinitionProperty == JointDefinitionProperty.MOTORTYPE) {
                request.text = String.format("The motor type of my %s is %s",Joint.toText(joint),mc.type.name)
            }
        }
        else if( request.type == RequestType.GET_MOTOR_PROPERTY &&
                 request.jointDefinitionProperty == JointDefinitionProperty.NONE ) {
            val joint = request.joint
            val mc = RobotModel.motorsByJoint[joint]!!
            if(request.jointDynamicProperty == JointDynamicProperty.MAXIMUMANGLE) {
                request.text = String.format("The maximum angle of my %s is %2.0f degrees",Joint.toText(joint),mc.maxAngle)
            }
            else if(request.jointDynamicProperty == JointDynamicProperty.MINIMUMANGLE) {
                request.text = String.format("The minimum angle of my %s is %2.0f degrees",Joint.toText(joint),mc.minAngle)
            }
            else if(request.jointDynamicProperty == JointDynamicProperty.RANGE) {
                request.text = String.format("I can move my %s from %2.0f to %2.0f",Joint.toText(joint),mc.minAngle,mc.maxAngle)
            }
        }
        // List the names of different kinds of motor properties
        else if (request.type.equals(RequestType.LIST_MOTOR_PROPERTIES)) {
            LOGGER.info(String.format("%s.handleLocalRequest: text=%s", CLSS, request.text))
            if(!request.jointDefinitionProperty.equals(JointDefinitionProperty.NONE)) {
                request.text = JointDefinitionProperty.toJSON()
            }
            else {
                request.text = JointDynamicProperty.toJSON()
            }
        }
        // List various
        else if (request.type.equals(RequestType.LIST_NAMES)) {
            LOGGER.info(String.format("%s.handleLocalRequest: metric=%s", CLSS, request.metric))
            val metric: MetricType = request.metric
            var text = ""
            when (metric) {
                MetricType.APPENDAGES -> {
                    text = URDFModel.chain.appendagesToJSON()
                }
                MetricType.JOINTS -> {
                    text = URDFModel.chain.jointsToJSON()
                }
                MetricType.LIMBS -> {
                    text = URDFModel.chain.limbsToJSON()
                }
                else -> request.error = String.format("I can't get the names of %s",metric.name)
            }
            request.text = text
        }
        else if (request.type.equals(RequestType.MAP_POSE)) {
            LOGGER.info(String.format("%s.handleLocalRequest: text=%s", CLSS, request.text))
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
            LOGGER.info(String.format("%s.handleLocalRequest: text=%s", CLSS, request.text))
            var poseName: String = request.pose
            if (!poseName.equals(BottleConstants.NO_POSE)) {
                Database.saveJointAnglesForPose(RobotModel.motorsByJoint, poseName)
            }
            else {
                poseName = Database.saveJointAnglesAsNewPose(RobotModel.motorsByJoint)
                request.text = "I saved the pose as $poseName"
            }
        }
        // We are here because there is a range error
        else if( request.type == RequestType.SET_MOTOR_PROPERTY &&
            request.jointDynamicProperty == JointDynamicProperty.ANGLE ) {
            val joint = request.joint
            val mc = RobotModel.motorsByJoint[joint]!!
            if( request.value>mc.maxAngle ) {
               request.error = String.format("I can only move my %s to %2.0f degrees", Joint.toText(joint),mc.maxAngle)
            }
            else if (request.value < mc.minAngle ) {
                request.error = String.format("I can only move my %s to %2.0f degrees", Joint.toText(joint),mc.minAngle)
            }
        }
        return request
    }

    // These are complex requests that require that several messages be created and processed
    // on the internal controller - or otherwise requests that cannot execute too closely in time.
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
        // Anything that sets "torque enable" to true requires that we read
        //  and save (in memory) current motor positions.
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

    // Local requests are those that can be handled immediately
    // without forwarding to the motor controllers. This includes some
    // error conditions.
    private fun isLocalRequest(request: MessageBottle): Boolean {
        if (request.type.equals(RequestType.GET_APPENDAGE_LOCATION) ||
            request.type.equals(RequestType.GET_JOINT_LOCATION) ||
            request.type.equals(RequestType.GET_METRIC) ||
            request.type.equals(RequestType.HANGUP)     ||
            request.type.equals(RequestType.LIST_MOTOR_PROPERTIES) ||
            request.type.equals(RequestType.LIST_NAMES) ||
            request.type.equals(RequestType.MAP_POSE) ||
            request.type.equals(RequestType.SAVE_POSE)) {
            return true
        }
        else if (request.type.equals(RequestType.COMMAND)) {
            val cmd = request.command
            return if (cmd.equals(CommandType.HALT) ||
                cmd.equals(CommandType.SHUTDOWN)) {
                true
            }
            else {
                false
            }
        }
        // THese are the definition properties
        else if( request.type == RequestType.GET_MOTOR_PROPERTY &&
                 request.jointDynamicProperty == JointDynamicProperty.NONE )  {
            return true
        }
        else if( request.type == RequestType.GET_MOTOR_PROPERTY &&
                 ( request.jointDynamicProperty == JointDynamicProperty.MAXIMUMANGLE ||
                   request.jointDynamicProperty == JointDynamicProperty.MINIMUMANGLE ||
                   request.jointDynamicProperty == JointDynamicProperty.RANGE ) ) {
            return true
        }
        // Some very specific errors
        else if( request.type == RequestType.SET_MOTOR_PROPERTY &&
                 request.jointDynamicProperty == JointDynamicProperty.ANGLE ) {
            val joint = request.joint
            val mc = RobotModel.motorsByJoint[joint]!!
            if( request.value>mc.maxAngle || request.value<mc.minAngle) {
                return true
            }
        }
        else if( request.type == RequestType.NOTIFICATION ) {
            request.source = ControllerType.DISPATCHER.name
            return true
        }
        return false
    }
    // These are requests that can be processed directly by the group controller and sent to the
    // proper motor controller.
    private fun isMotorRequest(request: MessageBottle): Boolean {
        if (request.type.equals(RequestType.COMMAND) ||
            request.type.equals(RequestType.GET_LIMITS) ||
            request.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
            request.type.equals(RequestType.INITIALIZE_JOINTS)  ||
            request.type.equals(RequestType.LIST_MOTOR_PROPERTY) ||
            request.type.equals(RequestType.READ_MOTOR_PROPERTY) ||
            request.type.equals(RequestType.SET_POSE) ||
            request.type.equals(RequestType.SET_MOTOR_PROPERTY) ) {
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
        LOGGER.info(String.format("%s.replyToSource: Received response %s(%s) from %s",
                    CLSS,response.type.name,response.text,source))
        if (source.equals(ControllerType.COMMAND.name, ignoreCase = true)) {
            commandResponseChannel.send(response)
        }
        else if (source.equals(ControllerType.TERMINAL.name, ignoreCase = true)) {
            stdoutChannel.send(response)
        }
        else if (source.equals(ControllerType.BITBUCKET.name, ignoreCase = true)) {
            ;   // Do nothing
        }
        else {
            // There should be no routes to Dispatcher, Internal or MotorController
            LOGGER.warning(String.format("%s.replyToSource: Unknown destination - %s, ignored", CLSS, source))
        }
    }

    // Report to both command and stdio controllers that we're running  ...
    private suspend fun reportStartup() {
        val startMessage = MessageBottle(RequestType.NOTIFICATION)
        startMessage.text = selectRandomText(startPhrases)
        LOGGER.info(String.format("%s.reportStartup: Bert is ready ... (from %s)", CLSS, ControllerType.DISPATCHER.name))
        startMessage.source = ControllerType.DISPATCHER.name
        if(RobotModel.useTerminal) stdoutChannel.send(startMessage)
        if(RobotModel.useNetwork)  commandController.startMessage = startMessage.text  // Would be better to use channel
    }
    private fun exponentiallyWeightedMovingAverage(currentValue: Double,previousValue: Double): Double {
        return (1.0 - WEIGHT) * currentValue + WEIGHT * previousValue
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

    // Phrases to choose from ... (randomly)
    private val mittenPhrases = arrayOf(
        "My hands cut easily",
        "My hands are cold",
        "Mittens are stylish",
        "My fingers don't fit into gloves"
    )
    private val startPhrases = arrayOf(
        "Bert is ready",
        "At your command",
        "Ready",
        "I'm listening",
        "Speak your wishes",
        "Bert is ready for commands",
        "Bert is at your service",
        "Marj I am ready",
        "Marj speak to me",
        "Marj command me"
    )

    private val CLSS = "Dispatcher"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    private val WEIGHT = 0.5 // weighting to give previous in EWMA

    override val controllerName = CLSS
    override val controllerType = ControllerType.DISPATCHER
    /**
     * The dispatcher creates all controllers and communication channels for the application. Request/response
     * naming is from the point of view of the Dispatcher.
     *    Command - network (wifi) connection to the tablet
     *    Internal - where multiple or repeating messages are required for a single user request.
     *    MotorController - make serial requests to the motors
     *    Terminal - communicate directly with the user console
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_DISPATCHER)
        commandRequestChannel = Channel<MessageBottle>()
        commandResponseChannel= Channel<MessageBottle>()
        fromInternalController  = Channel<MessageBottle>()
        toInternalController = Channel<MessageBottle>()
        mgcRequestChannel  = Channel<MessageBottle>()
        mgcResponseChannel = Channel<MessageBottle>()
        stdinChannel  = Channel<MessageBottle>()
        stdoutChannel = Channel<MessageBottle>()

        commandController     = Command(commandRequestChannel,commandResponseChannel)
        internalController    = InternalController(fromInternalController,toInternalController)
        motorGroupController  = MotorGroupController(mgcRequestChannel,mgcResponseChannel)
        terminalController    = Terminal(stdinChannel,stdoutChannel)

        running = false
        name = RobotModel.getProperty(ConfigurationConstants.PROPERTY_ROBOT_NAME)
        val cadenceString: String = RobotModel.getProperty(ConfigurationConstants.PROPERTY_CADENCE, "1000") // ~msecs
        try {
            cadence = cadenceString.toInt()
        }
        catch (nfe: NumberFormatException) {
            LOGGER.warning(String.format("%s.constructor: Cadence must be an integer (%s)", CLSS, nfe.localizedMessage))
        }
        LOGGER.info(String.format("%s.init: cadence %d msecs", CLSS, cadence))
    }
}