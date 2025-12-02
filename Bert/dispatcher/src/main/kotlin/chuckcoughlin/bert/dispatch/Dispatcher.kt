/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.ai.controller.InternetController
import chuckcoughlin.bert.command.CommandController
import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.*
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.common.solver.ForwardSolver
import chuckcoughlin.bert.motor.controller.MotorGroupController
import chuckcoughlin.bert.sql.db.Database
import chuckcoughlin.bert.term.controller.TerminalController
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.io.IOException
import java.time.LocalDate
import java.time.Month
import java.time.Period
import java.util.logging.Logger
import kotlin.math.roundToInt
import kotlin.system.exitProcess

/**
 * The Dispatcher is the distribution hub of the application. Its job is to accept requests from
 * the various peripheral controllers, distribute them to the motor manager channels and post the results.
 * For complicated requests it may invoke the services of the "Solver" and insert
 * internal intermediate requests.
 *
 * For each peripheral controller, the dispatcher uses a pair of communication channels
 * to send and receive message objects.
 */
@DelicateCoroutinesApi
class Dispatcher : Controller {
    // Communication channels
    private val aiRequestChannel            : Channel<MessageBottle>    // Requests for internet
    private val aiResponseChannel           : Channel<MessageBottle>    // Responses from internet
    private val commandRequestChannel      : Channel<MessageBottle>    // Commands from network (Wi-Fi)
    private val commandResponseChannel     : Channel<MessageBottle>    // Response to network (Wi-Fi)
    private val fromInternalController     : Channel<MessageBottle>    // Internal (i.e. local)  controller
    private val toInternalController       : Channel<MessageBottle>
    private val mgcRequestChannel          : Channel<MessageBottle>    // Motor group controller
    private val mgcResponseChannel         : Channel<MessageBottle>
    private val stdinChannel               : Channel<MessageBottle>    // Requests from stdin
    private val stdoutChannel              : Channel<MessageBottle>    // Responses to stdout
    // Controllers
    private val aiController        : InternetController
    private val commandController   : CommandController
    private var internalController  : InternalController
    private val motorGroupController: MotorGroupController
    private var terminalController  : TerminalController

    private val scope = GlobalScope // For long-running coroutines
    private val motorReadyMessage: MessageBottle
    private val internetReadyMessage: MessageBottle
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
                aiController.execute()
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
                        // Also send a synchronization message to the internal controller freeing it to continue
                        mgcResponseChannel.onReceive {     // Handle a serial response
                            if(DEBUG) LOGGER.info(String.format("%s.execute: mgcResponseChannel receive %s(%s) from %s",
                                CLSS, it.type.name,it.text,it.source))
                            toInternalController.send(motorReadyMessage)
                            replyToSource(it)
                        }
                        // When we get a response from the internal controller, dispatch the original request.
                        fromInternalController.onReceive { // The internal controller has completed
                            if(DEBUG) {
                                if(it.type==RequestType.EXECUTE_POSE) {
                                    LOGGER.info(String.format("%s.execute: fromInternalController receive %s(%s %2.0f) from %s",
                                        CLSS, it.type.name,it.arg,it.values[0],it.source))
                                }
                                else if(it.type==RequestType.INTERNET) {
                                    LOGGER.info(String.format("%s.execute: fromInternalController receive %s (%s [%s])",
                                        CLSS, it.type.name,it.text,it.error))
                                }
                                else if(it.type==RequestType.JSON) {
                                    LOGGER.info(String.format("%s.execute: fromInternalController receive %s (%s)",
                                        CLSS, it.type.name,it.jtype.name,it.error))
                                }
                                else {
                                    LOGGER.info(String.format("%s.execute: fromInternalController receive %s (%s) from %s",
                                        CLSS, it.type.name,it.text,it.source))
                                }
                            }
                            dispatchInternalResponse(it)
                        }
                        // A response has arrived from Chat GPT.
                        aiResponseChannel.onReceive {
                            if(DEBUG) LOGGER.info(String.format("%s.execute: aiResponseChannel receive %s(%s) from %s",
                                CLSS, it.type.name,it.text,it.source))
                            toInternalController.send(internetReadyMessage)
                            replyToSource(it)
                        }
                        // The Command request channel contains requests that originate on the connected app (tablet)
                        commandRequestChannel.onReceive {
                            if(DEBUG) {
                                if (it.type == RequestType.JSON)
                                    LOGGER.info(String.format("%s.execute: commandRequestChannel receive %s(%s) from %s",
                                        CLSS, it.type.name, it.jtype.name, it.source))
                            }
                            else {
                                LOGGER.info(String.format("%s.execute: commandRequestChannel receive %s(%s) from %s",
                                    CLSS, it.type.name, it.text, it.source))
                            }
                            dispatchCommandResponse(it)
                        }
                        // The Terminal stdin channel contains requests typed at the terminal
                        stdinChannel.onReceive {
                            if(DEBUG) {
                                if(it.type==RequestType.EXECUTE_POSE) {
                                    LOGGER.info(String.format("%s.execute: stdinChannel receive %s(%s %2.0f) from %s",
                                        CLSS, it.type.name,it.arg,it.values[0],it.source))
                                }
                                else {
                                    LOGGER.info(String.format("%s.execute: stdinChannel receive %s(%s) from %s",
                                        CLSS, it.type.name,it.text,it.source))
                                }
                            }
                            dispatchCommandResponse(it)
                        }
                    }
                    cycleCount += 1
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
    // When setting multiple joints at once, the value is the fraction of max
    private suspend fun initialize() {
        if(DEBUG) LOGGER.info(String.format("%s.initialize: sending messages to establish sanity", CLSS))
        // Set the speed to "normal" rate.
        var msg = MessageBottle(RequestType.SET_MOTOR_PROPERTY )
        msg.jointDynamicProperty = JointDynamicProperty.SPEED
        msg.joint = Joint.NONE
        msg.values[0] = ConfigurationConstants.HALF_SPEED
        msg.source = ControllerType.BITBUCKET
        toInternalController.send(msg)

        // Set the torque to maximum for all joints
        msg = MessageBottle(RequestType.SET_MOTOR_PROPERTY )
        msg.jointDynamicProperty = JointDynamicProperty.TORQUE
        msg.joint = Joint.NONE
        msg.values[0] = ConfigurationConstants.FULL_TORQUE
        msg.source = ControllerType.BITBUCKET
        toInternalController.send(msg)

        // Make sure that all motors are engaged
        msg = MessageBottle(RequestType.SET_MOTOR_PROPERTY )
        msg.jointDynamicProperty = JointDynamicProperty.STATE
        msg.joint = Joint.NONE
        msg.values[0] = ConfigurationConstants.ON_VALUE
        msg.source = ControllerType.BITBUCKET
        toInternalController.send(msg)

        // Read all the joint positions (using both controllers). This fills our
        // internal buffers with the current positions.
        msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
        msg.jointDynamicProperty = JointDynamicProperty.ANGLE
        msg.limb = Limb.NONE
        msg.joint = Joint.NONE
        msg.source = ControllerType.BITBUCKET
        msg.control.delay = 1000 // 1 sec delay
        toInternalController.send(msg)

        // Bring any joints that are outside sane limits into compliance
        msg = MessageBottle(RequestType.INITIALIZE_JOINTS)
        msg.source = ControllerType.BITBUCKET
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
                aiController.shutdown()
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
     * Analyze an incoming message from the command (Wi-Fi) or terminal channels. Some requests
     * are handled immediately. Any motor requests are first passed to the internal controller to
     * handle delay or conflict issues.
     */
    private suspend fun dispatchCommandResponse(msg : MessageBottle) {
        if(DEBUG) LOGGER.info(String.format("%s.dispatchCommandResponse %s from %s",CLSS,msg.type.name,msg.source))
        if(isLocalRequest(msg)) {
            // Handle local request -create response unless type set to NONE
            //if(DEBUG) LOGGER.info(String.format("%s.dispatchCommandResponse %s is local (%s %s)",
            //        CLSS,msg.type.name,msg.jointDefinitionProperty.name,msg.jointDynamicProperty.name))
            val response: MessageBottle = handleLocalRequest(msg)
            if( response.type!=RequestType.NONE &&
                response.type!=RequestType.HANGUP  ) replyToSource(response)
        }
        // Queue all internet requests
        else if( msg.type== RequestType.INTERNET) {
            toInternalController.send(msg)
        }
        // "motor" requests are those that need to be queued on the internal controller
        // and, perhaps preprocessed into multiple messages (by limb, for example)
        // before sending on to the MotorGroupController
        else if( isMotorRequest(msg) ) {
            toInternalController.send(msg)
        }
        else {
            LOGGER.info(String.format("%s.dispatchCommandResponse %s from %s is unhandled",
                                        CLSS,msg.type.name,msg.source))
            if( msg.type==RequestType.JSON)
                msg.error = String.format("internal error, %s (%s) message is unhandled in dispatcher",msg.type.name,msg.jtype.name)
            else
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
        if(msg.type== RequestType.EXECUTE_ACTION) {
            toInternalController.send(motorReadyMessage)  // Execute Action is just a marker at this point
            replyToSource(msg)
            // If there is a follow-on action, then add it to the queue. Reuse original message
            val nextAction = Database.getFollowOnAction(msg.arg)
            if(nextAction!=null) {
                msg.arg = nextAction
                toInternalController.send(msg)
            }
        }
        else if(isMotorRequest(msg)) {
            mgcRequestChannel.send(msg)
        }
        else if(msg.type==RequestType.INTERNET) {
            aiRequestChannel.send(msg)
        }
        else if(msg.type==RequestType.HEARTBEAT) {
            // Do nothing
        }
        else if(msg.type==RequestType.JSON) {
            if(commandController.connected) replyToSource(msg)  // Update animation
            toInternalController.send(motorReadyMessage)
        }
        else {
            LOGGER.info(String.format("%s.dispatchInternalResponse %s from %s is unhandled",
                    CLSS,msg.type.name,msg.source))
            msg.error = String.format("internal error, %s message was not handled by the dispatcher",msg.type.name)
            replyToSource(msg)
        }
    }


    // Create a response for a request that can be handled immediately, that is without
    // reference to the motors. The response is simply the original request
    // with altered text to return to the user.
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        if( request.error==BottleConstants.NO_ERROR) {
            if (request.type == RequestType.COMMAND) {
                val command = request.command
                if(DEBUG) LOGGER.info(String.format("%s.handleLocalRequest: command=%s", CLSS, command.name))
                if (command == CommandType.CREATE_ACTION) {
                    val actName: String = request.arg.lowercase()
                    val series = request.text.lowercase()
                    Database.createAction(actName, series)
                    request.text = String.format("To %s is to execute a series of %s poses", actName, series)
                }
                else if (command == CommandType.CREATE_NEXT_ACTION) {
                    val actName: String = request.arg.lowercase()
                    val followon = request.text.lowercase()
                    if( Database.actionExists(actName) && Database.actionExists(followon)) {
                        Database.defineNextAction(actName, followon)
                        request.text=String.format("After %s run %s", actName, followon)
                    }
                    else {
                        request.error = "Both actions $actName and $followon must exist in order to define a follow on"
                    }
                }
                else if (command == CommandType.CREATE_POSE) {
                    val poseName: String = request.arg.lowercase()
                    val index = request.values[0].toInt()
                    Database.createPose(RobotModel.motorsByJoint, poseName, index)
                    request.text = "I recorded pose $poseName $index"
                }
                // Also delete any poses associated with that action
                else if (command == CommandType.DELETE_ACTION) {
                    val name = request.arg
                    if (Database.actionExists(name)) {
                        val poses = Database.getPosesForAction(name)
                        for( def in poses ) {
                            Database.deletePose(def.name,def.index)
                        }
                        Database.deleteAction(name)
                        request.text = "I deleted action $name"
                    }
                    else  {
                        request.error = "Action $name doesn't exist"
                    }
                }
                else if (command == CommandType.DELETE_FACE) {
                    val name = request.arg
                    if (Database.faceExists(name)) {
                        Database.deleteFace(name)
                        request.text = "I have now forgotten $name"
                    }
                    else {
                        request.error = "I don't know $name"
                    }
                }
                // If the index is missing, delete all poses of the given name
                else if (command == CommandType.DELETE_POSE) {
                    val name = request.arg
                    if( request.values.size==0 ) {
                        Database.deletePose(name)
                    }
                    else {
                        val index = request.values[0].toInt()
                        if (Database.poseExists(name, index)) {
                            Database.deletePose(name, index)
                        }
                    }
                }
                else if (command == CommandType.STOP_ACTION) {
                    val actName: String = request.arg.lowercase()
                    if(Database.actionExists(actName) || Database.actionSeriesExists(actName)) {
                        Database.stopAction(actName)
                        request.text=String.format("Stop action %s", actName)
                    }
                    else  {
                        request.error = "Action $actName doesn't exist"
                    }
                }
                else if (command == CommandType.HALT) {
                    request.type = RequestType.NONE  // Suppress a response
                    exitProcess(0) // Rely on ShutdownHandler
                }
                else if (command == CommandType.SHUTDOWN) {
                    try {
                        val commands = arrayOf("sudo poweroff")
                        val rt = Runtime.getRuntime()
                        rt.exec(commands)
                    }
                    catch (ioe: IOException) {
                        LOGGER.warning(String.format("%s.handleLocalRequest: Powerdown error (%s)",
                            CLSS, ioe.message))
                    }
                }
                else {
                    LOGGER.warning(String.format("%s.handleLocalRequest: Unhandled command (%s)",
                        CLSS, request.command.name))
                }
            }
            // The following requests simply use the current positions of the motors, whatever they are
            else if (request.type==RequestType.GET_EXTREMITY_DIRECTION) {
                if(DEBUG) LOGGER.info(String.format("%s.handleLocalRequest: get direction appendage=%s joint=%s", CLSS, request.appendage.name,request.joint.name))
                var text:String
                if( request.joint==Joint.NONE ) {
                    val appendage = request.appendage
                    val xyz: DoubleArray = ForwardSolver.computeDirection(appendage.name)
                    text = String.format("my %s is aimed at %2.2f %2.2f %2.2f",
                        appendage.name, xyz[0], xyz[1], xyz[2])
                }
                else {
                    val joint = request.joint
                    val xyz: DoubleArray = ForwardSolver.computeDirection(joint.name)
                    text = String.format(
                        "My %s is oriented %2.2f and %2.2f degrees from the reference frame x and y axes, respectively",
                        Joint.toText(joint), xyz[0], xyz[1])
                }
                request.text = text
            }
            // The location in physical coordinates from the center of the robot.
            else if (request.type==RequestType.GET_EXTREMITY_POSITION) {
                var text:String
                if( request.joint==Joint.NONE ) {
                    val appendage = request.appendage
                    val xyz: Point3D = ForwardSolver.computePosition(appendage.name)
                    text = String.format("my %s is located at %2.2f %2.2f %2.2f millimeters",
                        appendage.name, xyz.x, xyz.y, xyz.z)
                    request.values[0] = xyz.x
                    request.values[1] = xyz.y
                    request.values[2] = xyz.z
                    if(DEBUG) LOGGER.info(String.format("%s.handleLocalRequest: get appendage %s location = %s", CLSS,request.appendage.name,text))
                }
                else {
                    val joint = request.joint
                    val xyz: Point3D = ForwardSolver.computePosition(joint.name)
                    text = String.format(
                        "My %s joint is at %2.2f %2.2f %2.2f millimeters",
                        Joint.toText(joint), xyz.x, xyz.y, xyz.z)
                    request.values[0] = xyz.x
                    request.values[1] = xyz.y
                    request.values[2] = xyz.z
                    if(DEBUG) LOGGER.info(String.format("%s.handleLocalRequest: get joint %s location = %s", CLSS,request.joint.name,text))
                }
                request.text = text
            }
            else if (request.type.equals(RequestType.METRIC)) {
                if(DEBUG)LOGGER.info(String.format("%s.handleLocalRequest: metric=%s", CLSS, request.metric))
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
                    // LIST implies we look at the JsonType and return a comma-separated list of names
                    MetricType.LIST -> {
                        when (request.jtype) {
                            JsonType.FACE_NAMES -> text = "I know " + Database.getFaceNames()
                            JsonType.MOTOR_DYNAMIC_PROPERTIES -> text = "Each joint has " + JointDynamicProperty.names()
                            JsonType.MOTOR_STATIC_PROPERTIES -> text = "Each joint has a " + JointDynamicProperty.names()
                            JsonType.END_EFFECTOR_NAMES -> text = "I have these end effectors:  " + Appendage.nameList()
                            JsonType.JOINT_NAMES -> text = "My joints are " + Joint.nameList()
                            JsonType.LIMB_NAMES -> text = "My limbs are " + Limb.nameList()
                            JsonType.JOINT_COORDINATES -> text = "Joint positiond are " + ForwardSolver.jointCoordinatesToJson()
                            JsonType.POSE_NAMES -> text = "I know poses " + Database.getPoseNames()
                            JsonType.ACTION_NAMES -> text = "I can " + Database.getActionNames()
                            else -> {
                                request.error = "badly formed metric list request"
                                text = ""
                            }
                        }
                    }

                    else -> request.error = String.format("I can't get the value of %s", metric.name)
                }
                request.text = text
            }
            // These are the definition properties
            else if (request.type == RequestType.GET_MOTOR_PROPERTY &&
                request.jointDynamicProperty == JointDynamicProperty.NONE) {
                val joint = request.joint
                val mc = RobotModel.motorsByJoint[joint]!!
                if (request.jointDefinitionProperty == JointDefinitionProperty.ID) {
                    request.text = String.format("The id of my %s is %d", Joint.toText(joint), mc.id)
                }
                else if (request.jointDefinitionProperty == JointDefinitionProperty.OFFSET) {
                    request.text = String.format("The angular offset of my %s is %d", Joint.toText(joint), mc.offset)
                }
                else if (request.jointDefinitionProperty == JointDefinitionProperty.ORIENTATION) {
                    request.text = String.format("The orientation of my %s is %s", Joint.toText(joint),
                        if (mc.isDirect) "direct" else "indirect")
                }
                else if (request.jointDefinitionProperty == JointDefinitionProperty.MOTORTYPE) {
                    request.text = String.format("The motor type of my %s is %s", Joint.toText(joint), mc.type.name)
                }
            }
            else if (request.type == RequestType.GET_MOTOR_PROPERTY) {
                val joint = request.joint
                val mc = RobotModel.motorsByJoint[joint]!!
                if (request.jointDynamicProperty == JointDynamicProperty.MAXIMUMANGLE) {
                    request.text = String.format("The maximum angle of my %s is %2.0f degrees",
                        Joint.toText(joint), mc.maxAngle)
                }
                else if (request.jointDynamicProperty == JointDynamicProperty.MINIMUMANGLE) {
                    request.text = String.format("The minimum angle of my %s is %2.0f degrees",
                        Joint.toText(joint), mc.minAngle)
                }
                else if (request.jointDynamicProperty == JointDynamicProperty.MAXIMUMSPEED) {
                    request.text = String.format("The maximum speed of my %s is %2.0f degrees per second",
                            Joint.toText(joint), mc.maxSpeed)
                }
                else if (request.jointDynamicProperty == JointDynamicProperty.MAXIMUMTORQUE) {
                    request.text = String.format("The maximum torque of my %s is %2.2f newton meters",
                            Joint.toText(joint), mc.maxTorque)
                }
                else if (request.jointDynamicProperty == JointDynamicProperty.RANGE) {
                    request.text = String.format("I can move my %s from %2.0f to %2.0f",
                        Joint.toText(joint),mc.minAngle,mc.maxAngle)
                }
            }
            // List various entities
            else if (request.type.equals(RequestType.JSON)) {
                LOGGER.info(String.format("%s.handleLocalRequest: JSON type=%s", CLSS, request.jtype.name))
                val jtype: JsonType = request.jtype
                var text = ""
                when (jtype) {
                    // List the names of different kinds of motor properties
                    JsonType.END_EFFECTOR_NAMES -> {
                        text = URDFModel.endEffectorNamesToJSON()
                    }

                    JsonType.FACE_NAMES -> {
                        text = Database.faceNamesToJSON()
                    }

                    JsonType.JOINT_IDS -> {
                        text = RobotModel.idsToJSON()
                    }

                    JsonType.JOINT_NAMES -> {
                        text = URDFModel.jointsToJSON()
                    }

                    JsonType.JOINT_OFFSETS -> {
                        text = RobotModel.offsetsToJSON()
                    }

                    JsonType.JOINT_ORIENTATIONS -> {
                        text = RobotModel.orientationsToJSON()
                    }
                    JsonType.JOINT_ANGLES -> {
                        text = RobotModel.anglesToJSON()
                    }

                    JsonType.JOINT_SPEEDS -> {
                        text = RobotModel.speedsToJSON()
                    }

                    JsonType.JOINT_STATES -> {
                        text = RobotModel.statesToJSON()
                    }

                    JsonType.JOINT_TEMPERATURES -> {
                        text = RobotModel.temperaturesToJSON()
                    }

                    JsonType.JOINT_TORQUES -> {
                        text = RobotModel.torquesToJSON()
                    }

                    JsonType.JOINT_VOLTAGES -> {
                        text = RobotModel.voltagesToJSON()
                    }

                    JsonType.JOINT_TYPES -> {
                        text = RobotModel.typesToJSON()
                    }
                    JsonType.JOINT_COORDINATES -> {
                        text = ForwardSolver.jointCoordinatesToJson()
                    }
                    JsonType.LIMB_NAMES -> {
                        text = RobotModel.limbsToJSON()
                    }
                    JsonType.MOTOR_DYNAMIC_PROPERTIES -> {
                        text = JointDynamicProperty.toJSON()
                    }

                    JsonType.MOTOR_GOALS -> {
                        text = "Dispatcher: error - resolve MOTOR_GOALS in motor controller"
                    }

                    JsonType.MOTOR_LIMITS -> {
                        text = "Dispatcher: error - resolve MOTOR_LIMITS in motor controller"
                    }

                    JsonType.MOTOR_PROPERTIES -> {
                        text = RobotModel.propertiesToJSON()
                    }

                    JsonType.MOTOR_STATIC_PROPERTIES -> {
                        text = JointDefinitionProperty.toJSON()
                    }

                    JsonType.POSE_DETAILS -> {
                        text = Database.poseDetailsToJSON(request.arg, request.values[0].roundToInt())
                    }

                    JsonType.POSE_NAMES -> {
                        text = Database.poseNamesToJSON()
                    }

                    else -> request.error = String.format("I can't get the names of %s", jtype.name)
                }
                request.text = text
            }
            // We are here because there is a range error
            else if (request.type == RequestType.SET_MOTOR_PROPERTY &&
                request.jointDynamicProperty == JointDynamicProperty.ANGLE) {
                val joint = request.joint
                val mc = RobotModel.motorsByJoint[joint]!!
                if (request.values[0] > mc.maxAngle) {
                    request.error = String.format("I can only move my %s to %2.0f degrees",
                        Joint.toText(joint),
                        mc.maxAngle)
                }
                else if (request.values[0] < mc.minAngle) {
                    request.error = String.format("I can only move my %s to %2.0f degrees",
                        Joint.toText(joint),
                        mc.minAngle)
                }
            }
            // We are here because of a value error
            else if (request.type == RequestType.SET_MOTOR_PROPERTY &&
                request.jointDynamicProperty == JointDynamicProperty.SPEED) {
                val joint = request.joint
                val mc = RobotModel.motorsByJoint[joint]!!
                if (request.values[0] > mc.maxSpeed) {
                    request.error = String.format("I can only move my %s %2.0f degrees per second",
                        Joint.toText(joint),mc.maxSpeed)
                }
            }
            else if (request.type == RequestType.SET_MOTOR_PROPERTY &&
                request.jointDynamicProperty == JointDynamicProperty.TORQUE) {
                val joint = request.joint
                val mc = RobotModel.motorsByJoint[joint]!!
                if (request.values[0] > mc.maxTorque) {
                    request.error = String.format("%s torque cannot exceed %2.0f newton meters ",Joint.toText(joint),
                                                                        mc.maxTorque)
                }
            }
            else if (request.type == RequestType.SET_MOTOR_PROPERTY &&
                request.jointDynamicProperty == JointDynamicProperty.LOAD) {
                request.error = String.format("load is a read only property")
            }
        }
        return request
    }


    // Local requests are those that can be handled immediately
    // without forwarding to the motor controllers. This includes
    // database queries, and some error conditions.
    private fun isLocalRequest(request: MessageBottle): Boolean {
        if (request.type==RequestType.COMMAND ||
            request.type==RequestType.GET_EXTREMITY_DIRECTION||
            request.type==RequestType.GET_EXTREMITY_POSITION ||
            request.type==RequestType.METRIC ||
            request.type==RequestType.HANGUP    ) {
            return true
        }
        // These are the definition properties
        else if( request.type == RequestType.JSON ) {
            if( request.jtype==JsonType.MOTOR_GOALS ||
                request.jtype==JsonType.MOTOR_LIMITS ) {
                    return false
            }
            else {
                return true
            }
        }
        else if( request.type == RequestType.GET_MOTOR_PROPERTY &&
            request.joint == Joint.IMU ) {
            request.error = String.format("the IMU has no readable properties")
            return true
        }
        else if( request.type == RequestType.GET_MOTOR_PROPERTY &&
                 request.jointDefinitionProperty != JointDefinitionProperty.NONE )  {
            return true
        }
        // These "dynamic" properties are gettable from bert.xml
        else if( request.type == RequestType.GET_MOTOR_PROPERTY &&
                  ( request.jointDynamicProperty == JointDynamicProperty.MAXIMUMANGLE ||
                    request.jointDynamicProperty == JointDynamicProperty.MINIMUMANGLE ||
                    request.jointDynamicProperty == JointDynamicProperty.MAXIMUMSPEED ||
                    request.jointDynamicProperty == JointDynamicProperty.MAXIMUMTORQUE ||
                    request.jointDynamicProperty == JointDynamicProperty.RANGE)) {
            return true
        }
        // Some very specific boundary errors --- set the error text
        else if( request.type == RequestType.SET_LIMB_PROPERTY &&
                 request.jointDynamicProperty == JointDynamicProperty.ANGLE ) {
            val joint = request.joint
            if( joint==Joint.NONE ) {
                request.error = String.format("setting the same angle to all motors on a limb is not allowed")
                return true
            }
        }
        else if( request.type == RequestType.SET_MOTOR_PROPERTY &&
            request.joint == Joint.IMU ) {
                request.error = String.format("the IMU has no settable properties")
                return true
        }
        else if( request.type == RequestType.SET_MOTOR_PROPERTY &&
                 request.jointDynamicProperty == JointDynamicProperty.ANGLE ) {
            val joint = request.joint
            if( joint==Joint.NONE ) {
                request.error = String.format("setting the same angle to all motors is not allowed")
                return true
            }
            val mc = RobotModel.motorsByJoint[joint]!!
            if( request.values[0]>mc.maxAngle ) {
                request.error = String.format("the maximum angle for %s is %2.0f degrees",joint.name,mc.maxAngle)
                return true
            }
            else if( request.values[0]<mc.minAngle) {
                request.error = String.format("the minimum angle for %s is %2.0f degrees",joint.name,mc.minAngle)
                return true
            }
        }
        else if( request.type == RequestType.SET_MOTOR_PROPERTY &&
                 request.jointDynamicProperty == JointDynamicProperty.SPEED ) {
            val joint = request.joint
            for( mc in RobotModel.motorsByJoint.values ) {
                if( joint==Joint.NONE || mc.joint==joint) {
                    if (request.values[0] > mc.maxSpeed) {
                        request.error = String.format("the maximum speed for %s is %2.0f degrees per second",
                            joint.name,mc.maxSpeed)
                        return true
                    }
                }
            }
        }
        else if( request.type == RequestType.SET_MOTOR_PROPERTY &&
                 request.jointDynamicProperty == JointDynamicProperty.TORQUE ) {
            val joint = request.joint
            for( mc in RobotModel.motorsByJoint.values ) {
                if( joint==Joint.NONE || mc.joint==joint) {
                    if (request.values[0] > mc.maxTorque) {
                        request.error = String.format("the maximum torque for %s is %2.2f newton meters",Joint.toText(joint),mc.maxTorque)
                        return true
                    }
                }
            }
        }
        else if( request.type == RequestType.NOTIFICATION ) {
            request.source = ControllerType.DISPATCHER
            return true
        }
        else if( request.type == RequestType.INTERNET ) {
            return false
        }
        // If there was any error caught by the parser (unless headed for internet)
        else if( !request.error.equals(BottleConstants.NO_ERROR) ) {
            return true
        }
        return false
    }

    // These are requests that can be processed directly by the group controller
    // to be forwarded to the proper motor controller. They have needed pre-processing
    // by the internal controller
    private fun isMotorRequest(request: MessageBottle): Boolean {
        if (request.type==RequestType.EXECUTE_ACTION ||   // Sort of
            request.type==RequestType.EXECUTE_POSE ||
            request.type==RequestType.GET_MOTOR_PROPERTY ||
            request.type==RequestType.INITIALIZE_JOINTS  ||
            request.type==RequestType.PLACE_END_EFFECTOR  ||
            request.type==RequestType.READ_MOTOR_PROPERTY ||
            request.type==RequestType.RESET ||
            request.type==RequestType.SET_LIMB_PROPERTY ||
            request.type==RequestType.SET_MOTOR_PROPERTY) {
            return true
        }
        else if(request.type==RequestType.JSON ) {
            if( request.jtype==JsonType.MOTOR_LIMITS ||
                request.jtype==JsonType.MOTOR_GOALS ||
                request.jtype==JsonType.MOVE_JOINTS ) {
                return true
            }
        }

        return false
    }
    /**
     * The sub-controller has posted a response meant to be returned to the controller
     * that had originated the request.
     */
    private suspend fun replyToSource(response: MessageBottle) {
        val source = response.source
        if(response.type==RequestType.JSON ) {
            LOGGER.info(String.format("%s.replyToSource: Forwarding %s (%s) to %s",
                CLSS, response.type.name, response.jtype.name, source))
        }
        else {
            LOGGER.info(String.format("%s.replyToSource: Forwarding %s (%s) to %s",
                CLSS, response.type.name, response.text, source))
        }

        if (source.equals(ControllerType.COMMAND)) {
            commandResponseChannel.send(response)
        }
        else if (source.equals(ControllerType.TERMINAL)) {
            stdoutChannel.send(response)
        }
        else if (source.equals(ControllerType.BITBUCKET)) {
              // Do nothing
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
        startMessage.source = ControllerType.DISPATCHER
        if(RobotModel.useTerminal) stdoutChannel.send(startMessage)
        if(RobotModel.useNetwork)  commandController.startMessage = startMessage.text  // Would be better to use channel
    }
    private fun exponentiallyWeightedMovingAverage(currentValue: Double,previousValue: Double): Double {
        return (1.0 - WEIGHT) * currentValue + WEIGHT * previousValue
    }
    /**
     * Select a random phrase from the list.
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
     *    Command - network (Wi-Fi) connection to the tablet
     *    Internal - where multiple or repeating messages are required for a single user request.
     *    MotorController - make serial requests to the motors
     *    Terminal - communicate directly with the user console
     */
    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_DISPATCHER)
        aiRequestChannel = Channel<MessageBottle>()
        aiResponseChannel= Channel<MessageBottle>()
        commandRequestChannel = Channel<MessageBottle>()
        commandResponseChannel= Channel<MessageBottle>()
        fromInternalController  = Channel<MessageBottle>()
        toInternalController = Channel<MessageBottle>()
        mgcRequestChannel  = Channel<MessageBottle>()
        mgcResponseChannel = Channel<MessageBottle>()
        stdinChannel  = Channel<MessageBottle>()
        stdoutChannel = Channel<MessageBottle>()

        aiController          = InternetController(aiRequestChannel,aiResponseChannel)
        commandController     = CommandController(commandRequestChannel,commandResponseChannel)
        internalController    = InternalController(fromInternalController,toInternalController)
        motorGroupController  = MotorGroupController(mgcRequestChannel,mgcResponseChannel)
        terminalController    = TerminalController(stdinChannel,stdoutChannel)
        motorReadyMessage     = MessageBottle(RequestType.READY)  // Reusable synchronization message
        internetReadyMessage     = MessageBottle(RequestType.READY)  // Reusable synchronization message
        motorReadyMessage.source = ControllerType.MOTOR
        internetReadyMessage.source = ControllerType.INTERNET
        running = false
        name = RobotModel.getProperty(ConfigurationConstants.PROPERTY_ROBOT_NAME)
        val cadenceString: String = RobotModel.getProperty(ConfigurationConstants.PROPERTY_CADENCE, "1000") // ~msecs
        try {
            cadence = cadenceString.toInt()
        }
        catch (nfe: NumberFormatException) {
            LOGGER.warning(String.format("%s.init: Cadence must be an integer (%s)", CLSS, nfe.localizedMessage))
        }
        LOGGER.info(String.format("%s.init: cadence %d msecs", CLSS, cadence))
    }
}