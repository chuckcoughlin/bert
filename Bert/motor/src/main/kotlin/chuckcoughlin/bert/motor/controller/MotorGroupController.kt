/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import com.google.gson.GsonBuilder
import jssc.SerialPort
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*
import java.util.logging.Logger

/**
 * The MotorGroupController receives requests from the dispatcher that have to do with
 * the Dynamixel motors. This controller then dispenses the request to the multiple
 * MotorControllers receiving results via a call-back. An await-signal scheme
 * is used to present a synchronized method interface to the server.
 *
 * The same channel is used for each of the MotorControllers. On receipt of a
 * message it is the responsibility of the controller to ignore inappropriate
 * requests.
 *
 * On initialization, the system architecture is checked to determine if
 * this is being run in a development or production environment. If
 * development, then responses are simulated without any direct serial
 * requests being made.
 */

class MotorGroupController(parent:Controller,req: Channel<MessageBottle>, rsp: Channel<MessageBottle>) : Controller,MotorManager {
    @DelicateCoroutinesApi
    private val scope = GlobalScope // For long-running coroutines
    private val motorControllers: MutableList<MotorController>   // One controller per serial port
    private var parentRequestChannel  = req   // Dispatcher->MGC (serial commands)
    private var parentResponseChannel = rsp   // MGC->Dispatcher (results of serial commands)
    private val requestChannel  = Channel<MessageBottle>()   // Same channel for each controller
    private val responseChannel = Channel<MessageBottle>()
    private val motorNameById: MutableMap<Int, String>
    var running: Boolean
    private var job: Job
    override var controllerCount: Int = 0

    /**
     * Start a motor controller for each port.
     */
    @DelicateCoroutinesApi
    override suspend fun execute() {
        if (!running) {
            LOGGER.info(String.format("%s.execute: started...", CLSS))
            running = true
            LOGGER.info(String.format("%s(%s).execute: Initializing ...",CLSS, controllerName))
            // Port is open, now use it.
            job = scope.launch(Dispatchers.IO) {
                while (running) {
                    select<MessageBottle> {
                        /**
                         * On receipt of a message from the SerialPort,
                         * decypher, forward to the MotorManager.
                         */
                        responseChannel.onReceive() {
                            handleAggregatedResponse(it)
                        }
                        /*
                         * The parent request is a motor command. Convert it
                         * into a message for the SerialPort amd write.
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
        if (canHandleImmediately(request)) {
            parentResponseChannel.send(createResponseForLocalRequest(request))
        }
        else if (!RobotModel.useSerial) {
            parentResponseChannel.send(simulateResponseForRequest(request))
        }
        else {
            LOGGER.info(String.format("%s.processRequest: processing %s",
                CLSS,request.type.name))
            requestChannel.send(request)     // All motor controllers receive
        }
        return request
    }
    // =========================== Motor Manager Interface ====================================
    /**
     * When the one of the controllers has detected the response is complete, it calls
     * this method. When all controllers have responded the response will be sent off.
     * For this to work, it is important that work on the response object
     * by synchronized.
     * @param rsp the original request
     */
    override suspend fun handleAggregatedResponse(response: MessageBottle):MessageBottle {
        val count: Int = response.incrementResponderCount()
        if(DEBUG) LOGGER.info(String.format("%s.handleAggregatedResponse: received %s (%d of %d)",
            CLSS,response.type.name,count,controllerCount))
        if (count >= controllerCount) {
            if(DEBUG) LOGGER.info(String.format("%s.handleAggregatedResponse: all controllers accounted for: responding ...",
                CLSS ))
            parentResponseChannel.send(response)
        }
        return response
    }

    /**
     * This method is called by each controller as it handles a request that does
     * not generate a response. Once each controller has responded, we forward the
     * result to the dispatcher.
     * @param rsp the response
     */
    override suspend fun handleSynthesizedResponse(response: MessageBottle) :MessageBottle{
        val count: Int = response.incrementResponderCount()
        if(DEBUG)LOGGER.info(String.format("%s.handleSynthesizedResponse: received %s (%d of %d)",
            CLSS,response.type.name,count,controllerCount) )
        if (count >= controllerCount) {
            if(DEBUG) LOGGER.info(String.format("%s.handleSynthesizedResponse: all controllers accounted for: responding ...",
                CLSS) )
            parentResponseChannel.send(response)
        }
        return response
    }

    /**
     * This method is called by the controller that handled a request that pertained to it
     * alone. It has modified the request directly. Forward result to the Dispatcher.
     * @param response the message to be forwarded.
     */
    override suspend fun handleSingleControllerResponse(response: MessageBottle):MessageBottle {
        parentResponseChannel.send(response)
        return response
    }

    // =========================== Private Helper Methods =====================================
    // Queries of fixed properties of the motors are the kinds of requests that can be handled
    // immediately. Results are created from the original configuration file
    private fun canHandleImmediately(request: MessageBottle): Boolean {
        if (request.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
            request.type.equals(RequestType.LIST_MOTOR_PROPERTY) ) {
            // Certain properties are constants available from the configuration file.
            val defProp= request.jointDefinitionProperty
            if( defProp==JointDefinitionProperty.ID         ||
                defProp==JointDefinitionProperty.MOTORTYPE  ||
                defProp==JointDefinitionProperty.OFFSET     ||
                defProp==JointDefinitionProperty.ORIENTATION  ) {
                return true
            }
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
        else if (request.type.equals(RequestType.LIST_MOTOR_PROPERTIES)) {
            return true
        }
        return false
    }

    // The "local" response is simply the original request with some text
    // to return directly to the user. These jointValues are obtained from the initial configuration.
    private fun createResponseForLocalRequest(request: MessageBottle): MessageBottle {
        var text: String = ""
        if( request.type == RequestType.GET_MOTOR_PROPERTY ) {
            val joint = request.joint
            val jointName: String = Joint.toText(joint)
            val mc: MotorConfiguration? = RobotModel.motors[joint]
            if (mc != null) {
                // The request can be for either a definition or dynamic property
                if (request.jointDefinitionProperty.equals(JointDefinitionProperty.NONE)) {
                    // Dynamic property
                    val property = request.jointDynamicProperty
                    if(DEBUG) LOGGER.info(String.format("%s.createResponseForLocalRequest: %s %s in %s",
                        CLSS, request.type.name, property.name, joint.name))

                    when (property) {
                        JointDynamicProperty.MAXIMUMANGLE -> {
                            text = String.format("The maximum angle of my %s is %.0f degrees",
                                jointName, mc.maxAngle)
                        }
                        JointDynamicProperty.MINIMUMANGLE -> {
                            text = String.format("The minimum angle of my %s is %.0f degrees",
                                jointName, mc.minAngle)
                        }
                        JointDynamicProperty.POSITION -> {
                            text = String.format("The current position of my %s is %.0f degrees",
                                jointName, mc.position)
                        }
                        JointDynamicProperty.SPEED -> {
                            text = String.format("The speed of my %s is %.0f degrees per second",
                                jointName, mc.speed)
                        }
                        JointDynamicProperty.STATE -> {
                            text = String.format("The position of my %s is %s",
                                jointName, if (mc.isDirect) "direct" else "indirect")
                        }
                        JointDynamicProperty.TEMPERATURE -> {
                            text = String.format("The temperature of my %s is %.0f degrees Centigrade",
                                jointName, mc.temperature)
                        }
                        JointDynamicProperty.TORQUE -> {
                            text = String.format("The torque of my %s is %.0f newton meters",
                                jointName, mc.torque)
                        }
                        JointDynamicProperty.VOLTAGE -> {
                            text = String.format("The voltage of my %s is %.0f volts",
                                jointName, mc.voltage)
                        }
                        JointDynamicProperty.NONE -> {
                        }
                    }
                }
                else {
                    // Configuration property
                    val property = request.jointDefinitionProperty
                    if(DEBUG)LOGGER.info(String.format("%s.createResponseForLocalRequest: %s %s in %s",
                        CLSS, request.type.name, property.name, joint.name))
                    when (property) {
                        JointDefinitionProperty.ID -> {
                            val id: Int = mc.id
                            text = "The id of my $jointName is $id"
                        }
                        JointDefinitionProperty.MOTORTYPE -> {
                            var modelName = "A X 12"
                            if (mc.type.equals(DynamixelType.MX28)) modelName = "M X 28"
                            else if (mc.type.equals(DynamixelType.MX64)) {
                                modelName = "M X 64"
                            }
                            text = "My $jointName is a dynamixel $modelName"
                        }
                        JointDefinitionProperty.OFFSET -> {
                            val offset: Double = mc.offset
                            text = "The offset of my $jointName is $offset"
                        }
                        JointDefinitionProperty.ORIENTATION -> {
                            val orientation: String = if (mc.isDirect) "direct" else "indirect"
                            text = "The orientation of my $jointName is $orientation"
                        }

                        JointDefinitionProperty.NONE -> {
                            // Neither type of property is set
                            request.error = String.format("The message does not specify a parameter (%s)",
                                property.name)
                        }
                    }
                }
                request.text = text
            }
            else {
                request.error = String.format("There is no information for a joint named (%s)",jointName)
            }
        }
        // Return a JSON list of parameter names
        else if (request.type.equals(RequestType.LIST_MOTOR_PROPERTIES)) {
            if(request.jointDefinitionProperty.equals(JointDefinitionProperty.NONE)) {
                text = JointDefinitionProperty.toJSON()
            }
            else { // Dynamic properties
                text = JointDynamicProperty.toJSON()
            }
            request.text = text
            for (controller in motorControllers) {
                val list: MutableCollection<MotorConfiguration> = controller.configurations
                for (mc in list) {
                    if(DEBUG)LOGGER.info(String.format("Joint: %s (%d) %s min,max,offset = %f.0 %f.0 %f.0 %s",
                        mc.joint, mc.id, mc.type.name, mc.minAngle, mc.maxAngle,mc.offset,
                        if( mc.isDirect ) "" else "(indirect)"))
                }
            }
        }
        else if (request.type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
            // Either a definition property or dynamic property
            val property = request.jointDynamicProperty
            if(DEBUG)LOGGER.info(String.format("%s.createResponseForLocalRequest: %s %s for all motors",
                CLSS, request.type.name, property.name))
            val mcs: Map<Joint, MotorConfiguration> = RobotModel.motors
            var mcList = mutableListOf<MotorConfiguration>()
            for (joint in mcs.keys) {
                val mc: MotorConfiguration = mcs[joint]!!
                mcList.add(mc)
                when (property) {
                    JointDynamicProperty.MAXIMUMANGLE -> {
                        text = String.format("The maximum angle for %s is %.0f degrees", joint, mc.maxAngle)
                    }
                    JointDynamicProperty.MINIMUMANGLE -> {
                        text = String.format("The minimum angle for %s is %.0f degrees", joint, mc.minAngle)
                    }
                    JointDynamicProperty.POSITION -> {
                        text = String.format("The position of %s is %.0f degrees", joint, mc.position)
                    }
                    JointDynamicProperty.SPEED -> {
                        text = String.format("The speed of %s is %.0f degrees per second", joint, mc.speed)
                    }
                    JointDynamicProperty.STATE -> {
                        text = String.format("%s is %s", joint, if(mc.isDirect) "direct" else "indirect")
                    }
                    JointDynamicProperty.TEMPERATURE -> {
                        text = String.format("The temperature of %s is %.0f degrees Celsius", joint, mc.temperature)
                    }
                    JointDynamicProperty.TORQUE -> {
                        text = String.format("The torque of %s is %.0f newton meters", joint, mc.torque)
                    }
                    JointDynamicProperty.VOLTAGE -> {
                        text = String.format("The voltage of %s is %.0f volts", joint, mc.voltage)
                    }
                    // Not a dynamic property, try definitions
                    JointDynamicProperty.NONE -> {
                        val definition = request.jointDefinitionProperty
                        when (definition) {
                            JointDefinitionProperty.ID -> {
                                val id: Int = mc.id
                                text = "The id of $joint is $id"
                            }
                            JointDefinitionProperty.MOTORTYPE -> {
                                var modelName = "A X 12"
                                if (mc.type.equals(DynamixelType.MX28))      modelName = "M X 28"
                                else if (mc.type.equals(DynamixelType.MX64)) modelName = "M X 64"
                                text = joint.toString() + " is a dynamixel " + modelName
                            }
                            JointDefinitionProperty.OFFSET -> {
                                val offset: Double = mc.offset
                                text = "The offset of $joint is $offset"
                            }
                            JointDefinitionProperty.ORIENTATION -> {
                                var orientation = "indirect"
                                if( mc.isDirect ) orientation = "direct"
                                text = "The orientation of $joint is $orientation"
                            }
                            JointDefinitionProperty.NONE -> {
                                text = ""
                                request.error = definition.name + " is not a property that I can look up"
                            }
                        }
                    }
                }
                if(DEBUG) LOGGER.info(text)
            }
            val gson = GsonBuilder().setPrettyPrinting().create()
            text = gson.toJson(mcList)
            request.text = text
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


    // When in development mode, simulate something reasonable as a response.
    private fun simulateResponseForRequest(request: MessageBottle): MessageBottle {
        val requestType = request.type
        var text = ""
        if (requestType.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val joint: Joint = request.joint
            val jointName: String = Joint.toText(joint)
            val mcmaybe: MotorConfiguration? = RobotModel.motors[joint]
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
                    JointDefinitionProperty.NONE -> TODO()
                }
            }
            else {
                val property = request.jointDynamicProperty
                when (property) {
                    JointDynamicProperty.POSITION -> {
                        val position = 0
                        text = "The position of my $jointName is $position"
                    }
                    JointDynamicProperty.MAXIMUMANGLE -> TODO()
                    JointDynamicProperty.MINIMUMANGLE -> TODO()
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
    /**
     * Create the "serial" controllers that handle Dynamixel motors. We launch multiple
     * instances each running in its own thread. Each controller handles a group of
     * motors all communicating on the same serial port.
     */
    init  {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_GROUP)
        motorControllers = mutableListOf<MotorController>()
        motorNameById    = mutableMapOf<Int, String>()
        running = false
        LOGGER.info(String.format("%s: os.arch = %s", CLSS, System.getProperty("os.arch"))) // x86_64
        LOGGER.info(String.format("%s: os.name = %s", CLSS, System.getProperty("os.name"))) // Mac OS X
        if (RobotModel.useSerial) {
            val motors: Map<Joint, MotorConfiguration> = RobotModel.motors
            for(cname in RobotModel.motorControllerNames) {
                val portName : String = RobotModel.getPortForMotorController(cname)
                if( portName==ConfigurationConstants.NO_PORT ) continue   // Controller is not a motor controller

                val port:SerialPort = SerialPort(portName)
                val controller = MotorController(port,this,requestChannel,responseChannel)
                motorControllers.add(controller)

                // Add configurations to the controller for each motor in the group
                val joints: List<Joint> = RobotModel.getJointsForMotorController(cname)
                LOGGER.info(String.format("%s.initialize: getting joints for %s", CLSS, cname))
                LOGGER.info(String.format("%s.initialize: %d joints for %s", CLSS, joints.size, cname))
                for (joint in joints) {
                    val motor: MotorConfiguration? = motors[joint]
                    if (motor != null) {
                        //LOGGER.info(String.format("%s.initialize: Added motor %s to group %s",CLSS,joint.name(),controller.getGroupName()));
                        controller.putMotorConfiguration(joint, motor)
                        motorNameById[motor.id] = joint.name
                    }
                    else {
                        LOGGER.warning(String.format("%s.initialize: Motor %s not found in %s",
                            CLSS,joint.name, cname))
                    }
                }

                if(DEBUG) LOGGER.info(String.format("%s.initialize: Created motor controller %s",
                    CLSS,controller.controllerName))
            }
            controllerCount = motorControllers.size
        }
        else {
            controllerCount = 0
        }
        job=Job()
    }
}