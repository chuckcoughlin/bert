/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import jssc.SerialPort
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*
import java.util.logging.Logger

/**
 * The MotorGroupController receives requests from the dispatcher that have to do with
 * the Dynamixel motors. This controller then dispenses the request to the multiple
 * MotorControllers then receives results via a call-back. When all motor controllers
 * have reported, results are then forwarded to the dispatcher.
 *
 * The same request channel is used for each of the MotorControllers. On receipt of a
 * message it is the responsibility of the motor controller to ignore inappropriate
 * requests.
 *
 * On initialization, the system architecture is checked to determine if
 * this is being run in a development or production environment. If
 * development, then responses are simulated without any direct serial
 * requests being made.
 */

class MotorGroupController(req: Channel<MessageBottle>, rsp: Channel<MessageBottle>) : Controller {
    @DelicateCoroutinesApi
    private val scope = GlobalScope // For long-running coroutines
    val motorControllers: MutableMap<String,MotorController>
    private val lowerController: MotorController
    private val upperController: MotorController
    private var parentRequestChannel  = req   // Dispatcher->MGC (serial commands)
    private var parentResponseChannel = rsp   // MGC->Dispatcher (results of serial commands)

    // It would be nice to have an indeterminate number of motor controllers, but I haven't
    // found a way of executing the select() in a generic manner, broadcast channel is obsolete.
    private val lowerRequestChannel  = Channel<MessageBottle>()   // For joints in lower controller
    private val lowerResponseChannel = Channel<MessageBottle>()
    private val upperRequestChannel  = Channel<MessageBottle>()   // For joints in upper controller
    private val upperResponseChannel = Channel<MessageBottle>()
    private val motorNameById: MutableMap<Int, String>
    private var messageId: Long
    private var pendingMessages: MutableMap<Long,MutableList<String>>
    var running: Boolean
    private var job: Job
    var controllerCount: Int = 0

    /**
     * Start a motor controller for each port.
     */
    @DelicateCoroutinesApi
    override suspend fun execute() {
        if (!running) {
            running = true
            LOGGER.info(String.format("%s.execute: started...", CLSS))
            // Start the individual motor controllers
            lowerController.execute()
            upperController.execute()

            // Port is open, now use it.
            job = scope.launch(Dispatchers.IO) {
                while (running) {
                    select<MessageBottle> {
                        /**
                         * On receipt of a message from a motor controller,
                         * accumulate, if necessary, then forward to the
                         * dispatcher. NOTE: Each controller returns the
                         * same request object.
                         */
                        lowerResponseChannel.onReceive() {
                            handleControllerResponse(LOWER,it)
                        }
                        upperResponseChannel.onReceive() {
                            handleControllerResponse(UPPER,it)
                        }
                        /*
                         * The parent request is a motor command. Forward it
                         * to one or both of the motor controllers.
                         */
                        parentRequestChannel.onReceive() {
                            processRequest(it)
                        }
                    }
                }
            }
        }
        else {
            LOGGER.warning(String.format("%s.execute: attempted to start, but already running...", CLSS))
        }
    }

    override suspend fun shutdown() {
        if (running) {
            running = false
            job.cancel()
        }
    }

    /**
     * There are 3 kinds of messages that we process:
     * 1) Requests that can be satisfied from information in our static
     * configuration. Compose results and return immediately.
     * 2) Commands or requests that can be satisfied by a single port handler,
     * for example requesting status or commanding control of a single joint.
     * In this case, we blindly send the request to all port handlers, but
     * expect a reply from only one.
     * 3) Global commands or requests. These apply to all motor groups. Examples
     * include setting a pose or commanding an action, requesting positional state.
     * These requests are satisfied by sending the same request to all port handlers.
     * The single unique request object collects partial results from all
     * controllers. When complete, it is passed here and forwarded to the dispatcher.
     *
     * @param request
     * @return the response, usually containing current joint positions.
     */
    suspend fun processRequest(request: MessageBottle):MessageBottle {
        LOGGER.info(String.format("%s.processRequest: processing %s",CLSS,request.type.name))
        if (canHandleImmediately(request) ) {
            parentResponseChannel.send(createImmediateResponse(request))
        }
        else if (!RobotModel.useSerial) {
            parentResponseChannel.send(simulateResponseForRequest(request))
        }
        else {
            pendingMessages.put(request.id,mutableListOf<String>())
            lowerRequestChannel.send(request)     // All motor controllers receive
            // delay(6000)   // Makes is so motor controllers don't overlap
            upperRequestChannel.send(request)
        }
        return request
    }
    /**
     * When the one of the controllers has reported its response, we check to see if we need to
     * wait until all controllers have reported.
     * For this to work, it is important that work on the response object be synchronized.
     * @param rsp the original request
     */
    suspend fun handleControllerResponse(cname:String,response: MessageBottle):MessageBottle {
        val id = response.id
        LOGGER.info(String.format("%s.handleControllerResponse: %s processing %s (%s)",
            CLSS,cname,response.type.name,response.text))
        if( isSingleControllerRequest(response)) {
            parentResponseChannel.send(response)
        }
        else {
            val list = pendingMessages.get(id)!!
            list.add(cname)
            if (DEBUG) LOGGER.info(String.format("%s.handleControllerResponse: pending %s ( at %d of %d)",
                CLSS, response.type.name, list.size, controllerCount))
            if(list.size >= controllerCount) {
                parentResponseChannel.send(response)
                pendingMessages.remove(id)
            }
        }
        return response
    }

    // =========================== Private Helper Methods =====================================
    // Queries of fixed properties of the motors are the kinds of requests that can be handled
    // immediately. Results are created from the original configuration file.
    // We also create error messages some requests that are illegal
    private fun canHandleImmediately(request: MessageBottle): Boolean {
        if (request.type.equals(RequestType.COMMAND) &&
            request.command.equals(CommandType.RESET) ) {
            return true
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY)) {
            // Some properties cannot be set. Catch them here in order to formulate an error response.
            val prop = request.jointDynamicProperty
            if (prop==JointDynamicProperty.MAXIMUMANGLE ||
                prop== JointDynamicProperty.MINIMUMANGLE ) {
                return true
            }
        }
        else if (request.type.equals(RequestType.SET_MOTOR_PROPERTY)) {
            // Some properties cannot be set. Catch them here in order to formulate an error response.
            val prop = request.jointDefinitionProperty
            if( prop == JointDefinitionProperty.ID ||
                prop == JointDefinitionProperty.MOTORTYPE ||
                prop == JointDefinitionProperty.OFFSET ||
                prop == JointDefinitionProperty.ORIENTATION ) {
                return true
            }
        }
        return false
    }

    // The requests handled heere must correspond exactly with those listed in canHandleImmediately.
    // to return directly to the user. These jointValues are obtained from the initial configuration
    private fun createImmediateResponse(request: MessageBottle): MessageBottle {
        var text: String = ""
        val type = request.type
        val command = request.command
        val jtype = request.jtype
        if (type.equals(RequestType.COMMAND) &&
            command.equals(CommandType.RESET) ) {
            pendingMessages.clear()
            request.text = "I have been reset"
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY)) {
            val property = request.jointDynamicProperty.toString()
            request.error = "I cannot change " + property.lowercase(Locale.getDefault()) + " for all joints in the limb"
        }
        else if (request.type.equals(RequestType.SET_MOTOR_PROPERTY)) {
            val property = request.jointDynamicProperty.toString()
            request.error = "I cannot change a motor " + property.lowercase(Locale.getDefault())
        }
        return request
    }

    /**
     * @param msg the request
     * @return true if this is the type of request satisfied by a single controller.
     */
    private fun isSingleControllerRequest(msg: MessageBottle): Boolean {
        if (msg.type.equals(RequestType.GET_GOALS) ||
            msg.type.equals(RequestType.GET_LIMITS) ||
            msg.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
            msg.type.equals(RequestType.SET_LIMB_PROPERTY)  ) {
            return true
        }
        else if( msg.type.equals(RequestType.SET_MOTOR_PROPERTY) ) {
            if( !msg.joint.equals(Joint.NONE)) {   // Applies to all joints
                return true
            }
        }
        return false
    }

    // When in development mode with no access to actual motors, simulate something reasonable as a response.
    private fun simulateResponseForRequest(request: MessageBottle): MessageBottle {
        val requestType = request.type
        var text = ""
        if (requestType.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val joint: Joint = request.joint
            val jointName: String = Joint.toText(joint)
            val mcmaybe: MotorConfiguration? = RobotModel.motorsByJoint[joint]
            val mc:MotorConfiguration = mcmaybe!!
            if( request.jointDynamicProperty==JointDynamicProperty.NONE ) {
                // Definition property
                val property = request.jointDefinitionProperty
                when(property) {
                    JointDefinitionProperty.ID -> {
                        val id = mc.id
                        text = "The id of my $jointName is $id"
                    }
                    JointDefinitionProperty.MOTORTYPE -> TODO()
                    JointDefinitionProperty.OFFSET -> TODO()
                    JointDefinitionProperty.ORIENTATION -> TODO()
                    JointDefinitionProperty.NONE -> text = ""
                }
            }
            else {
                val property = request.jointDynamicProperty
                when (property) {
                    JointDynamicProperty.ANGLE -> {
                        val position = 0
                        text = "The position of my $jointName is $position"
                    }
                    JointDynamicProperty.MAXIMUMANGLE -> TODO()
                    JointDynamicProperty.MINIMUMANGLE -> TODO()
                    JointDynamicProperty.RANGE -> TODO()
                    JointDynamicProperty.SPEED -> TODO()
                    JointDynamicProperty.STATE -> TODO()
                    JointDynamicProperty.TEMPERATURE -> TODO()
                    JointDynamicProperty.TORQUE -> TODO()
                    JointDynamicProperty.VOLTAGE -> TODO()
                    JointDynamicProperty.NONE -> text = ""
                }
            }
            request.text = text
        }
        else {
            LOGGER.warning(String.format("%s.simulateResponseForRequest: Request type %s not handled",
                CLSS,requestType.name))
        }
        return request
    }

    private val CLSS = "MotorGroupController"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    override val controllerName = CLSS
    override val controllerType = ControllerType.MOTORGROUP
    private val UPPER = "upper"  // Must match bert.xml
    private val LOWER = "lower"  // Must match bert.xml

    /**
     * Create the "serial" controllers that handle Dynamixel motors. We launch multiple
     * instances each running in its own thread. Each controller handles a group of
     * motors all communicating on the same serial port.
     */
    init  {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_GROUP)
        motorNameById    = mutableMapOf<Int, String>()
        messageId = 0
        motorControllers = mutableMapOf<String, MotorController>()
        pendingMessages  = mutableMapOf<Long,MutableList<String>>()
        running = false
        LOGGER.info(String.format("%s: os.arch = %s", CLSS, System.getProperty("os.arch"))) // x86_64
        LOGGER.info(String.format("%s: os.name = %s", CLSS, System.getProperty("os.name"))) // Mac OS X

        var device = RobotModel.getDeviceForMotorController(LOWER)
        LOGGER.info(String.format("%s.init: %s controller has port %s", CLSS, LOWER,device))
        var port:SerialPort = SerialPort(device)
        lowerController = MotorController(LOWER,port,lowerRequestChannel,lowerResponseChannel)
        motorControllers.put(LOWER,lowerController)

        device = RobotModel.getDeviceForMotorController(UPPER)
        LOGGER.info(String.format("%s.init: %s controller has port %s", CLSS, UPPER, device))
        port = SerialPort(device)
        upperController = MotorController(UPPER,port,upperRequestChannel,upperResponseChannel)
        motorControllers.put(UPPER,upperController)

        if(RobotModel.useSerial) {
            for(controller in motorControllers.values) {
                // Add configurations to the controller for each motor in the group
                val joints: List<Joint> = RobotModel.getJointsForMotorController(controller.controllerName)
                for (joint in joints) {
                    val motor: MotorConfiguration? = RobotModel.motorsByJoint[joint]
                    if (motor != null) {
                        if(DEBUG) LOGGER.info(String.format("%s.init: Added motor %s: %s",CLSS,controller.controllerName,joint.name))
                        controller.putMotorConfiguration(joint, motor)
                        motorNameById[motor.id] = joint.name
                    }
                    else {
                        LOGGER.warning(String.format("%s.initialize: Motor %s not found in %s",
                            CLSS,joint.name, controller.controllerName))
                    }
                }
            }
            controllerCount = motorControllers.size
        }
        else {
            controllerCount = 0
        }
        job=Job()
    }
}