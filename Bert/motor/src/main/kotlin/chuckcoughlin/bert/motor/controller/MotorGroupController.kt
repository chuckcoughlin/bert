/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageHandler
import chuckcoughlin.bert.common.message.PropertyType
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.DynamixelType
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDefinitionProperty
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.JointProperty
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.common.model.RobotMotorModel
import jssc.SerialPort
import java.util.*
import java.util.logging.Logger
import kotlin.collections.HashMap

/**
 * The MotorGroupController receives requests from the server having to do with
 * the Dynamixel motors. This controller dispenses the request to the multiple
 * MotorControllers receiving results via a call-back. An await-signal scheme
 * is used to present a synchronized method interface to the server.
 *
 * On initialization, the system architecture is checked to determine if
 * this is being run in a development or production environment. If
 * development, then responses are simulated without any direct serial
 * requests being made.
 */
class MotorGroupController(m: RobotMotorModel) : Controller,MotorManager {
    private val model: RobotMotorModel = m
    private val motorControllers: MutableMap<String, MotorController>
    private val motorNameById: MutableMap<Int, String>
    private val motorControllerThreads: MutableMap<String, Thread>
    var development: Boolean = false
    override var controllerCount:Int
    var lazy responseHandler: MessageHandler // Dispatcher - delayed initialization

    /**
     * Create the "serial" controllers that handle Dynamixel motors. We launch multiple
     * instances each running in its own thread. Each controller handles a group of
     * motors all communicating on the same serial port.
     */
    fun initialize() {
        if (!development) {
            val controllerNames: Set<String> = model.handlerTypes.keys
            val motors: Map<Joint, MotorConfiguration> = model.motors
            for (cname in controllerNames) {
                val port: SerialPort = model.getPortForController(cname) ?: continue
                // Controller is not a motor controller
                val controller = MotorController(cname, port, this)
                val t = Thread(controller)
                motorControllers[cname] = controller
                motorControllerThreads[cname] = t

                // Add configurations to the controller for each motor in the group
                val joints: List<Joint> = model.getJointsForController(cname)
                LOGGER.info(String.format("%s.initialize: getting joints for %s", CLSS, cname))
                LOGGER.info(String.format("%s.initialize: %d joints for %s", CLSS, joints.size, cname))
                for (joint in joints) {
                    val motor: MotorConfiguration? = motors[joint]
                    if (motor != null) {
                        //LOGGER.info(String.format("%s.initialize: Added motor %s to group %s",CLSS,joint.name(),controller.getGroupName()));
                        controller.putMotorConfiguration(joint.name, motor)
                        motorNameById[motor.id] = joint.name
                    }
                    else {
                        LOGGER.warning(String.format("%s.initialize: Motor %s not found in %s",
                                CLSS,joint.name, cname))
                    }
                }
                controllerCount += 1
                LOGGER.info(String.format("%s.initialize: Created motor controller %s",
                        CLSS,controller.controllerName
                    )
                )
            }
        }
    }

    override fun getControllerCount(): Int {
        return motorControllers.size
    }

    fun setResponseHandler(mh: MessageHandler) {
        responseHandler = mh
    }

    fun start() {
        if (!development) {
            for (key in motorControllers.keys) {
                val controller = motorControllers[key]
                controller!!.setStopped(false)
                controller.initialize()
                motorControllerThreads[key]!!.start()
            }
        }
    }

    /**
     * Called by Dispatcher when it is stopped.
     */
    fun stop() {
        if (!development) {
            for (key in motorControllers.keys) {
                val controller = motorControllers[key]
                controller!!.setStopped(true)
                controller.stop()
                motorControllerThreads[key]!!.interrupt()
            }
        }
    }

    /**
     * Called by the Dispatcher when confronted with Motor requests.
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
    fun processRequest(request: MessageBottle) {
        if (canHandleImmediately(request)) {
            responseHandler.handleResponse(createResponseForLocalRequest(request))
        } else if (development) {
            responseHandler.handleResponse(simulateResponseForRequest(request))
        } else {
            LOGGER.info(String.format(
                    "%s.processRequest: processing %s",
                    CLSS,
                    request.type.name()
                )
            )
            for (controller in motorControllers.values) {
                controller.receiveRequest(request)
            }
        }
    }
    // =========================== Motor Manager Interface ====================================
    /**
     * When the one of the controllers has detected the response is complete, it calls
     * this method. When all controllers have responded the response will be sent off.
     * For this to work, it is important that work on the response object
     * by synchronized.
     * @param rsp the original request
     */
    override fun handleAggregatedResponse(rsp: MessageBottle) {
        val count: Int = rsp.incrementResponderCount()
        LOGGER.info(String.format("%s.handleAggregatedResponse: received %s (%d of %d)",
                CLSS,rsp.type.name,count,controllerCount))
        if (count >= controllerCount) {
            LOGGER.info(String.format("%s.handleAggregatedResponse: all controllers accounted for: responding ...",
                    CLSS ))
            responseHandler.handleResponse(rsp)
        }
    }

    /**
     * This method is called by each controller as it handles a request that does
     * not generate a response. Once each controller has responded, we forward the
     * result to the dispatcher.
     * @param rsp the response
     */
    @Synchronized
    override fun handleSynthesizedResponse(rsp: MessageBottle) {
        val count: Int = rsp.incrementResponderCount()
        LOGGER.info(String.format("%s.handleSynthesizedResponse: received %s (%d of %d)",
                CLSS,rsp.type.name,count,controllerCount)
        )
        if (count >= controllerCount) {
            LOGGER.info(String.format("%s.handleSynthesizedResponse: all controllers accounted for: responding ...",
                    CLSS) )
            responseHandler.handleResponse(rsp)
        }
    }

    /**
     * This method is called by the controller that handled a request that pertained to it
     * alone. It has modified the request directly. Forward result to the Dispatcher.
     * @param response the message to be forwarded.
     */
    override fun handleSingleControllerResponse(response: MessageBottle) {
        responseHandler.handleResponse(response)
    }

    // =========================== Private Helper Methods =====================================
    // Queries of fixed properties of the motors are the kinds of requests that can be handled
    // immediately. Results are created from the original configuration file
    private fun canHandleImmediately(request: MessageBottle): Boolean {
        if (request.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
            request.type.equals(RequestType.LIST_MOTOR_PROPERTY)
        ) {
            // Certain properties are constants available from the configuration file.
            val property: String = request.getProperty(PropertyType.PROPERTY_NAME, "")
            if (property.equals(JointDefinitionProperty.ID.name, ignoreCase = true) ||
                property.equals(JointDynamicProperty.MINIMUMANGLE.name, ignoreCase = true) ||
                property.equals(JointDynamicProperty.MAXIMUMANGLE.name, ignoreCase = true) ||
                property.equals(JointDefinitionProperty.MOTORTYPE.name, ignoreCase = true) ||
                property.equals(JointDefinitionProperty.OFFSET.name, ignoreCase = true) ||
                property.equals(JointDefinitionProperty.ORIENTATION.name, ignoreCase = true)
            ) {
                return true
            }
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY)) {
            // Some properties cannot be set. Catch them here in order to formulate an error response.
            val property = request.property
            if (property.equals(JointDefinitionProperty.ID) ||
                property.equals(JointDefinitionProperty.MOTORTYPE) ||
                property.equals(JointDefinitionProperty.OFFSET) ||
                property.equals(JointDefinitionProperty.ORIENTATION) ||
                property.equals(JointDynamicProperty.POSITION)
            ) {
                return true
            }
        }
        else if (request.type.equals(RequestType.SET_MOTOR_PROPERTY)) {
            // Some properties cannot be set. Catch them here in order to formulate an error response.
            val property = request.property
            if (property.equals(JointDefinitionProperty.ID.) ||
                property.equals(JointDefinitionProperty.MOTORTYPE) ||
                property.equals(JointDefinitionProperty.OFFSET) ||
                property.equals(JointDefinitionProperty.ORIENTATION)
            ) {
                return true
            }
        }
        else if (request.type.equals(RequestType.GET_CONFIGURATION)) {
            return true
        }
        return false
    }

    // The "local" response is simply the original request with some text
    // to return directly to the user. These jointValues are obtained from the initial configuration.
    private fun createResponseForLocalRequest(request: MessageBottle): MessageBottle {
        if (request.type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val property = request.property
            val joint: Joint = Joint.valueOf(request.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN"))
            LOGGER.info(
                java.lang.String.format(
                    "%s.createResponseForLocalRequest: %s %s in %s",
                    CLSS,request.type.name,property.name,joint.name))
            var text: String? = ""
            val jointName: String = Joint.toText(joint)
            val mc: MotorConfiguration = model.getMotors().get(joint)
            if (mc != null) {
                when (property) {
                    ID -> {
                        val id: Int = mc.getId()
                        text = "The id of my $jointName is $id"
                    }

                    JointProperty.MAXIMUMANGLE -> text = java.lang.String.format(
                        "The maximum angle of my %s is %.0f degrees",
                        jointName,
                        mc.getMaxAngle()
                    )

                    JointProperty.MINIMUMANGLE -> text = java.lang.String.format(
                        "The minimum angle of my %s is %.0f degrees",
                        jointName,
                        mc.getMinAngle()
                    )

                    JointProperty.MOTORTYPE -> {
                        var modelName = "A X 12"
                        if (mc.getType().equals(DynamixelType.MX28)) modelName = "M X 28" else if (mc.getType()
                                .equals(DynamixelType.MX64)
                        ) modelName = "M X 64"
                        text = "My $jointName is a dynamixel $modelName"
                    }

                    JointProperty.OFFSET -> {
                        val offset: Double = mc.getOffset()
                        text = "The offset of my $jointName is $offset"
                    }

                    JointProperty.ORIENTATION -> {
                        var orientation = "indirect"
                        if (mc.isDirect()) orientation = "direct"
                        text = "The orientation of my $jointName is $orientation"
                    }

                    else -> {
                        text = ""
                        request.assignError(property.name() + " is not a property that I can look up")
                    }
                }
            }
            else {
                request.assignError(String.format("The configuration file does not include joint %s",
                        joint.name ) )
            }
            request.text = text
        }
        else if (request.type.equals(RequestType.GET_CONFIGURATION)) {
            val text = "Motor configuration parameters have been logged"
            request.text = text
            for (group in motorControllers.keys) {
                val controller = motorControllers[group]
                val map: Map<String?, MotorConfiguration?>? = controller.getConfigurations()
                for (joint in map!!.keys) {
                    val mc: MotorConfiguration? = map[joint]
                    LOGGER.info(String.format("Joint: %s (%d) %s min,max,offset = %f.0 %f.0 %f.0 %s",
                            joint,mc.id,mc.type.name,mc.minAngle,mc.maxAngle,
                            mc.offset,
                            if (mc.isDirect()) "" else "(indirect)"
                        )
                    )
                    request.setProperty(PropertyType.PROPERTY_NAME, JointProperty.MOTORTYPE.name())
                    request.setJointValue(JointProperty.MOTORTYPE.name(), mc.getType().name())
                }
            }
        }
        else if (request.type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
            val property = request.property
            LOGGER.info(String.format("%s.createResponseForLocalRequest: %s %s for all motors",
                    CLSS,request.type.name,property.name()))
            var text: String? = ""
            val mcs: Map<Joint, MotorConfiguration> = model.motors
            for (joint in mcs.keys) {
                val mc: MotorConfiguration = mcs[joint]!!
                when (property) {
                    ID -> {
                        val id: Int = mc.id
                        text = "The id of $joint is $id"
                    }

                    JointProperty.MAXIMUMANGLE -> text =
                        java.lang.String.format("The maximum angle for %s is %.0f degrees", joint, mc.getMaxAngle())

                    JointProperty.MINIMUMANGLE -> text =
                        java.lang.String.format("The minimum angle for %s is %.0f degrees", joint, mc.getMinAngle())

                    JointProperty.MOTORTYPE -> {
                        var modelName = "A X 12"
                        if (mc.getType().equals(DynamixelType.MX28)) modelName = "M X 28" else if (mc.getType()
                                .equals(DynamixelType.MX64)
                        ) modelName = "M X 64"
                        text = joint.toString() + " is a dynamixel " + modelName
                    }

                    JointProperty.OFFSET -> {
                        val offset: Double = mc.getOffset()
                        text = "The offset of $joint is $offset"
                    }

                    JointProperty.ORIENTATION -> {
                        var orientation = "indirect"
                        if (mc.isDirect()) orientation = "direct"
                        text = "The orientation of $joint is $orientation"
                    }

                    else -> {
                        text = ""
                        request.error = property.name() + " is not a property that I can look up"
                    }
                }
                LOGGER.info(text)
            }
            text = String.format("The %ss of all motors have been logged", property.name().toLowerCase())
            request.text =  text
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY)) {
            val property: String = request.getProperty(PropertyType.PROPERTY_NAME, "")
            request.error = "I cannot change " + property.lowercase(Locale.getDefault()) + " for all joints in the limb")
        }
        else if (request.type.equals(RequestType.SET_MOTOR_PROPERTY)) {
            val property: String = request.getProperty(PropertyType.PROPERTY_NAME, "")
            request.error = "I cannot change a motor " + property.lowercase(Locale.getDefault())
        }
        return request
    }

    // When in development mode, simulate something reasonable as a response.
    private fun simulateResponseForRequest(request: MessageBottle): MessageBottle {
        val requestType = request.type
        if (requestType.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val property: JointProperty = JointProperty.valueOf(request.getProperty(BottleConstants.PROPERTY_NAME, ""))
            val joint: Joint = Joint.valueOf(request.getProperty(BottleConstants.JOINT_NAME, "UNKNOWN"))
            var text = ""
            val jointName: String = Joint.toText(joint)
            val mc: MotorConfiguration = model.motors.get(joint)
            when (property) {
                JointProperty.POSITION -> {
                    val position = 0
                    text = "The position of my $jointName is $position"
                }

                else -> {
                    text = ""
                    request.error = property.name() + " is not a property that I can read")
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
    private val LOGGER = Logger.getLogger(CLSS)

    /**
     * Constructor:
     * @param m the server model
     */
    init {
        motorControllers = HashMap()
        motorControllerThreads = HashMap()
        motorNameById = HashMap()
        LOGGER.info(String.format("%s: os.arch = %s", CLSS, System.getProperty("os.arch"))) // x86_64
        LOGGER.info(String.format("%s: os.name = %s", CLSS, System.getProperty("os.name"))) // Mac OS X
        development = System.getProperty("os.arch").startsWith("x86")
    }
}