/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointProperty
import chuckcoughlin.bert.common.model.Limb
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.motor.dynamixel.DxlMessage
import jssc.SerialPort
import jssc.SerialPortEvent
import jssc.SerialPortEventListener
import jssc.SerialPortException
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Handle requests directed to a specific controllerName of motors. All motors under the
 * same controller are connected to the same serial port. We respond to the group controller
 * using call-backs. The responses from the serial port do not necessarily keep
 * to request boundaries. All we are sure of is that the requests are processed
 * in order.
 *
 * The configuration array has only those joints that are part of the controllerName.
 * It is important that the MotorConfiguration objects are the same objects
 * (not clones) as those held by the MotorManager (MotorGroupController).
 */
class MotorController(name: String, p: SerialPort, mm: MotorManager) : Runnable, SerialPortEventListener {
    private val running: Condition
    private val dxl: DxlMessage
    val controllerName // Group name
            : String
    private val lock: Lock
    private val port: SerialPort
    private var stopped = false
    private val motorManager: MotorManager
    private val configurationsById: MutableMap<Int, MotorConfiguration>
    private val configurationsByName: MutableMap<String, MotorConfiguration>
    private var remainder: ByteArray? = null
    private val requestQueue // requests waiting to be processed
            : LinkedList<MessageBottle>
    private val responseQueue // responses waiting for serial results
            : LinkedList<MessageWrapper>
    private var timeOfLastWrite: Long

    init {
        dxl = DxlMessage()
        controllerName = name
        port = p
        motorManager = mm
        configurationsById = HashMap<Int, MotorConfiguration>()
        configurationsByName = HashMap<String, MotorConfiguration>()
        requestQueue = LinkedList<MessageBottle>()
        responseQueue = LinkedList<MessageWrapper>()
        lock = ReentrantLock()
        running = lock.newCondition()
        timeOfLastWrite = System.nanoTime() / 1000000
    }

    val configurations: Map<String, Any>
        get() = configurationsByName

    fun getMotorConfiguration(name: String): MotorConfiguration? {
        return configurationsByName[name]
    }

    fun putMotorConfiguration(name: String, mc: MotorConfiguration) {
        configurationsById[mc.id] = mc
        configurationsByName[name] = mc
    }

    /**
     * Open and configure the port.
     * Dynamixel documentation: No parity, 1 stop bit, 8 bits of data, no flow control
     *
     * At one point, we thought we should initialize the motors somehow.  This is now
     * taken care of by the dispatcher. The dispatcher:
     * 1) requests a list of current positions (thus updating the MotorConfigurations)
     * 2) sets travel speeds to "normal"
     * 3) moves any limbs that are "out-of-bounds" back into range.
     */
    fun initialize() {
        LOGGER.info(
            java.lang.String.format(
                "%s(%s).initialize: Initializing port %s)",
                CLSS,
                controllerName,
                port.getPortName()
            )
        )
        if (!port.isOpened()) {
            try {
                val success: Boolean = port.openPort()
                if (success && port.isOpened()) {
                    port.setParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE)
                    port.setEventsMask(SerialPort.MASK_RXCHAR)
                    port.purgePort(SerialPort.PURGE_RXCLEAR)
                    port.purgePort(SerialPort.PURGE_TXCLEAR)
                    port.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN or SerialPort.FLOWCONTROL_RTSCTS_OUT)
                    port.addEventListener(this)
                }
                else {
                    LOGGER.severe(
                        java.lang.String.format(
                            "%s.initialize: Failed to open port %s for %s",
                            CLSS,
                            port.getPortName(),
                            controllerName
                        )
                    )
                }
            } catch (spe: SerialPortException) {
                LOGGER.severe(
                    java.lang.String.format(
                        "%s.initialize: Error opening port %s for %s (%s)",
                        CLSS,
                        port.getPortName(),
                        controllerName,
                        spe.getLocalizedMessage()
                    )
                )
            }
            LOGGER.info(java.lang.String.format("%s.initialize: Initialized port %s)", CLSS, port.getPortName()))
        }
    }

    fun stop() {
        try {
            port.closePort()
        } catch (spe: SerialPortException) {
            LOGGER.severe(
                java.lang.String.format(
                    "%s.close: Error closing port for %s (%s)",
                    CLSS,
                    controllerName,
                    spe.getLocalizedMessage()
                )
            )
        }
        stopped = true
    }

    fun setStopped(flag: Boolean) {
        stopped = flag
    }

    /**
     * This method blocks until the prior request completes. Ignore requests that apply to a single controller
     * and that controller is not this one, otherwise add the request to the request queue.
     * @param request
     */
    fun receiveRequest(request: MessageBottle) {
        lock.lock()
        //LOGGER.info(String.format("%s(%s).receiveRequest: processing %s",CLSS,controllerName,request.fetchRequestType().name()));
        try {
            if (isLocalRequest(request)) {
                handleLocalRequest(request)
                return
            } else if (isSingleControllerRequest(request)) {
                // Do nothing if the joint or limb isn't in our controllerName.
                val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name())
                val cName: String = request.getProperty(BottleConstants.CONTROLLER_NAME, "")
                val limbName: String = request.getProperty(BottleConstants.LIMB_NAME, Limb.UNKNOWN.name())
                if (!jointName.equals(Joint.UNKNOWN.name(), ignoreCase = true)) {
                    val mc: MotorConfiguration = configurationsByName[jointName] ?: return
                }
                else if (!cName.isEmpty()) {
                    if (!cName.equals(controllerName, ignoreCase = true)) {
                        return
                    }
                }
                else if (!limbName.equals(Limb.UNKNOWN.name(), ignoreCase = true)) {
                    val limb: Limb = Limb.valueOf(limbName)
                    val count = configurationsForLimb(limb).size
                    if (count == 0) {
                        return
                    }
                }
                else {
                    val propertyName: String =
                        request.getProperty(BottleConstants.PROPERTY_NAME, JointProperty.UNRECOGNIZED.name())
                    LOGGER.info(
                        java.lang.String.format(
                            "%s(%s).receiveRequest: %s (%s)",
                            CLSS,
                            controllerName,
                            request.fetchRequestType().name(),
                            propertyName
                        )
                    )
                }
            }
            else {
                LOGGER.info(
                    java.lang.String.format(
                        "%s(%s).receiveRequest: multi-controller request (%s)",
                        CLSS,
                        controllerName,
                        request.fetchRequestType().name()
                    )
                )
            }
            requestQueue.addLast(request)
            // LOGGER.info(String.format("%s(%s).receiveRequest: added to request queue %s",CLSS,controllerName,request.fetchRequestType().name()));
            running.signal()
        }
        finally {
            lock.unlock()
        }
    }

    /**
     * Wait until we receive a request message. Convert to serial request, write to port.
     * From there a listener forwards the responses to the controllerName controller (MotorManager).
     * Do not free the request lock until we have the response in-hand.
     *
     * Integer.toHexString(this.hashCode())
     */
    override fun run() {
        while (!stopped) {
            lock.lock()
            try {
                running.await()
                // LOGGER.info(String.format("%s.run: %s Got signal for message, writing to %s",CLSS,controllerName,port.getPortName()));
                val req: MessageBottle = requestQueue.removeFirst() // Oldest
                val wrapper = MessageWrapper(req)
                if (isSingleWriteRequest(req)) {
                    val bytes = messageToBytes(wrapper)
                    if (bytes != null) {
                        if (wrapper.responseCount > 0) {
                            responseQueue.addLast(wrapper)
                        }
                        writeBytesToSerial(bytes)
                        LOGGER.info(String.format("%s(%s).run: wrote %d bytes", CLSS, controllerName, bytes.size))
                    }
                }
                else {
                    val byteArrayList = messageToByteList(wrapper)
                    if (wrapper.responseCount > 0) {
                        responseQueue.addLast(wrapper)
                    }
                    for (bytes in byteArrayList) {
                        writeBytesToSerial(bytes)
                        LOGGER.info(String.format("%s(%s).run: wrote %d bytes", CLSS, controllerName, bytes.size))
                    }
                }
                if (wrapper.responseCount == 0) {
                    synthesizeResponse(req)
                }
            } catch (ie: InterruptedException) {
            } finally {
                lock.unlock()
            }
        }
    }

    // ============================= Private Helper Methods =============================
    // Create a response for a request that can be handled immediately. There aren't many of them. The response is simply the original request
    // with some text to send directly to the user. 
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        // The following two requests simply use the current positions of the motors, whatever they are
        if (request.fetchRequestType().equals(RequestType.COMMAND)) {
            val command: String = request.getProperty(BottleConstants.COMMAND_NAME, "NONE")
            LOGGER.warning(
                String.format(
                    "%s(%s).createResponseForLocalRequest: command=%s",
                    CLSS,
                    controllerName,
                    command
                )
            )
            if (command.equals(BottleConstants.COMMAND_RESET, ignoreCase = true)) {
                remainder = null // Resync after dropped messages.
                responseQueue.clear()
                motorManager.handleAggregatedResponse(request)
            } else {
                val msg = String.format("Unrecognized command: %s", command)
                request.assignError(msg)
            }
        }
        return request
    }

    /**
     * @param msg the request
     * @return true if this is the type of request that can be satisfied locally.
     */
    private fun isLocalRequest(msg: MessageBottle): Boolean {
        return if (msg.fetchRequestType().equals(RequestType.COMMAND) &&
            msg.getProperty(BottleConstants.COMMAND_NAME, "NONE").equalsIgnoreCase(BottleConstants.COMMAND_RESET)
        ) {
            true
        } else false
    }

    /**
     * @param msg the request
     * @return true if this is the type of request satisfied by a single controller.
     */
    private fun isSingleControllerRequest(msg: MessageBottle?): Boolean {
        if (msg.fetchRequestType().equals(RequestType.GET_GOALS) ||
            msg.fetchRequestType().equals(RequestType.GET_LIMITS) ||
            msg.fetchRequestType().equals(RequestType.GET_MOTOR_PROPERTY) ||
            msg.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY) ||
            msg.fetchRequestType().equals(RequestType.SET_MOTOR_PROPERTY)
        ) {
            return true
        } else if (msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY) &&
            (!msg.getProperty(BottleConstants.CONTROLLER_NAME, "").equals("") ||
                    !msg.getProperty(BottleConstants.LIMB_NAME, Limb.UNKNOWN.name())
                        .equalsIgnoreCase(Limb.UNKNOWN.name()))
        ) {
            return true
        }
        return false
    }

    /**
     * The list here should match the request types in messageToByteList().
     * @param msg the request
     * @return true if this request translates into a single serial message.
     * false implies that an array of serial messages are required.
     */
    private fun isSingleWriteRequest(msg: MessageBottle): Boolean {
        return if (msg.fetchRequestType().equals(RequestType.INITIALIZE_JOINTS) ||
            msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY) ||
            msg.fetchRequestType().equals(RequestType.SET_POSE)
        ) {
            false
        } else true
    }

    /**
     * @param msg the request
     * @return true if this is the type of message that returns a separate
     * status response for every motor referenced in the request.
     * (There may be only one).
     */
    private fun returnsStatusArray(msg: MessageBottle?): Boolean {
        return if (msg.fetchRequestType().equals(RequestType.GET_MOTOR_PROPERTY) ||
            msg.fetchRequestType().equals(RequestType.LIST_MOTOR_PROPERTY)
        ) {
            true
        } else false
    }

    /**
     * Convert the request message into a command for the serial port. As a side
     * effect set the number of expected responses. This can vary by request type.
     * @param wrapper
     * @return
     */
    private fun messageToBytes(wrapper: MessageWrapper): ByteArray? {
        val request: MessageBottle? = wrapper.message
        var bytes: ByteArray? = null
        if (request != null) {
            val type: RequestType = request.fetchRequestType()
            if (type.equals(RequestType.COMMAND) &&
                request.getProperty(BottleConstants.COMMAND_NAME, "").equalsIgnoreCase(BottleConstants.COMMAND_FREEZE)
            ) {
                val propertyName: String = JointProperty.STATE.name()
                for (mc in configurationsByName.values) {
                    mc.setTorqueEnabled(true)
                }
                bytes = dxl.byteArrayToSetProperty(configurationsByName, propertyName)
                wrapper.responseCount = 0 // No response
            } else if (type.equals(RequestType.COMMAND) &&
                request.getProperty(BottleConstants.COMMAND_NAME, "").equalsIgnoreCase(BottleConstants.COMMAND_RELAX)
            ) {
                for (mc in configurationsByName.values) {
                    mc.setTorqueEnabled(false)
                }
                val propertyName: String = JointProperty.STATE.name()
                bytes = dxl.byteArrayToSetProperty(configurationsByName, propertyName)
                wrapper.responseCount = 0 // No response
            } else if (type.equals(RequestType.GET_GOALS)) {
                val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, "")
                val mc: MotorConfiguration? = configurationsByName[jointName]
                if (mc != null) {
                    bytes = dxl.bytesToGetGoals(mc.getId())
                    wrapper.responseCount = 1 // Status message
                }
            } else if (type.equals(RequestType.GET_LIMITS)) {
                val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, "")
                val mc: MotorConfiguration? = configurationsByName[jointName]
                if (mc != null) {
                    bytes = dxl.bytesToGetLimits(mc.getId())
                    wrapper.responseCount = 1 // Status message
                }
            } else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
                val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, "")
                val mc: MotorConfiguration? = configurationsByName[jointName]
                val propertyName: String = request.getProperty(BottleConstants.PROPERTY_NAME, "")
                if (mc != null) {
                    bytes = dxl.bytesToGetProperty(mc.getId(), propertyName)
                    wrapper.responseCount = 1 // Status message
                }
            } else if (type.equals(RequestType.SET_LIMB_PROPERTY)) {
                val limbName: String = request.getProperty(BottleConstants.LIMB_NAME, Limb.UNKNOWN.name())
                val propertyName: String =
                    request.getProperty(BottleConstants.PROPERTY_NAME, JointProperty.UNRECOGNIZED.name())
                val jp: JointProperty = JointProperty.valueOf(propertyName)
                val value: Double = request.getProperty(propertyName.uppercase(Locale.getDefault()), "0.0").toDouble()
                // Loop over motor config map, set the property
                val limb: Limb = Limb.valueOf(limbName)
                val configs: Map<String, MotorConfiguration> = configurationsForLimb(limb)
                for (mc in configs.values) {
                    mc.setProperty(jp, value)
                }
                bytes = dxl.byteArrayToSetProperty(configs, propertyName) // Returns null if limb not on this controller
                wrapper.responseCount = 0 // ASYNC WRITE, no response. Let source set text.
            } else if (type.equals(RequestType.SET_MOTOR_PROPERTY)) {
                val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name())
                val mc: MotorConfiguration? = configurationsByName[jointName]
                val propertyName: String = request.getProperty(BottleConstants.PROPERTY_NAME, "")
                val value: String = request.getProperty(propertyName.uppercase(Locale.getDefault()), "0.0")
                if (value != null && !value.isEmpty() && mc != null) {
                    bytes = dxl.bytesToSetProperty(mc, propertyName, value.toDouble())
                    if (propertyName.equals("POSITION", ignoreCase = true)) {
                        val duration: Long = mc.getTravelTime()
                        if (request.getDuration() < duration) request.setDuration(duration)
                        request.assignText(java.lang.String.format("My position is %.0f", mc.getPosition()))
                    } else if (propertyName.equals("STATE", ignoreCase = true)) {
                        request.assignText(
                            java.lang.String.format(
                                "My %s state is torque-%s", Joint.toText(mc.getJoint()),
                                if (value.equals("0", ignoreCase = true)) "disabled" else "enabled"
                            )
                        )
                    } else {
                        request.assignText(
                            java.lang.String.format(
                                "My %s %s is %s",
                                Joint.toText(mc.getJoint()),
                                propertyName.lowercase(Locale.getDefault()),
                                value
                            )
                        )
                    }
                    wrapper.responseCount = 1 // Status message
                } else {
                    LOGGER.warning(
                        java.lang.String.format(
                            "%s.messageToBytes: Empty property value - ignored (%s)",
                            CLSS,
                            type.name()
                        )
                    )
                    wrapper.responseCount = 0 // Error, there will be no response
                }
            } else if (type.equals(RequestType.NONE)) {
                LOGGER.warning(
                    java.lang.String.format(
                        "%s.messageToBytes: Empty request - ignored (%s)",
                        CLSS,
                        type.name()
                    )
                )
                wrapper.responseCount = 0 // Error, there will be no response
            } else {
                LOGGER.severe(
                    java.lang.String.format(
                        "%s.messageToBytes: Unhandled request type %s",
                        CLSS,
                        type.name()
                    )
                )
                wrapper.responseCount = 0 // Error, there will be no response
            }
            LOGGER.info(
                java.lang.String.format(
                    "%s.messageToBytes: %s = \n%s",
                    CLSS,
                    request.fetchRequestType(),
                    dxl.dump(bytes)
                )
            )
        }
        return bytes
    }

    /**
     * Convert the request message into a list of commands for the serial port. As a side
     * effect set the number of expected responses. This can vary by request type (and may
     * be none).
     * @param wrapper
     * @return
     */
    private fun messageToByteList(wrapper: MessageWrapper): List<ByteArray> {
        val request: MessageBottle? = wrapper.message
        var list: List<ByteArray> = ArrayList()
        if (request != null) {
            val type: RequestType = request.fetchRequestType()
            // Unfortunately broadcast requests don't work here. We have to concatenate the
            // requests into single long lists.
            if (type.equals(RequestType.INITIALIZE_JOINTS)) {
                list = dxl.byteArrayListToInitializePositions(configurationsByName)
                val duration: Long = dxl.getMostRecentTravelTime()
                if (request.getDuration() < duration) request.setDuration(duration)
                wrapper.responseCount = 0 // No response
            } else if (type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
                val limbName: String = request.getProperty(BottleConstants.LIMB_NAME, Limb.UNKNOWN.name())
                val propertyName: String = request.getProperty(BottleConstants.PROPERTY_NAME, "")
                if (limbName.equals(Limb.UNKNOWN.name(), ignoreCase = true)) {
                    list = dxl.byteArrayListToListProperty(propertyName, configurationsByName.values)
                    wrapper.responseCount = configurationsByName.size // Status packet for each motor
                } else {
                    val limb: Limb = Limb.valueOf(limbName)
                    val configs: Map<String, MotorConfiguration> = configurationsForLimb(limb)
                    list = dxl.byteArrayListToListProperty(propertyName, configs.values)
                    wrapper.responseCount = configs.size // Status packet for each motor in limb
                }
            } else if (type.equals(RequestType.SET_POSE)) {
                val poseName: String = request.getProperty(BottleConstants.POSE_NAME, "")
                list = dxl.byteArrayListToSetPose(configurationsByName, poseName)
                val duration: Long = dxl.getMostRecentTravelTime()
                if (request.getDuration() < duration) request.setDuration(duration)
                wrapper.responseCount = 0 // AYNC WRITE, no responses
            } else {
                LOGGER.severe(
                    java.lang.String.format(
                        "%s.messageToByteList: Unhandled request type %s",
                        CLSS,
                        type.name()
                    )
                )
            }
            for (bytes in list) {
                LOGGER.info(
                    java.lang.String.format(
                        "%s(%s).messageToByteList: %s = \n%s",
                        CLSS,
                        controllerName,
                        request.fetchRequestType(),
                        dxl.dump(bytes)
                    )
                )
            }
        }
        return list
    }

    /**
     * We have just written a message to the serial port that generates no
     * response. Make one up and send it off to the "MotorManager". It expects
     * a response from each controller.
     * @param msg the request
     */
    private fun synthesizeResponse(msg: MessageBottle) {
        if (msg.fetchRequestType().equals(RequestType.INITIALIZE_JOINTS) ||
            msg.fetchRequestType().equals(RequestType.SET_POSE)
        ) {
            motorManager.handleSynthesizedResponse(msg)
        } else if (msg.fetchRequestType().equals(RequestType.COMMAND)) {
            val cmd: String = msg.getProperty(BottleConstants.COMMAND_NAME, "")
            if (cmd.equals(BottleConstants.COMMAND_FREEZE, ignoreCase = true) ||
                cmd.equals(BottleConstants.COMMAND_RELAX, ignoreCase = true)
            ) {
                motorManager.handleSynthesizedResponse(msg)
            } else {
                LOGGER.severe(String.format("%s.synthesizeResponse: Unhandled response for command %s", CLSS, cmd))
                motorManager.handleSingleControllerResponse(msg) // Probably an error
            }
        } else if (msg.fetchRequestType().equals(RequestType.SET_LIMB_PROPERTY)) {
            motorManager.handleSingleControllerResponse(msg)
        } else {
            LOGGER.severe(
                java.lang.String.format(
                    "%s.synthesizeResponse: Unhandled response for %s",
                    CLSS,
                    msg.fetchRequestType().name()
                )
            )
            motorManager.handleSingleControllerResponse(msg) // Probably an error
        }
    }

    // We update the properties in the request from our serial message.
    // The properties must include motor type and orientation
    private fun updateRequestFromBytes(request: MessageBottle?, bytes: ByteArray?) {
        if (request != null) {
            val type: RequestType = request.fetchRequestType()
            val properties: MutableMap<String, String> = request.getProperties()
            if (type.equals(RequestType.GET_GOALS)) {
                val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name())
                val mc: MotorConfiguration? = getMotorConfiguration(jointName)
                dxl.updateGoalsFromBytes(mc, properties, bytes)
            } else if (type.equals(RequestType.GET_LIMITS)) {
                val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name())
                val mc: MotorConfiguration? = getMotorConfiguration(jointName)
                dxl.updateLimitsFromBytes(mc, properties, bytes)
            } else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
                val jointName: String = request.getProperty(BottleConstants.JOINT_NAME, Joint.UNKNOWN.name())
                val mc: MotorConfiguration? = getMotorConfiguration(jointName)
                val propertyName: String =
                    request.getProperty(BottleConstants.PROPERTY_NAME, JointProperty.UNRECOGNIZED.name())
                dxl.updateParameterFromBytes(propertyName, mc, properties, bytes)
                val partial = properties[BottleConstants.TEXT]
                if (partial != null && !partial.isEmpty()) {
                    val joint: Joint = Joint.valueOf(jointName)
                    properties[BottleConstants.TEXT] =
                        java.lang.String.format(
                            "My %s %s is %s",
                            Joint.toText(joint),
                            propertyName.lowercase(Locale.getDefault()),
                            partial
                        )
                }
            } else if (type.equals(RequestType.LIST_MOTOR_PROPERTY) ||
                type.equals(RequestType.SET_LIMB_PROPERTY) ||
                type.equals(RequestType.SET_MOTOR_PROPERTY)
            ) {
                val err: String = dxl.errorMessageFromStatus(bytes)
                if (err != null && !err.isEmpty()) {
                    request.assignError(err)
                }
            } else {
                LOGGER.severe(
                    java.lang.String.format(
                        "%s.updateRequestFromBytes: Unhandled response for %s",
                        CLSS,
                        type.name()
                    )
                )
            }
        }
    }

    // The bytes array contains the results of a request for status. It may be the concatenation
    // of several responses. Update the loacal motor configuration map and return a map keyed by motor
    // id to be aggregated by the MotorManager with similar responses from other motors.
    // 
    private fun updateStatusFromBytes(propertyName: String, bytes: ByteArray?): Map<Int, String> {
        val props: Map<Int, String> = HashMap()
        dxl.updateParameterArrayFromBytes(propertyName, configurationsById, bytes, props)
        return props
    }

    /*
	 * Guarantee that consecutive writes won't be closer than MIN_WRITE_INTERVAL
	 */
    private fun writeBytesToSerial(bytes: ByteArray?) {
        if (bytes != null && bytes.size > 0) {
            try {
                val now = System.nanoTime() / 1000000
                val interval = now - timeOfLastWrite
                if (interval < MIN_WRITE_INTERVAL) {
                    Thread.sleep(MIN_WRITE_INTERVAL - interval)
                    //LOGGER.info(String.format("%s(%s).writeBytesToSerial: Slept %d msecs",CLSS,controllerName,MIN_WRITE_INTERVAL-interval));
                }
                LOGGER.info(
                    String.format(
                        "%s(%s).writeBytesToSerial: Write interval %d msecs",
                        CLSS,
                        controllerName,
                        interval
                    )
                )
                val success: Boolean = port.writeBytes(bytes)
                timeOfLastWrite = System.nanoTime() / 1000000
                port.purgePort(SerialPort.PURGE_RXCLEAR or SerialPort.PURGE_TXCLEAR) // Force the write to complete
                if (!success) {
                    LOGGER.severe(
                        java.lang.String.format(
                            "%s(%s).writeBytesToSerial: Failed write of %d bytes to %s",
                            CLSS,
                            controllerName,
                            bytes.size,
                            port.getPortName()
                        )
                    )
                }
            } catch (ie: InterruptedException) {
                LOGGER.severe(
                    java.lang.String.format(
                        "%s(%s).writeBytesToSerial: Interruption writing to %s (%s)",
                        CLSS,
                        controllerName,
                        port.getPortName(),
                        ie.localizedMessage
                    )
                )
            } catch (spe: SerialPortException) {
                LOGGER.severe(
                    java.lang.String.format(
                        "%s(%s).writeBytesToSerial: Error writing to %s (%s)",
                        CLSS,
                        controllerName,
                        port.getPortName(),
                        spe.getLocalizedMessage()
                    )
                )
            }
        }
    }
    // ============================== SerialPortEventListener ===============================
    /**
     * Handle the response from the serial request. Note that all our interactions with removing from the
     * responseQueue and dealing with remainder are synchronized here.
     *
     * Unless an error is returned, the response queue must have at least one response for associating results.
     */
    @Synchronized
    fun serialEvent(event: SerialPortEvent) {
        LOGGER.info(String.format("%s(%s).serialEvent queue is %d", CLSS, controllerName, responseQueue.size))
        if (event.isRXCHAR()) {
            var req: MessageBottle? = null
            var wrapper: MessageWrapper? = null
            if (!responseQueue.isEmpty()) {
                wrapper = responseQueue.getFirst()
                req = wrapper.message
            }
            // The value is the number of bytes in the read buffer
            val byteCount: Int = event.getEventValue()
            LOGGER.info(
                java.lang.String.format(
                    "%s(%s).serialEvent (%s) %s: expect %d msgs got %d bytes",
                    CLSS,
                    controllerName,
                    event.getPortName(),
                    if (req == null) "" else req.fetchRequestType().name(),
                    wrapper?.responseCount ?: 0,
                    byteCount
                )
            )
            if (byteCount > 0) {
                try {
                    var bytes: ByteArray? = port.readBytes(byteCount)
                    bytes = prependRemainder(bytes)
                    bytes = dxl.ensureLegalStart(bytes) // null if no start characters
                    if (bytes != null) {
                        var nbytes = bytes.size
                        LOGGER.info(
                            String.format(
                                "%s(%s).serialEvent: read =\n%s",
                                CLSS,
                                controllerName,
                                dxl.dump(bytes)
                            )
                        )
                        val mlen: Int = dxl.getMessageLength(bytes) // First message
                        if (mlen < 0 || nbytes < mlen) {
                            LOGGER.info(
                                String.format(
                                    "%s(%s).serialEvent Message too short (%d), requires additional read",
                                    CLSS,
                                    controllerName,
                                    nbytes
                                )
                            )
                            return
                        } else if (dxl.errorMessageFromStatus(bytes) != null) {
                            LOGGER.severe(
                                String.format(
                                    "%s(%s).serialEvent: ERROR: %s",
                                    CLSS,
                                    dxl.errorMessageFromStatus(bytes)
                                )
                            )
                            if (req == null) return  // The original request was not supposed to have a response.
                        }
                        if (returnsStatusArray(req)) {  // Some requests return a message for each motor
                            var nmsgs = nbytes / STATUS_RESPONSE_LENGTH
                            if (nmsgs > wrapper.getResponseCount()) nmsgs = wrapper.getResponseCount()
                            nbytes = nmsgs * STATUS_RESPONSE_LENGTH
                            if (nbytes < bytes.size) {
                                bytes = truncateByteArray(bytes, nbytes)
                            }
                            val propertyName: String = req.getProperty(BottleConstants.PROPERTY_NAME, "NONE")
                            val map = updateStatusFromBytes(propertyName, bytes)
                            for (key in map.keys) {
                                val param = map[key]
                                val name: String = configurationsById[key].getJoint().name()
                                req.setJointValue(name, param)
                                wrapper!!.decrementResponseCount()
                                LOGGER.info(
                                    String.format(
                                        "%s(%s).serialEvent: received %s (%d remaining) = %s",
                                        CLSS, controllerName, name, wrapper.responseCount, param
                                    )
                                )
                            }
                        }
                        if (wrapper.getResponseCount() <= 0) {
                            responseQueue.removeFirst()
                            if (isSingleControllerRequest(req)) {
                                updateRequestFromBytes(req, bytes)
                                motorManager.handleSingleControllerResponse(req)
                            } else {
                                motorManager.handleAggregatedResponse(req)
                            }
                        }
                    }
                } catch (ex: SerialPortException) {
                    System.out.println(ex)
                }
            }
        }
    }

    private fun configurationsForLimb(limb: Limb): Map<String, MotorConfiguration> {
        val result: MutableMap<String, MotorConfiguration> = HashMap<String, MotorConfiguration>()
        for (mc in configurationsByName.values) {
            if (mc.getLimb().equals(limb)) {
                result[mc.getJoint().name()] = mc
            }
        }
        return result
    }

    /**
     * Combine the remainder from the previous serial read. Set the remainder null.
     * @param bytes
     * @return
     */
    private fun prependRemainder(bytes: ByteArray?): ByteArray? {
        if (remainder == null) return bytes
        val combination = ByteArray(remainder!!.size + bytes!!.size)
        System.arraycopy(remainder, 0, combination, 0, remainder!!.size)
        System.arraycopy(bytes, 0, combination, remainder!!.size, bytes.size)
        remainder = null
        return combination
    }

    /**
     * Create a remainder from extra bytes at the end of the array.
     * Remainder should always be null as we enter this routine.
     * @param bytes
     * @param nbytes count of bytes we need.
     * @return
     */
    private fun truncateByteArray(bytes: ByteArray?, nbytes: Int): ByteArray? {
        var nbytes = nbytes
        if (nbytes > bytes!!.size) nbytes = bytes.size
        if (nbytes == bytes.size) return bytes
        val copy = ByteArray(nbytes)
        System.arraycopy(bytes, 0, copy, 0, nbytes)
        remainder = null
        return copy
    }

    companion object {
        const val CLSS = "MotorController"
        val LOGGER = Logger.getLogger(CLSS)
        const val BAUD_RATE = 1000000
        const val MIN_WRITE_INTERVAL = 100   // msecs between writes (50 was too short)
        const val STATUS_RESPONSE_LENGTH = 8 // byte count
    }
}