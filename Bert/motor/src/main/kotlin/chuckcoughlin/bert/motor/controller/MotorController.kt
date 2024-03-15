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
import jssc.SerialPortEvent
import jssc.SerialPortEventListener
import jssc.SerialPortException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
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
 * @param p - the serial port shared by the motors under control
 * @param req - channel for requests from the parent (motor manager)
 * @param rsp - channel for responses sent to the parent (motor manager)
 */
@Suppress("UNUSED_PARAMETER")
class MotorController(name:String,p:SerialPort,req: Channel<MessageBottle>,rsp:Channel<MessageBottle>) : Controller,SerialPortEventListener {
    @DelicateCoroutinesApi
    private val scope = GlobalScope // For long-running coroutines
    override val controllerName = name
    private var port: SerialPort
    private var running:Boolean
    private var job: Job
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
            : LinkedList<MessageBottle>
    private var timeOfLastWrite: Long

    val configurations: MutableCollection<MotorConfiguration>
        get() = configurationsByJoint.values

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
                    port.addEventListener(this)
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
        // Port is open, now use it.
        running = true
        job = scope.launch(Dispatchers.IO) {
            while (running) {
                select<Unit> {
                    /**
                     * On receipt of a message from the SerialPort,
                     * decypher and forward to the MotorManager.
                     */
                    receiveSerialResponse().onAwait{}
                    /**
                     * The parent request is a motor command. Convert it
                     * into a message for the SerialPort amd write.
                     */
                    parentRequestChannel.onReceive() { // parent request
                        processRequest(it)
                    }
                }
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
     * This method blocks until any prior request completes. Ignore requests that apply to a single controller
     * and that controller is not this one, otherwise add the request to the request queue.
     * @param request
     */
     suspend fun processRequest(request:MessageBottle) {
            lock.lock()
            LOGGER.info(String.format("%s.processRequest:%s processing %s",CLSS,controllerName,request.type.name));
            try {
                if(isLocalRequest(request)) {
                    handleLocalRequest(request)
                }
                else if(isSingleControllerRequest(request)) {
                    LOGGER.info(String.format("%s.processRequest: %s single-controller request (%s)", CLSS, controllerName, request.type.name))
                    // Do nothing if the joint or limb isn't in our controllerName.
                    val joint: Joint=request.joint
                    val cName: String=request.control.controller
                    val limb: Limb=request.limb
                    if(!joint.equals(Joint.NONE)) {
                        val mc: MotorConfiguration=configurationsByJoint[joint] ?: return
                    }
                    else if(!cName.isEmpty()) {
                        if(!cName.equals(controllerName, ignoreCase=true)) {
                            return
                        }
                    }
                    else if(!limb.equals(Limb.NONE)) {
                        val count=configurationsForLimb(limb).size
                        if(count == 0) {
                            return
                        }
                    }
                    else {
                        val property: JointDynamicProperty=request.jointDynamicProperty
                        LOGGER.info(String.format("%s.processRequest: %s %s (%s)",
                                CLSS, controllerName, request.type.name, property.name))
                    }
                }
                else {
                    LOGGER.info(String.format("%s.processRequest: %s multi-controller request (%s)",
                            CLSS, controllerName, request.type.name))
                }
                requestQueue.addLast(request)
                LOGGER.info(String.format("%s.processRequest: %s added to request queue %s",CLSS,controllerName,request.type.name));
                condition.signal()
            }
            finally {
                lock.unlock()
            }

    }

    /**
     * Wait until a request message is ready on the queue. Convert to serial request, write to port.
     * From there a listener forwards the responses to the group controller (a MotorManager).
     * Do not free the request lock until we have the response in-hand.
     *
     * Integer.toHexString(this.hashCode())
     */
     fun receiveSerialResponse() : Deferred<Unit> =
        GlobalScope.async(Dispatchers.IO) {
            lock.lock()
            try {
                condition.await()
                LOGGER.info(String.format("%s.receiveSerialResponse:  %s got signal for message, writing to %s",CLSS,controllerName,port.getPortName()));
                val req: MessageBottle = requestQueue.removeFirst() // Oldest
                if (isSingleWriteRequest(req)) {
                    val bytes = messageToBytes(req)
                    if (bytes != null) {
                        if (req.control.responseCount > 0) {
                            responseQueue.addLast(req)
                        }
                        writeBytesToSerial(bytes)
                        LOGGER.info(String.format("%s.receiveSerialResponse: %s wrote %d bytes", CLSS, controllerName, bytes.size))
                    }
                }
                else {
                    val byteArrayList = messageToByteList(req)
                    if (req.control.responseCount > 0) {
                        responseQueue.addLast(req)
                    }
                    for (bytes in byteArrayList) {
                        writeBytesToSerial(bytes)
                        LOGGER.info(String.format("%s.receiveSerialResponse: %s wrote %d bytes", CLSS, controllerName, bytes.size))
                    }
                }
                if (req.control.responseCount == 0) {
                    synthesizeResponse(req)
                }
            }
            catch (ie: InterruptedException) { }
            finally {
                lock.unlock()
            }
    }

    // ============================= Private Helper Methods =============================
    // Create a response for a request that can be handled immediately. There aren't many of them.
    private suspend fun handleLocalRequest(request: MessageBottle) {
        // The following two requests simply use the current positions of the motors, whatever they are
        if (request.type.equals(RequestType.COMMAND)) {
            val command: CommandType = request.command
            LOGGER.warning(String.format("%s.handleLocalRequest: %s command=%s",
                CLSS,controllerName,command.name) )
            if (command.equals(CommandType.RESET)) {
                remainder = null // Resync after dropped messages.
                responseQueue.clear()
            }
            else {
                val msg = String.format("my motor controller cannot handle a %s command", command)
                request.error = msg
            }
            parentResponseChannel.send(request)
        }
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
            (!msg.control.controller.isEmpty() || !msg.limb.equals(Limb.NONE))) {
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
            msg.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
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
        return if(  msg.type.equals(RequestType.GET_MOTOR_PROPERTY) ||
                    msg.type.equals(RequestType.LIST_MOTOR_PROPERTY) ||
                    msg.type.equals(RequestType.READ_MOTOR_PROPERTY)) {
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
    private fun messageToBytes(request: MessageBottle): ByteArray? {
        request.control.responseCount = 0 // No response, unless specified otherwise
        var bytes: ByteArray  = ByteArray(0)
        val type: RequestType = request.type
        if(DEBUG) LOGGER.info(String.format("%s.messageToBytes: %s handling %s",
            CLSS,controllerName,type.name))
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
                    request.control.responseCount = 1 // Status message
                    break
                }
            }
        }
        else if (type.equals(RequestType.GET_LIMITS)) {
            val joint = request.joint
            for (mc in configurationsByJoint.values) {
                if (mc.joint.equals(joint)) {
                    bytes = DxlMessage.bytesToGetLimits(mc.id)
                    request.control.responseCount = 1 // Status message
                    break
                }
            }
        }
        // Assume a dynamic property
        else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val joint = request.joint
            for (mc in configurationsByJoint.values) {
                if (mc.joint.equals(joint)) {
                    bytes = DxlMessage.bytesToGetLimits(mc.id)
                    request.control.responseCount = 1 // Status message
                    break
                }
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
            val propertyValues = request.getJointValueIterator()
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
                    request.control.responseCount = 1 // Status message
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
    private fun messageToByteList(request: MessageBottle): List<ByteArray> {
        var list: List<ByteArray> = ArrayList()
        val type: RequestType = request.type
        if(DEBUG) LOGGER.info(String.format("%s.messageToByteList: %s handling %s",
            CLSS,controllerName,type.name))
        if (type.equals(RequestType.INITIALIZE_JOINTS)) {
            list = DxlMessage.byteArrayListToInitializePositions(configurationsByJoint)
            val duration: Long = DxlMessage.mostRecentTravelTime
            if (request.duration < duration) request.duration = duration
            request.control.responseCount = 0 // No response
        }
        else if( type==RequestType.LIST_MOTOR_PROPERTY ||
                 type==RequestType.READ_MOTOR_PROPERTY ) {
            val limb = request.limb
            val prop = request.jointDynamicProperty
            if( limb.equals(Limb.NONE) ) {
                list = DxlMessage.byteArrayListToListProperty(prop, configurationsByJoint.values)
                request.control.responseCount = configurationsByJoint.size // Status packet for each motor
            }
            else {
                val configs: Map<Joint, MotorConfiguration> = configurationsForLimb(limb)
                list = DxlMessage.byteArrayListToListProperty(prop, configs.values)
                request.control.responseCount = configs.size // Status packet for each motor in limb
            }
        }
        else if (type==RequestType.SET_POSE) {
            val poseName: String = request.pose
            list = DxlMessage.byteArrayListToSetPose(configurationsByJoint, poseName)
            val duration: Long = DxlMessage.mostRecentTravelTime
            if (request.duration < duration) request.duration = duration
            request.control.responseCount = 0 // AYNC WRITE, no responses
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
    private suspend fun synthesizeResponse(msg: MessageBottle) {
        if (msg.type.equals(RequestType.INITIALIZE_JOINTS) ||
            msg.type.equals(RequestType.SET_LIMB_PROPERTY) ||
            msg.type.equals(RequestType.SET_POSE) ) {
            msg.text = ""   // Random acknowledgement added later
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
        parentResponseChannel.send(msg)
    }

    // We update the properties in the request from our serial message.
    // The properties must include motor type and orientation
    private fun updateRequestFromBytes(request: MessageBottle, bytes: ByteArray) {
        val type: RequestType = request.type
        if (type.equals(RequestType.GET_GOALS)) {
            val joint = request.joint
            val mc: MotorConfiguration? = getMotorConfiguration(joint)
            DxlMessage.updateGoalsFromBytes(mc!!, request, bytes)
        }
        else if (type.equals(RequestType.GET_LIMITS)) {
            val joint = request.joint
            val mc: MotorConfiguration? = getMotorConfiguration(joint)
            DxlMessage.updateLimitsFromBytes(mc!!, request, bytes)
        }
        else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val joint = request.joint
            val mc: MotorConfiguration? = getMotorConfiguration(joint)
            val property = request.jointDynamicProperty
            DxlMessage.updateParameterFromBytes(property, mc!!, request, bytes)
            val partial = request.text
            if (partial.isNotEmpty()) {
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
        val props: MutableMap<Int, String> = HashMap()
        DxlMessage.updateParameterArrayFromBytes(property, configurationsById, bytes, props)
        return props
    }

    /*
	 * Guarantee that consecutive writes won't be closer than MIN_WRITE_INTERVAL.
	 * Use of serial ports must be configured on command line.
	 */
    private fun writeBytesToSerial(bytes: ByteArray) {
        if( RobotModel.useSerial and (bytes.size>0) ) {
            try {
                val now = System.currentTimeMillis()
                val interval = now - timeOfLastWrite
                if (interval < MIN_WRITE_INTERVAL) {
                    Thread.sleep(MIN_WRITE_INTERVAL - interval)
                    //LOGGER.info(String.format("%s(%s).writeBytesToSerial: Slept %d msecs",CLSS,controllerName,MIN_WRITE_INTERVAL-interval));
                }
                LOGGER.info(String.format("%s(%s).writeBytesToSerial: Write interval %d msecs",
                    CLSS, controllerName,interval))
                val success: Boolean = port.writeBytes(bytes)
                timeOfLastWrite = System.currentTimeMillis()
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
     * Handle the response from the serial request. Note that all our interactions removing from the
     * responseQueue and dealing with remainder are synchronized here.
     *
     * Unless an error is returned, the response queue must have at least one response for associating results.
     */
    override fun serialEvent(event: SerialPortEvent) {
        LOGGER.info(String.format("%s(%s).serialEvent queue is %d", CLSS, controllerName, responseQueue.size))
        if (event.isRXCHAR()) {
            var req: MessageBottle = MessageBottle(RequestType.NONE)
            if (!responseQueue.isEmpty()) {
                req = responseQueue.getFirst()
            }
            // The value is the number of bytes in the read buffer
            val byteCount: Int = event.getEventValue()
            LOGGER.info(String.format("%s.serialEvent %s: expect %d %s msgs got %d bytes",
                CLSS,controllerName,req.control.responseCount,req.type.name, byteCount) )
            if (byteCount > 0) {
                try {
                    var bytes: ByteArray = port.readBytes(byteCount)
                    bytes = prependRemainder(bytes)
                    bytes = DxlMessage.ensureLegalStart(bytes) // never null
                    var nbytes = bytes.size
                    LOGGER.info(String.format("%s(%s).serialEvent: read =\n%s",
                        CLSS,controllerName, DxlMessage.dump(bytes)))
                    val mlen: Int = DxlMessage.getMessageLength(bytes) // First message
                    if (mlen < 0 || nbytes < mlen) {
                        LOGGER.info( String.format("%s(%s).serialEvent Message too short (%d), requires additional read",
                            CLSS,controllerName,nbytes))
                        return
                    }
                    else if (DxlMessage.errorMessageFromStatus(bytes).isNotBlank()) {
                        LOGGER.severe( String.format("%s(%s).serialEvent: ERROR: %s",
                            CLSS,DxlMessage.errorMessageFromStatus(bytes)) )
                        if (req.type.equals(RequestType.NONE)) return  // The original request was not supposed to have a response.
                    }
                    if(returnsStatusArray(req)) {  // Some requests return a message for each motor
                        var nmsgs = nbytes / STATUS_RESPONSE_LENGTH
                        if (nmsgs > req.control.responseCount) nmsgs = req.control.responseCount
                        nbytes = nmsgs * STATUS_RESPONSE_LENGTH
                        if (nbytes < bytes.size) {
                            bytes = truncateByteArray(bytes, nbytes)
                        }
                        val prop= req.jointDynamicProperty
                        val map = updateStatusFromBytes(prop, bytes)
                        for (key in map.keys) {
                            val param = map[key]
                            val joint = configurationsById[key]!!.joint
                            req.addJointValue(joint,prop, param!!.toDouble())
                            req.control.responseCount = req.control.responseCount - 1
                            LOGGER.info(
                                String.format("%s(%s).serialEvent: received %s (%d remaining) = %s",
                                    CLSS, controllerName, prop.name, req.control.responseCount, param) )
                        }
                    }
                    if (req.control.responseCount <= 0) {
                        responseQueue.removeFirst()
                        if (isSingleControllerRequest(req)) {
                            updateRequestFromBytes(req, bytes)
                            runBlocking {
                                parentResponseChannel.send(req)
                            }
                        }
                        else {
                            runBlocking {
                                parentResponseChannel.send(req)
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

    private fun configurationsForLimb(limb: Limb): Map<Joint, MotorConfiguration> {
        val result: MutableMap<Joint, MotorConfiguration> = HashMap<Joint, MotorConfiguration>()
        for (mc in configurationsByJoint.values) {
            if (mc.limb.equals(limb)) {
                result[mc.joint] = mc
            }
        }
        return result
    }

    /**
     * Combine the remainder from the previous serial read. Set the remainder null.
     * @param bytes
     * @return
     */
    private fun prependRemainder(bytes: ByteArray): ByteArray {
        if(remainder == null) return bytes
        val combination = ByteArray(remainder!!.size + bytes.size)
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
    private fun truncateByteArray(bytes: ByteArray, nb: Int): ByteArray {
        var nbytes = nb
        if (nbytes > bytes.size) nbytes = bytes.size
        if (nbytes == bytes.size) return bytes
        val copy = ByteArray(nbytes)
        System.arraycopy(bytes, 0, copy, 0, nbytes)
        remainder = null
        return copy
    }

    private val CLSS = "MotorController"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    private val BAUD_RATE = 1000000
    private val MIN_WRITE_INTERVAL = 100   // msecs between writes (50 was too short)
    private val STATUS_RESPONSE_LENGTH = 8 // byte count

    override val controllerType = ControllerType.MOTOR

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_MOTOR)
        port = p
        lock = ReentrantLock()
        condition = lock.newCondition()
        configurationsById = HashMap<Int, MotorConfiguration>()
        configurationsByJoint = HashMap<Joint, MotorConfiguration>()
        requestQueue = LinkedList<MessageBottle>()
        responseQueue = LinkedList<MessageBottle>()
        timeOfLastWrite = System.currentTimeMillis()
        running = false
        job = Job()
        LOGGER.info(String.format("%s.init: created...", CLSS))
    }
}