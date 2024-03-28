/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.motor.dynamixel.DxlMessage
import jssc.SerialPort
import jssc.SerialPortException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.logging.Logger

/**
 * Handle requests directed to a specific set of motors. All motors under the
 * same controller are connected to the same serial port. We respond to the group controller
 * using call-backs. The responses from the serial port do not necessarily keep
 * to request boundaries. All we are sure of is that the requests are processed
 * in order.
 *
 * The configuration array has only those joints that are part of the controller set.
 * It is important that the MotorConfiguration objects are the same objects
 * (not clones) as those held by the MotorManager (MotorGroupController).
 *
 * Since the same request object is processed in parallel by multiple MotorControl
 * and SerialResponder objects, it is imperative that any object updates be synchronized.
 *
 * @param p - the serial port shared by the motors under control
 * @param req - channel for requests from the parent (motor manager)
 * @param rsp - channel for responses sent to the parent (motor manager)
 */
class MotorController(name:String,p:SerialPort,req: Channel<MessageBottle>,rsp:Channel<MessageBottle>) : Controller {
    @DelicateCoroutinesApi
    private val scope = GlobalScope // For long-running coroutines
    override val controllerName = name
    private var port: SerialPort
    private var running:Boolean
    private var job: Job
    private val configurationsByJoint: MutableMap<Joint, MotorConfiguration>
    private var parentRequestChannel = req
    private var parentResponseChannel = rsp
    private val requestQueue: Channel<MessageBottle>
    private val responseQueue: Channel<MessageBottle>
    // The pending message allows the serial callback to accumulate results for the same message
    private val responder: SerialResponder
    private var timeOfLastWrite: Long

    val configurations: MutableCollection<MotorConfiguration>
        get() = configurationsByJoint.values

    fun getMotorConfiguration(joint: Joint): MotorConfiguration? {
        return configurationsByJoint[joint]
    }

    fun putMotorConfiguration(joint: Joint, mc: MotorConfiguration) {
        configurationsByJoint[joint] = mc
    }

    /**
     * Open and configure the port.
     * Dynamixel documentation: No parity, 1 stop bit, 8 bits of data, no flow control
     *
     * At one point, we thought we should initialize the motors somehow.  This is now
     * taken care of by the dispatcher. The dispatcher:
     *     1) requests a list of current positions (thus updating the MotorConfigurations)
     *     2) sets travel speeds to "normal"
     *     3) moves any limbs that are "out-of-bounds" back into range.
     */
    @DelicateCoroutinesApi
    override suspend fun execute() {
        LOGGER.info(String.format("%s.execute: %s initializing port %s)",CLSS, controllerName, port.portName))
        if (!port.isOpened) {
            try {
                val success: Boolean = port.openPort()
                if (success && port.isOpened) {
                    port.setParams(BAUD_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE)
                    port.eventsMask = SerialPort.MASK_RXCHAR
                    port.purgePort(SerialPort.PURGE_RXCLEAR)
                    port.purgePort(SerialPort.PURGE_TXCLEAR)
                    port.flowControlMode = SerialPort.FLOWCONTROL_RTSCTS_IN or SerialPort.FLOWCONTROL_RTSCTS_OUT
                    port.addEventListener(responder)
                }
                else {
                    LOGGER.severe(String.format("%s.execute: Failed to open port %s for %s",
                        CLSS, port.portName, controllerName))
                }
            }
            catch (spe: SerialPortException) {
                LOGGER.severe(String.format("%s.execute: Error opening port %s for %s (%s)",
                    CLSS, port.portName, controllerName, spe.getLocalizedMessage()))
                return
            }
            LOGGER.info(String.format("%s.execute: Initialized port %s)", CLSS, port.getPortName()))
        }
        // Port is open, now use it. Simply run in a loop getting the next request.
        running = true
        job = scope.launch(Dispatchers.IO) {
            while (running) {
                processRequest()
            }
        }
    }

    override suspend fun shutdown() {
        try {
            port.closePort()
        }
        catch (spe: SerialPortException) {
            LOGGER.severe(String.format("%s.shutdown: Error closing port for %s (%s)",
                CLSS,controllerName,spe.localizedMessage))
        }
        if (running) {
            running = false
            job.cancel()
        }
    }
    /**
     * This method waits for the next request in the inout. It then processes the message synchronously
     * until the response is returned to the group controller. Ignore requests that apply to a single
     * controller that is not this one. Otherwise each request will have a response.
     */
    suspend fun processRequest() {
        val request = parentRequestChannel.receive()    //  Waits for message
        LOGGER.info(String.format("%s.processRequest:%s processing %s",CLSS,controllerName,request.type.name));
        // Do nothing if the joint or limb isn't on this controller
        // -- a limb is on one side or the other, not both
        if(isSingleControllerRequest(request)) {
            val joint: Joint=request.joint
            val limb: Limb=request.limb
            if(!joint.equals(Joint.NONE)) {
                val mc: MotorConfiguration=configurationsByJoint[joint] ?: return
            }
            else if(!limb.equals(Limb.NONE)) {
                val count=configurationsForLimb(limb).size
                if(count == 0) { return }
            }
        }
        // Make sure that there is MIN_WRITE_INTERVAL between writes
        val now = System.currentTimeMillis()
        if( now - timeOfLastWrite < MIN_WRITE_INTERVAL ) {
            delay(MIN_WRITE_INTERVAL - (now - timeOfLastWrite))
            timeOfLastWrite = System.currentTimeMillis()
        }
        // Now construct the byte array to write to the port
        if (isSingleWriteRequest(request)) {
            val bytes = messageToBytes(request)
            if (bytes != null) {
                if (request.control.responseCount[controllerName]!! > 0) {
                    requestQueue.send(request)
                }
                writeBytesToSerial(bytes)
                LOGGER.info(String.format("%s.processRequest: %s wrote %d bytes", CLSS, controllerName, bytes.size))
            }
        }
        else {
            val byteArrayList = messageToByteList(request)
            if (request.control.responseCount[controllerName]!! > 0) {
                requestQueue.send(request)
            }
            var count = byteArrayList.size
            for (bytes in byteArrayList) {
                writeBytesToSerial(bytes)
                count--
                if(count>0) delay(MIN_WRITE_INTERVAL)    // This could be significant
                LOGGER.info(String.format("%s.processRequest: %s wrote %d bytes", CLSS, controllerName, bytes.size))
            }
        }

        if( request.control.responseCount[controllerName]!!>0 ) {
            val response = responseQueue.receive()
            LOGGER.info(String.format("%s.processRequest: %s got response", CLSS, controllerName))
            parentResponseChannel.send(response)
        }
        else {
            synthesizeResponse(request)
            LOGGER.info(String.format("%s.processRequest: %s responding %s with synthesis", CLSS, controllerName,request.type.name))
            parentResponseChannel.send(request)
        }
    }

    // ============================= Private Helper Methods =============================
    /**
     * @param msg the request
     * @return true if this is the type of request satisfied by a single controller.
     */
    private fun isSingleControllerRequest(msg: MessageBottle): Boolean {
        if (msg.type.equals(RequestType.GET_GOALS) ||
            msg.type.equals(RequestType.GET_LIMITS) ||
            msg.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
            msg.type.equals(RequestType.LIST_MOTOR_PROPERTY) ||
            msg.type.equals(RequestType.SET_LIMB_PROPERTY) ||
            msg.type.equals(RequestType.SET_MOTOR_PROPERTY) ) {
            return true
        }
        return false
    }

    /**
     * The list of NOT single request here should match the request types in messageToByteList().
     * @param msg the request
     * @return true if this request translates into a single serial message.
     * false implies that an array of serial messages are required.
     */
    private fun isSingleWriteRequest(msg: MessageBottle): Boolean {
        return if (msg.type.equals(RequestType.INITIALIZE_JOINTS) ||
            msg.type.equals(RequestType.READ_MOTOR_PROPERTY) ||
            msg.type.equals(RequestType.LIST_MOTOR_PROPERTY) ||
            msg.type.equals(RequestType.SET_POSE) ) {
            false
        }
        else {
            true
        }
    }

    /**
     * Convert the request message into a command for the serial port. As a side
     * effect set the number of expected responses. This can vary by request type.
     * @param wrapper
     * @return
     */
    private fun messageToBytes(request: MessageBottle): ByteArray? {
        request.control.responseCount[controllerName] = 0 // No response, unless specified otherwise
        var bytes: ByteArray  = ByteArray(0)
        val type: RequestType = request.type
        if(DEBUG) LOGGER.info(String.format("%s.messageToBytes: %s handling %s",CLSS,controllerName,type.name))
        if (type.equals(RequestType.COMMAND) &&
            request.command.equals(CommandType.FREEZE) ) {

            val propertyValues = request.getJointValueIterator()
            if( propertyValues.hasNext() ) {             // There should be only one entry
                val pv = propertyValues.next()
                val joint = pv.joint // This is the one we want to freeze
                val prop  = pv.property

                for (mc in configurationsByJoint.values) {
                    if (mc.joint.equals(joint)) {
                        mc.isTorqueEnabled = true
                        bytes = DxlMessage.byteArrayToSetProperty(configurationsByJoint, prop)
                        break
                    }
                }
            }
        }
        else if (type.equals(RequestType.COMMAND) &&
            request.command.equals(CommandType.RELAX) ) {
            val propertyValues = request.getJointValueIterator()
            if( propertyValues.hasNext() ) {             // There should be only one entry
                val pv = propertyValues.next()
                val joint = pv.joint // This is the one we want to freeze
                val prop = pv.property
                for (mc in configurationsByJoint.values) {
                    if (mc.joint.equals(joint)) {
                        mc.isTorqueEnabled = false
                        bytes = DxlMessage.byteArrayToSetProperty(configurationsByJoint, prop)
                        break
                    }
                }
            }
        }
        else if (type.equals(RequestType.GET_GOALS)) {
            val joint = request.joint
            for (mc in configurationsByJoint.values) {
                if (mc.joint.equals(joint)) {
                    bytes = DxlMessage.bytesToGetGoals(mc.id)
                    request.control.responseCount[controllerName] = 1 // Status message
                    break
                }
            }
        }
        else if (type.equals(RequestType.GET_LIMITS)) {
            val joint = request.joint
            for (mc in configurationsByJoint.values) {
                if (mc.joint.equals(joint)) {
                    bytes = DxlMessage.bytesToGetLimits(mc.id)
                    request.control.responseCount[controllerName] = 1 // Status message
                    break
                }
            }
        }
        // Assume a dynamic property
        else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val joint = request.joint
            val mc = RobotModel.motorsByJoint[joint]!!
            if (mc.joint.equals(joint)) {
                bytes = DxlMessage.bytesToGetProperty(mc.id,request.jointDynamicProperty)
                request.control.responseCount[controllerName] = 1 // Status message
            }
        }
        else if (type.equals(RequestType.SET_LIMB_PROPERTY))  {
            val limb = request.limb
            val propertyValues = request.getJointValueIterator()
            if (propertyValues.hasNext()) {             // There should be only one entry
                val pv = propertyValues.next()
                val prop = pv.property
                val value = pv.value.toDouble()
                // Loop over motor config map, set the property
                val configs = configurationsForLimb(limb)
                for (mc in configs.values) {
                    mc.setDynamicProperty(prop,value)
                }
                bytes =
                    DxlMessage.byteArrayToSetProperty(configs, prop) // Returns null if limb not on this controller
                // ASYNC WRITE, no response. Let source set text.
            }
        }
        else if (type.equals(RequestType.SET_MOTOR_PROPERTY)) {
            val joint = request.joint
            val prop = request.jointDynamicProperty
            val value = request.value.toDouble()
            val mc = RobotModel.motorsByJoint[joint]!!
            if (mc.joint.equals(joint)) {
                bytes = DxlMessage.bytesToSetProperty(mc, prop, value.toDouble())
                if (prop.equals(JointDynamicProperty.POSITION) ) {
                    val duration = mc.travelTime
                    if (request.duration < duration) request.duration = duration
                    request.text = String.format("My %s is at %.0f", Joint.toText(mc.joint),mc.position)
                }
                else if (prop.equals(JointDynamicProperty.STATE) ) {
                    request.text = String.format("My %s state is torque-%s", Joint.toText(mc.joint),
                        if (value == 0.0) "disabled" else "enabled"
                    )
                }
                else {
                    request.text = String.format("My %s %s is %s",Joint.toText(mc.joint), prop.name, value)
                }
            }
            request.control.responseCount[controllerName] = 1 // Status message
        }
        else if (type.equals(RequestType.NONE)) {
            LOGGER.warning(String.format("%s.messageToBytes: Empty request - ignored (%s)",
                CLSS, type.name ) )
        }
        else {
            LOGGER.severe(String.format("%s.messageToBytes: %s unhandled request %s",
                CLSS, controllerName,type.name))
        }
        LOGGER.info(String.format("%s.messageToBytes: %s expect %d responses %d bytes",CLSS,
            controllerName,request.control.responseCount[controllerName],bytes.size))
        return bytes
    }
    /**
     * Convert the request message into a list of commands for the serial port. As a side
     * effect set the number of expected responses. This can vary by request type (and may
     * be none).
     * @param wrapper
     * @return
     */
    private fun messageToByteList(request: MessageBottle): List<ByteArray> {
        var list: List<ByteArray> = mutableListOf<ByteArray>()
        val type: RequestType = request.type
        if(DEBUG) LOGGER.info(String.format("%s.messageToByteList: %s handling %s",
            CLSS,controllerName,type.name))
        if (type.equals(RequestType.INITIALIZE_JOINTS)) {
            list = DxlMessage.byteArrayListToInitializePositions(configurationsByJoint)
            val duration: Long = DxlMessage.mostRecentTravelTime
            if (request.duration < duration) request.duration = duration
            request.control.responseCount[controllerName] = 0 // No response
        }
        else if( type==RequestType.LIST_MOTOR_PROPERTY ||
                 type==RequestType.READ_MOTOR_PROPERTY ) {
            val limb = request.limb
            val prop = request.jointDynamicProperty
            if( limb.equals(Limb.NONE) ) {
                list = DxlMessage.byteArrayListToListProperty(prop, configurationsByJoint.values)
                request.control.responseCount[controllerName] = configurationsByJoint.size // Status packet for each motor
            }
            else {
                val configs: Map<Joint, MotorConfiguration> = configurationsForLimb(limb)
                list = DxlMessage.byteArrayListToListProperty(prop, configs.values)
                request.control.responseCount[controllerName] = configs.size // Status packet for each motor in limb
            }
        }
        else if (type==RequestType.SET_POSE) {
            val poseName: String = request.pose
            list = DxlMessage.byteArrayListToSetPose(poseName,configurationsByJoint)
            val duration: Long = DxlMessage.mostRecentTravelTime
            if (request.duration < duration) request.duration = duration
            request.control.responseCount[controllerName] = 0 // AYNC WRITE, no responses
        }
        else {
            LOGGER.severe(String.format("%s.messageToByteList: Unhandled request type %s",
                CLSS,type.name))
        }
        if(DEBUG) {
            for (bytes in list) {
                LOGGER.info(String.format( "%s.messageToByteList: %s %s = \n%s",
                    CLSS,controllerName,request.type,DxlMessage.dump(bytes)))
            }
        }

        return list
    }

    /**
     * We have just written a message to the serial port that generates no
     * response. Make one up and reply to the group controller. It expects
     * a response from each controller.
     *
     * A random acknowledgement will be added before the response is presented
     * to the user.
     * @param msg the request
     */
    private fun synthesizeResponse(msg: MessageBottle) {
        if (msg.type.equals(RequestType.INITIALIZE_JOINTS) ||
            msg.type.equals(RequestType.SET_LIMB_PROPERTY) ||
            msg.type.equals(RequestType.SET_POSE) ) {
            msg.text = ""   // Random acknowledgement added later
        }
        else if(msg.type.equals(RequestType.SET_MOTOR_PROPERTY) ) {
            // Description is added by
        }
        else if( msg.type.equals(RequestType.COMMAND))  {
            if (msg.command.equals(CommandType.FREEZE) ||
                msg.command.equals(CommandType.RELAX) ) {
                msg.text=""
            }
            else {
                msg.error = String.format("could not synthesize a response for command %s", msg.command.name)
            }
        }
        else {
            msg.error = String.format("could not synthesize a response for message %s", msg.type.name)
        }
    }

    /*
	 * Guarantee that consecutive writes won't be closer than MIN_WRITE_INTERVAL.
	 * Use of serial ports must be configured on command line.
	 */
    private fun writeBytesToSerial(bytes: ByteArray) {
        LOGGER.info(String.format("%s.writeBytesToSerial: %s %d bytes to %s",CLSS,controllerName,bytes.size,port.portName))
        if( bytes.size>0 ) {
            try {
                val success: Boolean = port.writeBytes(bytes)
                timeOfLastWrite = System.currentTimeMillis()
                port.purgePort(SerialPort.PURGE_RXCLEAR or SerialPort.PURGE_TXCLEAR) // Force the write to complete
                if (!success) {
                    LOGGER.severe(String.format("%s(%s).writeBytesToSerial: Failed write of %d bytes to %s",
                        CLSS,controllerName, bytes.size,port.getPortName()) )
                }
            }
            catch (spe: SerialPortException) {
                LOGGER.severe(String.format("%s(%s).writeBytesToSerial: Error writing to %s (%s)",
                    CLSS,controllerName,port.getPortName(),spe.getLocalizedMessage() ) )
            }
        }
        LOGGER.info(String.format("%s.writeBytesToSerial: %s wrote %d bytes to %s",CLSS,controllerName,bytes.size,port.portName))
    }

    private fun configurationsForLimb(limb: Limb): Map<Joint, MotorConfiguration> {
        val result: MutableMap<Joint, MotorConfiguration> = HashMap<Joint, MotorConfiguration>()
        for (mc in configurationsByJoint.values) {
            if (mc.limb.equals(limb)) {
                result[mc.joint] = mc
            }
        }
        return result
    }

    // Attempt to recover from an error
    // NOTE: If there is an error on the controller itself, we need to recycle power
    fun reset() {

    }

    private val CLSS = "MotorController"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    private val BAUD_RATE = 1000000
    private val MIN_WRITE_INTERVAL = 100L   // msecs between writes (50 was too short)

    override val controllerType = ControllerType.MOTOR

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_MOTOR)
        port = p
        requestQueue  = Channel<MessageBottle>(1)
        responseQueue = Channel<MessageBottle>(1)
        responder = SerialResponder(name,requestQueue,responseQueue)
        configurationsByJoint = mutableMapOf<Joint, MotorConfiguration>()
        timeOfLastWrite = System.currentTimeMillis()
        running = false
        job = Job()
        LOGGER.info(String.format("%s.init: created %s...", CLSS,name))
    }
}