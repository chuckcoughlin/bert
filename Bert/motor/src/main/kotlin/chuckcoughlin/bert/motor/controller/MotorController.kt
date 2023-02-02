/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.CommandType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.Joint
import chuckcoughlin.bert.common.model.JointDynamicProperty
import chuckcoughlin.bert.common.model.Limb
import chuckcoughlin.bert.common.model.MotorConfiguration
import chuckcoughlin.bert.motor.dynamixel.DxlMessage
import jssc.SerialPort
import jssc.SerialPortEvent
import jssc.SerialPortEventListener
import jssc.SerialPortException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.contracts.InvocationKind

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
 * @param p - the serial port shared by the motors under control
 * @param parent - the motor manager
 * @param req - channel for requests from the parent (motor manager)
 * @param rsp - channel for responses sent to the parent (motor manager)
 */
class MotorController(p: SerialPort, parent: MotorManager,req: Channel<MessageBottle>,rsp:Channel<MessageBottle>) : Controller,SerialPortEventListener {

    private val port: SerialPort
    private var running:Boolean
    private val motorManager: MotorManager
    private val condition: Condition
    private val configurationsById: MutableMap<Int, MotorConfiguration>
    private val configurationsByJoint: MutableMap<Joint, MotorConfiguration>
    private var parentRequestChannel = req
    private var parentResponseChannel = rsp
    private val lock: Lock
    private var remainder: ByteArray? = null
    private val requestQueue // requests waiting to be processed
            : LinkedList<MessageBottle>
    private val responseQueue // responses waiting for serial results
            : LinkedList<MessageWrapper>
    private var timeOfLastWrite: Long



    val configurations: Map<Joint, MotorConfiguration>
        get() = configurationsByJoint

    fun getMotorConfiguration(joint: Joint): MotorConfiguration? {
        return configurationsByJoint[joint]
    }

    fun putMotorConfiguration(joint: Joint, mc: MotorConfiguration) {
        configurationsById[mc.id] = mc
        configurationsByJoint[joint] = mc
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
    override suspend fun start() {
        LOGGER.info(String.format("%s(%s).start: Initializing port %s)",
            CLSS, controllerName, port.getPortName()))
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
                    LOGGER.severe(String.format(
                        "%s.initialize: Failed to open port %s for %s",
                        CLSS, port.getPortName(), controllerName))
                }
            }
            catch (spe: SerialPortException) {
                LOGGER.severe(String.format("%s.initialize: Error opening port %s for %s (%s)",
                    CLSS, port.getPortName(), controllerName, spe.getLocalizedMessage()))
                return
            }
            LOGGER.info(String.format("%s.initialize: Initialized port %s)", CLSS, port.getPortName()))
        }
        // Port is open, now use it.
        running = true
        runBlocking<Unit> {
            launch {
                Dispatchers.IO
                while (running) {
                    select<Unit> {
                        /**
                         * On receipt of a message from the SerialPort,
                         * decypher and forward to the MotorManager.
                         */
                        async {
                            receiveSerialResponse()
                        }
                        /**
                         * The parent request is a motor command. Convert it
                         * into a message for the SerialPort amd write.
                         */
                        parentRequestChannel.onReceive() {
                            receiveRequest(it)   // stdOut
                        }
                    }
                }
            }
        }
    }

    override suspend fun stop() {
        try {
            port.closePort()
        }
        catch (spe: SerialPortException) {
            LOGGER.severe(String.format("%s.close: Error closing port for %s (%s)",
                    CLSS,controllerName,spe.getLocalizedMessage()))
        }
        running = false
    }

    /**
     * This method blocks until any prior request completes. Ignore requests that apply to a single controller
     * and that controller is not this one, otherwise add the request to the request queue.
     * @param request
     */
    suspend fun receiveRequest(request: MessageBottle) {
        lock.lock()
        //LOGGER.info(String.format("%s(%s).receiveRequest: processing %s",CLSS,controllerName,request.fetchRequestType().name()));
        try {
            if (isLocalRequest(request)) {
                handleLocalRequest(request)
                return
            }
            else if (isSingleControllerRequest(request)) {
                // Do nothing if the joint or limb isn't in our controllerName.
                val joint: Joint = request.joint
                val cName: String = request.controller
                val limb: Limb = request.limb
                if( !joint.equals(Joint.NONE) ) {
                    val mc: MotorConfiguration = configurationsByJoint[joint] ?: return
                }
                else if (!cName.isEmpty()) {
                    if (!cName.equals(controllerName, ignoreCase = true)) {
                        return
                    }
                }
                else if (!limb.equals(Limb.NONE) ) {
                    val count = configurationsForLimb(limb).size
                    if (count == 0) {
                        return
                    }
                }
                else {
                    val property: JointDynamicProperty = request.property
                    LOGGER.info(String.format("%s(%s).receiveRequest: %s (%s)",
                            CLSS,controllerName,request.type.name,property.name))
                }
            }
            else {
                LOGGER.info(String.format("%s(%s).receiveRequest: multi-controller request (%s)",
                        CLSS,controllerName,request.type.name))
            }
            requestQueue.addLast(request)
            // LOGGER.info(String.format("%s(%s).receiveRequest: added to request queue %s",CLSS,controllerName,request.fetchRequestType().name()));
            condition.signal()
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
     suspend fun receiveSerialResponse() {
        while (running) {
            lock.lock()
            try {
                condition.await()
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
            }
            catch (ie: InterruptedException) {
            }
            finally {
                lock.unlock()
            }
        }
    }

    // ============================= Private Helper Methods =============================
    // Create a response for a request that can be handled immediately. There aren't many of them.
    private fun handleLocalRequest(request: MessageBottle): MessageBottle {
        // The following two requests simply use the current positions of the motors, whatever they are
        if (request.type.equals(RequestType.COMMAND)) {
            val command: CommandType = request.command
            LOGGER.warning(String.format(
                    "%s(%s).createResponseForLocalRequest: command=%s",
                    CLSS,controllerName,command.name) )
            if (command.equals(CommandType.RESET)) {
                remainder = null // Resync after dropped messages.
                responseQueue.clear()
                motorManager.handleAggregatedResponse(request)
            }
            else {
                val msg = String.format("Unrecognized command: %s", command)
                request.error = msg
            }
        }
        return request
    }

    /**
     * A local command is one that can be handled directly without any serial
     * communications.
     * @param msg the request
     * @return true if this is the type of request that can be satisfied locally.
     */
    private fun isLocalRequest(msg: MessageBottle): Boolean {
        return if (msg.type.equals(RequestType.COMMAND) &&
            msg.command.equals(CommandType.RESET) ) {
            true
        }
        else false
    }

    /**
     * @param msg the request
     * @return true if this is the type of request satisfied by a single controller.
     */
    private fun isSingleControllerRequest(msg: MessageBottle): Boolean {
        if (msg.type.equals(RequestType.GET_GOALS) ||
            msg.type.equals(RequestType.GET_LIMITS) ||
            msg.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
            msg.type.equals(RequestType.SET_LIMB_PROPERTY) ||
            msg.type.equals(RequestType.SET_MOTOR_PROPERTY) ) {
            return true
        }
        else if (msg.type.equals(RequestType.LIST_MOTOR_PROPERTY) &&
                (!msg.controller.isEmpty() || !msg.limb.equals(Limb.NONE))) {
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
        return if (msg.type.equals(RequestType.INITIALIZE_JOINTS) ||
            msg.type.equals(RequestType.LIST_MOTOR_PROPERTY) ||
            msg.type.equals(RequestType.SET_POSE) ) {
            false
        }
        else {
            true
        }
    }

    /**
     * @param msg the request
     * @return true if this is the type of message that returns a separate
     * status response for every motor referenced in the request.
     * (There may be only one).
     */
    private fun returnsStatusArray(msg: MessageBottle): Boolean {
        return if (msg.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
                  msg.type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
            true
        }
        else {
            false
        }
    }

    /**
     * Convert the request message into a command for the serial port. As a side
     * effect set the number of expected responses. This can vary by request type.
     * @param wrapper
     * @return
     */
    private fun messageToBytes(wrapper: MessageWrapper): ByteArray? {
        val request: MessageBottle = wrapper.message
        wrapper.responseCount = 0 // No response, unless specified otherwise
        var bytes: ByteArray? = null
        val type: RequestType = request.type
        if (type.equals(RequestType.COMMAND) &&
            request.command.equals(CommandType.FREEZE) ) {

            val propertyValues = request.getPropertyValueIterator()
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
            val propertyValues = request.getPropertyValueIterator()
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
                    wrapper.responseCount = 1 // Status message
                    break
                }
            }
        }
        else if (type.equals(RequestType.GET_LIMITS)) {
            val joint = request.joint
            for (mc in configurationsByJoint.values) {
                if (mc.joint.equals(joint)) {
                    bytes = DxlMessage.bytesToGetLimits(mc.id)
                    wrapper.responseCount = 1 // Status message
                    break
                }
            }
        }
        else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val propertyValues = request.getPropertyValueIterator()
            if( propertyValues.hasNext() ) {             // There should be only one entry
                val pv = propertyValues.next()
                val joint = pv.joint // This is the one we want to freeze
                val prop = pv.property
                for (mc in configurationsByJoint.values) {
                    if (mc.joint.equals(joint)) {
                        bytes = DxlMessage.bytesToGetLimits(mc.id)
                        wrapper.responseCount = 1 // Status message
                        break
                    }
                }
            }
        }
        else if (type.equals(RequestType.SET_LIMB_PROPERTY))  {
            val limb = request.limb
            val propertyValues = request.getPropertyValueIterator()
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
            val propertyValues = request.getPropertyValueIterator()
            if (propertyValues.hasNext()) {             // There should be only one entry
                val pv = propertyValues.next()
                val joint = pv.joint
                val prop = pv.property
                val value = pv.value.toDouble()
                for (mc in configurationsByJoint.values) {
                    if (mc.joint.equals(joint)) {
                        bytes = DxlMessage.bytesToSetProperty(mc, prop, value.toDouble())
                        if (prop.equals(JointDynamicProperty.POSITION) ) {
                            val duration = mc.travelTime
                            if (request.duration < duration) request.duration = duration
                            request.text = String.format("My position is %.0f", mc.position)
                        }
                        else if (prop.equals(JointDynamicProperty.STATE) ) {
                            request.text = String.format("My %s state is torque-%s", Joint.toText(mc.joint),
                                if (value == 0.0) "disabled" else "enabled"
                            )
                        }
                        else {
                            request.text = String.format("My %s %s is %s",Joint.toText(mc.joint), prop.name, value
                            )
                        }
                    }
                    wrapper.responseCount = 1 // Status message
                    break
                }
            }
            else {
                LOGGER.warning(String.format("%s.messageToBytes: Empty property value - ignored (%s)",
                            CLSS, type.name) )
            }
        }
        else if (type.equals(RequestType.NONE)) {
                LOGGER.warning(String.format("%s.messageToBytes: Empty request - ignored (%s)",
                        CLSS, type.name ) )
        }
        else {
            LOGGER.severe(String.format("%s.messageToBytes: Unhandled request type %s",
                CLSS, type.name))
        }
        LOGGER.info(String.format("%s.messageToBytes: %s = \n%s",
            CLSS, request.type.name, DxlMessage.dump(bytes)))
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
        val request: MessageBottle = wrapper.message
        var list: List<ByteArray> = ArrayList()
        val type: RequestType = request.type
        // Unfortunately broadcast requests don't work here. We have to concatenate the
        // requests into single long lists.
        if (type.equals(RequestType.INITIALIZE_JOINTS)) {
            list = DxlMessage.byteArrayListToInitializePositions(configurationsByJoint)
            val duration: Long = DxlMessage.mostRecentTravelTime
            if (request.duration < duration) request.duration = duration
            wrapper.responseCount = 0 // No response
        }
        else if (type.equals(RequestType.LIST_MOTOR_PROPERTY)) {
            val limb = request.limb
            val prop = request.property
            if( limb.equals(Limb.NONE) ) {
                list = DxlMessage.byteArrayListToListProperty(prop, configurationsByJoint.values)
                wrapper.responseCount = configurationsByJoint.size // Status packet for each motor
            }
            else {
                val configs: Map<String, MotorConfiguration> = configurationsForLimb(limb)
                list = DxlMessage.byteArrayListToListProperty(prop, configs.values)
                wrapper.responseCount = configs.size // Status packet for each motor in limb
            }
        }
        else if (type.equals(RequestType.SET_POSE)) {
            val poseName: String = request.pose
            list = DxlMessage.byteArrayListToSetPose(configurationsByJoint, poseName)
            val duration: Long = DxlMessage.mostRecentTravelTime
            if (request.duration < duration) request.duration = duration
            wrapper.responseCount = 0 // AYNC WRITE, no responses
        }
        else {
            LOGGER.severe(String.format("%s.messageToByteList: Unhandled request type %s",
                CLSS,type.name))
        }
        for (bytes in list) {
            LOGGER.info(String.format( "%s(%s).messageToByteList: %s = \n%s",
                CLSS,controllerName,request.type,DxlMessage.dump(bytes)))
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
        if (msg.type.equals(RequestType.INITIALIZE_JOINTS) ||
            msg.type.equals(RequestType.SET_POSE)) {
            motorManager.handleSynthesizedResponse(msg)
        }
        else if (msg.type.equals(RequestType.COMMAND)) {
            val cmd: CommandType = msg.command
            if (cmd.equals(CommandType.FREEZE) ||
                cmd.equals(CommandType.RELAX) ) {
                motorManager.handleSynthesizedResponse(msg) }
            else {
                LOGGER.severe(String.format("%s.synthesizeResponse: Unhandled response for command %s", CLSS, cmd))
                motorManager.handleSingleControllerResponse(msg) // Probably an error
            }
        }
        else if (msg.type.equals(RequestType.SET_LIMB_PROPERTY)) {
            motorManager.handleSingleControllerResponse(msg)
        }
        else {
            LOGGER.severe(String.format("%s.synthesizeResponse: Unhandled response for %s",
                    CLSS, msg.type.name))
            motorManager.handleSingleControllerResponse(msg) // Probably an error
        }
    }

    // We update the properties in the request from our serial message.
    // The properties must include motor type and orientation
    private fun updateRequestFromBytes(request: MessageBottle, bytes: ByteArray?) {
        val type: RequestType = request.type
        val properties: MutableMap<String, String> = request.getProperties()
        if (type.equals(RequestType.GET_GOALS)) {
            val joint = request.joint
            val mc: MotorConfiguration? = getMotorConfiguration(joint)
            DxlMessage.updateGoalsFromBytes(mc, properties, bytes)
        }
        else if (type.equals(RequestType.GET_LIMITS)) {
            val joint = request.joint
            val mc: MotorConfiguration = getMotorConfiguration(joint)
            DxlMessage.updateLimitsFromBytes(mc, properties, bytes)
        }
        else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val joint = request.joint
            val mc: MotorConfiguration = getMotorConfiguration(joint)
            val property = request.property
            DxlMessage.updateParameterFromBytes(property, mc, properties, bytes)
            val partial = request.text
            if (partial != null && !partial.isEmpty()) {
                request.text = String.format("My %s %s is %s",
                    Joint.toText(joint),property.name.lowercase(Locale.getDefault()),partial)
            }
        }
        else if (type.equals(RequestType.LIST_MOTOR_PROPERTY) ||
            type.equals(RequestType.SET_LIMB_PROPERTY) ||
            type.equals(RequestType.SET_MOTOR_PROPERTY)) {
            val err: String = DxlMessage.errorMessageFromStatus(bytes)
            request.error = err
        }
        else {
            LOGGER.severe(String.format("%s.updateRequestFromBytes: Unhandled response for %s",
                CLSS,type.name))
        }
    }

    // The bytes array contains the results of a request for status. It may be the concatenation
    // of several responses. Update the loacal motor configuration map and return a map keyed by motor
    // id to be aggregated by the MotorManager with similar responses from other motors.
    // 
    private fun updateStatusFromBytes(property: JointDynamicProperty, bytes: ByteArray): Map<Int, String> {
        val props: Map<Int, String> = HashMap()
        DxlMessage.updateParameterArrayFromBytes(property, configurationsById, bytes, props)
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
                LOGGER.info(String.format("%s(%s).writeBytesToSerial: Write interval %d msecs",
                        CLSS, controllerName,interval))
                val success: Boolean = port.writeBytes(bytes)
                timeOfLastWrite = System.nanoTime() / 1000000
                port.purgePort(SerialPort.PURGE_RXCLEAR or SerialPort.PURGE_TXCLEAR) // Force the write to complete
                if (!success) {
                    LOGGER.severe(String.format("%s(%s).writeBytesToSerial: Failed write of %d bytes to %s",
                            CLSS,controllerName, bytes.size,port.getPortName()) )
                }
            }
            catch (ie: InterruptedException) {
                LOGGER.severe(String.format("%s(%s).writeBytesToSerial: Interruption writing to %s (%s)",
                        CLSS,controllerName,
                        port.getPortName(),ie.localizedMessage))
            }
            catch (spe: SerialPortException) {
                LOGGER.severe(String.format("%s(%s).writeBytesToSerial: Error writing to %s (%s)",
                        CLSS,controllerName,port.getPortName(),spe.getLocalizedMessage() ) )
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
            LOGGER.info(String.format("%s(%s).serialEvent (%s) %s: expect %d msgs got %d bytes",
                    CLSS,controllerName,event.getPortName(),
                    if (req == null) "" else req.type.name,
                    wrapper?.responseCount ?: 0,
                    byteCount
                )
            )
            if (byteCount > 0) {
                try {
                    var bytes: ByteArray? = port.readBytes(byteCount)
                    bytes = prependRemainder(bytes)
                    bytes = DxlMessage.ensureLegalStart(bytes) // null if no start characters
                    if (bytes != null) {
                        var nbytes = bytes.size
                        LOGGER.info(String.format("%s(%s).serialEvent: read =\n%s",
                                CLSS,controllerName, DxlMessage.dump(bytes)))
                        val mlen: Int = DxlMessage.getMessageLength(bytes) // First message
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
                        }
                        else if (DxlMessage.errorMessageFromStatus(bytes) != null) {
                            LOGGER.severe( String.format("%s(%s).serialEvent: ERROR: %s",
                                    CLSS,DxlMessage.errorMessageFromStatus(bytes))
                            )
                            if (req == null) return  // The original request was not supposed to have a response.
                        }
                        if (returnsStatusArray(req)) {  // Some requests return a message for each motor
                            var nmsgs = nbytes / STATUS_RESPONSE_LENGTH
                            if (nmsgs > wrapper.responseCount) nmsgs = wrapper.responseCount
                            nbytes = nmsgs * STATUS_RESPONSE_LENGTH
                            if (nbytes < bytes.size) {
                                bytes = truncateByteArray(bytes, nbytes)
                            }
                            val propertyName: String = req.getProperty(PropertyType.PROPERTY_NAME, "NONE")
                            val map = updateStatusFromBytes(propertyName, bytes)
                            for (key in map.keys) {
                                val param = map[key]
                                val name: String = configurationsById[key].getJoint().name()
                                req.setJointValue(name, param)
                                wrapper!!.decrementResponseCount()
                                LOGGER.info(
                                    String.format("%s(%s).serialEvent: received %s (%d remaining) = %s",
                                        CLSS, controllerName, name, wrapper.responseCount, param
                                    )
                                )
                            }
                        }
                        if (wrapper.responseCount <= 0) {
                            responseQueue.removeFirst()
                            if (isSingleControllerRequest(req)) {
                                updateRequestFromBytes(req, bytes)
                                motorManager.handleSingleControllerResponse(req)
                            }
                            else {
                                motorManager.handleAggregatedResponse(req)
                            }
                        }
                    }
                }
                catch (ex: SerialPortException) {
                    System.out.println(ex)
                }
            }
        }
    }

    private fun configurationsForLimb(limb: Limb): Map<String, MotorConfiguration> {
        val result: MutableMap<String, MotorConfiguration> = HashMap<String, MotorConfiguration>()
        for (mc in configurationsByJoint.values) {
            if (mc.limb.equals(limb)) {
                result[mc.joint.name] = mc
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
        System.arraycopy(remainder!!, 0, combination, 0, remainder!!.size)
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
    private fun truncateByteArray(bytes: ByteArray, var nbytes: Int): ByteArray? {
        if (nbytes > bytes.size) nbytes = bytes.size
        if (nbytes == bytes.size) return bytes
        val copy = ByteArray(nbytes)
        System.arraycopy(bytes, 0, copy, 0, nbytes)
        remainder = null
        return copy
    }

    private val CLSS = "MotorController"
    private val LOGGER = Logger.getLogger(CLSS)
    private val BAUD_RATE = 1000000
    private val MIN_WRITE_INTERVAL = 100   // msecs between writes (50 was too short)
    private val STATUS_RESPONSE_LENGTH = 8 // byte count
    override var controllerName = CLSS

    init {
        port = p
        controllerName = String.format("%s:%s",CLSS,port.portName)
        motorManager = parent
        lock = ReentrantLock()
        condition = lock.newCondition()
        configurationsById = HashMap<Int, MotorConfiguration>()
        configurationsByJoint = HashMap<Joint, MotorConfiguration>()
        requestQueue = LinkedList<MessageBottle>()
        responseQueue = LinkedList<MessageWrapper>()
        timeOfLastWrite = System.nanoTime() / 1000000
        running = false
    }
}