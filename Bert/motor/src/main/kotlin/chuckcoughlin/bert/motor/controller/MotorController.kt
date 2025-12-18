/**
 * Copyright 2019-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.motor.dynamixel.DxlMessage
import chuckcoughlin.bert.motor.dynamixel.DxlMessage.LOGGER
import chuckcoughlin.bert.sql.db.Database
import chuckcoughlin.bert.sql.db.SQLConstants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    private val gson: Gson
    private val configurationsByJoint: MutableMap<Joint, MotorConfiguration>
    private var parentRequestChannel = req
    private var parentResponseChannel = rsp
    private val requestQueue: Channel<MessageBottle>   // To serial responder
    private val responseQueue: Channel<MessageBottle>  // From SerialResponder
    private var responseCount: Int
    // The pending message allows the serial callback to accumulate results for the same message
    private val responder: SerialResponder
    private var timeOfLastWrite: Long // To space out serial writes


    /**
     * Motors are assigned to controller by the RobotModel on startup
     */
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
            //LOGGER.info(String.format("%s.execute: Initialized port %s)", CLSS, port.getPortName()))
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
        if(DEBUG) {
            if (request.type == RequestType.COMMAND)
                LOGGER.info(String.format("%s.processRequest:%s processing %s (%s)",  CLSS,
                    controllerName,request.type.name,request.command.name))
            else if (request.type == RequestType.JSON)
                LOGGER.info(String.format("%s.processRequest:%s processing %s (%s)", CLSS,
                    controllerName,request.type.name,request.jtype.name))
            else
                LOGGER.info(String.format("%s.processRequest:%s processing %s",CLSS,
                    controllerName, request.type.name))
        }
        // Do nothing if the joint or limb isn't on this controller
        // -- a limb is on one side or the other, not both
        if( isSingleControllerRequest(request) ) {
            val joint: Joint=request.joint
            val limb: Limb=request.limb
            if( joint!=Joint.NONE ) {
                if( configurationsByJoint[joint]==null ) return
            }
            else if( limb!=Limb.NONE) {
                val count=configurationsForLimb(limb).size
                if(count == 0) { return }
            }
        }

        // Make sure that there is MIN_WRITE_INTERVAL between writes
        val now = System.currentTimeMillis()
        if( now - timeOfLastWrite < ConfigurationConstants.MIN_SERIAL_WRITE_INTERVAL ) {
            delay(ConfigurationConstants.MIN_SERIAL_WRITE_INTERVAL - (now - timeOfLastWrite))
            timeOfLastWrite = System.currentTimeMillis()
        }
        // Get the intended response count - zero if controller is unknown
        var responseCount: Int
        // Now construct the byte array to write to the port. Set response count
        if( isSingleWriteRequest(request) ) {
            val bytes = messageToBytes(request)
            responseCount=request.control.getResponseCountForController(controllerName)
            if (bytes != null) {
                if(responseCount > 0) {
                    requestQueue.send(request)
                }
                writeBytesToSerial(bytes)
                if(DEBUG) LOGGER.info(String.format("%s.processRequest: %s wrote %d bytes (rsp count=%d) contents =\n%s", CLSS, controllerName,
                    bytes.size,responseCount,DxlMessage.dump(bytes)))
            }
        }
        else {       // Multiple write requests. Set total response count
            val byteArrayList = messageToByteList(request)
            responseCount = request.control.getResponseCountForController(controllerName)
            if (responseCount > 0) {
                requestQueue.send(request)
            }
            for (bytes in byteArrayList) {
                delay(ConfigurationConstants.MIN_SERIAL_WRITE_INTERVAL)    // This could be significant
                writeBytesToSerial(bytes)
                if(DEBUG) LOGGER.info(String.format("%s.processRequest: %s wrote %d bytes (rsp count=%d) contents =\n%s",CLSS,
                        controllerName,bytes.size,responseCount,DxlMessage.dump(bytes)))
                }
        }
        if( responseCount>0 ) {
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
     * NOTE: Require same logic in MotorController, MotorGroupController and SerialResponder
     * @param msg the request
     * @return true if this is the type of request satisfied by a single controller.
     */
    private fun isSingleControllerRequest(msg: MessageBottle): Boolean {
        if( msg.type==RequestType.GET_MOTOR_PROPERTY ||
            msg.type==RequestType.SET_LIMB_PROPERTY  ) {
            return true
        }
        else if( msg.type==RequestType.READ_MOTOR_PROPERTY  ||
                 msg.type==RequestType.SET_MOTOR_PROPERTY ) {
            if( msg.joint==Joint.NONE &&      // Applies to all joints
                msg.limb==Limb.NONE      ) {
                return false
            }
            return true
        }
        else if( msg.type==RequestType.JSON ) {
            if( msg.jtype==JsonType.MOTOR_GOALS ||
                msg.jtype==JsonType.MOTOR_LIMITS ) {
                return true
            }
        }
        return false
    }


    /**
     * The list of NOT single requests here should match the request types in messageToByteList().
     * @param msg the request
     * @return true if this request translates into a single serial message.
     * false implies that an array of serial messages is required.
     */
    private fun isSingleWriteRequest(msg: MessageBottle): Boolean {
        if (msg.type==RequestType.EXECUTE_POSE  ||
            msg.type==RequestType.INITIALIZE_JOINTS  ||
            msg.type==RequestType.READ_MOTOR_PROPERTY ||
            msg.type==RequestType.SET_JOINT_POSITIONS   ) {
               return false
        }
        return true
    }

    /**
     * Convert the request message into a command for the serial port. Set the MotorCOnfigurations
     * from the messsage before calculating the byte array. As a side effect set the number of expected
     * responses. This can vary by request type. These are requests that can be handled with a single write.
     * @param wrapper
     * @return
     */
    @Synchronized
    private fun messageToBytes(request: MessageBottle): ByteArray? {
        var responseCount = 0              // No response, reset if otherwise
        var bytes: ByteArray=ByteArray(0)
        val type: RequestType=request.type
        val jtype: JsonType=request.jtype
        val value = if(request.values.size>0) request.values[0] else Double.NaN
        if(DEBUG) LOGGER.info(String.format("%s.messageToBytes: %s handling %s", CLSS, controllerName, type.name))

        if(type.equals(RequestType.JSON) &&
            jtype==JsonType.MOTOR_GOALS) {
            val joint=request.joint
            for (mc in configurationsByJoint.values) {
                if(mc.joint.equals(joint)) {
                    bytes=DxlMessage.bytesToGetGoals(mc.id)
                    responseCount = 1  // Status message
                    break
                }
            }
        }
        else if(type==RequestType.JSON && jtype==JsonType.MOTOR_LIMITS) {
            val joint=request.joint
            for (mc in configurationsByJoint.values) {
                if(mc.joint.equals(joint)) {
                    bytes=DxlMessage.bytesToGetLimits(mc.id)
                    responseCount = 1  // Status message
                    break
                }
            }
        }
        // Assume a dynamic property
        else if(type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val joint=request.joint
            val mc=RobotModel.motorsByJoint[joint]!!
            if(mc.joint.equals(joint)) {
                bytes=DxlMessage.bytesToGetProperty(mc.id, request.jointDynamicProperty)
                responseCount = 1  // Status message
            }
        }
        // Property value must be the same for every joint in limb
        // Those that don't make sense are trapped by the Dispatcher
        else if(type.equals(RequestType.SET_LIMB_PROPERTY)) {
            val limb=request.limb
            val prop=request.jointDynamicProperty
            // Loop over motor config map, set the property
            val configs=configurationsForLimb(limb)

            for (mc in configs.values) {
                mc.setDynamicProperty(prop, value)
            }
            bytes=DxlMessage.byteArrayToSetProperty(configs, prop) // Returns null if limb not on this controller
            // ASYNC WRITE, no response. Let source set text.
            var enabled=true
            if(value < ConfigurationConstants.ON_VALUE) enabled=false
            request.text=String.format("My %s is %s", Limb.toText(limb),
                    if(enabled) "rigid" else "limp")
        }
        // Handle set torque enable for all joints. ASYNC_WRITE, no response
        // NOTE: When enabling torque, a read is required. This extra request is handled by the InternalController
        else if( request.type.equals(RequestType.SET_MOTOR_PROPERTY)        &&
            request.jointDynamicProperty.equals(JointDynamicProperty.STATE) &&
            request.joint.equals(Joint.NONE)  )   {

            if(DEBUG) LOGGER.info(String.format("%s.messageToBytes: %s setting STATE to %2.0f for all joints" ,
                                    CLSS,controllerName,value))
            var enable = true
            if( value<ConfigurationConstants.ON_VALUE ) enable = false
            if( enable ) request.text = "I am now rigid"
            else         request.text = "I am relaxed"

            for (mc in configurationsByJoint.values) {
                mc.isTorquePending = enable
            }
            bytes = DxlMessage.byteArrayToSetProperty(configurationsByJoint,JointDynamicProperty.STATE)
        }
        // Handle set speed for all joints. The speed is specified as a fraction of max.
        else if( request.type.equals(RequestType.SET_MOTOR_PROPERTY)        &&
            request.jointDynamicProperty.equals(JointDynamicProperty.SPEED) &&
            request.joint.equals(Joint.NONE)  )   {

            if(DEBUG) LOGGER.info(String.format("%s.messageToBytes: %s setting speed to %2.0f percent for all joints" ,
                CLSS,controllerName,100.0*value))

            request.text = String.format("all motion will be %2.0f percent of maximum speed", 100.0*value)
            for (mc in configurationsByJoint.values) {
                mc.goalSpeed = mc.maxSpeed*value
            }
            bytes = DxlMessage.byteArrayToSetProperty(configurationsByJoint,JointDynamicProperty.SPEED)
        }
        // Handle set torque for all joints
        // NOTE: torque is specified as a fraction of max
        else if( request.type.equals(RequestType.SET_MOTOR_PROPERTY)        &&
            request.jointDynamicProperty.equals(JointDynamicProperty.TORQUE) &&
            request.joint.equals(Joint.NONE)  )   {

            if(DEBUG) LOGGER.info(String.format("%s.messageToBytes: %s setting torque to %2.0f percent for all joints" ,
                CLSS,controllerName,100.0*value))
            request.text = String.format("All motors are set to %2.0f percent of maximum torque", 100.0*value)
            for (mc in configurationsByJoint.values) {
                mc.goalTorque = mc.maxTorque*value
            }
            bytes = DxlMessage.byteArrayToSetProperty(configurationsByJoint,JointDynamicProperty.TORQUE)
        }
        else if( request.type.equals(RequestType.SET_MOTOR_PROPERTY)        &&
            request.joint.equals(Joint.NONE)  )   {
            request.error = String.format("Setting %s for all motors is not allowed", request.jointDynamicProperty.name)
        }
        // Set the requested property for the single specified joint
        else if (type.equals(RequestType.SET_MOTOR_PROPERTY)) {
            val joint = request.joint
            val prop = request.jointDynamicProperty
            val mc = RobotModel.motorsByJoint[joint]!!

            // Note that this text is over-ridden with the response to the request.
            if (prop.equals(JointDynamicProperty.ANGLE) ) {
                mc.goalAngle = value
                request.text = String.format("Setting my %s to %.0f degrees", Joint.toText(mc.joint),value)
            }
            else if (prop.equals(JointDynamicProperty.SPEED) ) {
                mc.goalSpeed = value
                request.text = String.format("Setting my %s speed to %2.0f degrees per second", Joint.toText(mc.joint),value)
            }
            else if (prop.equals(JointDynamicProperty.STATE) ) {
                var enabled = false
                if(value>ConfigurationConstants.OFF_VALUE) enabled = true
                request.text = String.format("Setting my %s %s", Joint.toText(mc.joint),
                    if(enabled) "rigid" else "relaxed")
                mc.isTorquePending = enabled
            }
            else if (prop.equals(JointDynamicProperty.TORQUE) ) {
                mc.goalTorque = value
                request.text = String.format("Setting my %s torque to %2.2f newton meters", Joint.toText(mc.joint),value)
            }
            else if (prop.equals(JointDynamicProperty.RANGE) ) {
                request.error = String.format("%s minimum and maximum angles must be set separately", Joint.toText(mc.joint))
            }
            else {
                request.error = String.format("I failed to set my %s %s to %2.0f",
                        Joint.toText(mc.joint), prop.name, value)
            }
            bytes = DxlMessage.bytesToSetProperty(mc, prop, value)
            responseCount = 1  // Status message
        }
        else if (type.equals(RequestType.NONE)) {
            LOGGER.warning(String.format("%s.messageToBytes: Empty request - ignored (%s)",
                CLSS, type.name ) )
        }
        else {
            LOGGER.severe(String.format("%s.messageToBytes: %s unhandled request %s",
                CLSS, controllerName,type.name))
        }
        runBlocking{request.control.setResponseCountForController(controllerName,responseCount)}
        LOGGER.info(String.format("%s.messageToBytes: %s expect %d responses %d bytes",CLSS,
            controllerName,responseCount,bytes.size))
        return bytes
    }
    /**
     * Convert the request message into a list of commands for the serial port. As a side
     * effect set the number of expected responses. This can vary by request type (and may
     * be none). Set the number of responses for the request.
     * @param wrapper
     * @return
     */
    @Synchronized
    private fun messageToByteList(request: MessageBottle): List<ByteArray> {
        var responseCount = 0         // No response unless otherwise set
        var list: List<ByteArray> = mutableListOf<ByteArray>()
        val type: RequestType = request.type
        val limb: Limb = request.limb
        val value = if(request.values.size>0) request.values[0] else Double.NaN
        if(DEBUG) LOGGER.info(String.format("%s.messageToByteList: %s handling %s on %s",
                    CLSS,controllerName,type.name,limb.name))

        if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {   // Range is the only legal one for list
            val mc = RobotModel.motorsByJoint[request.joint]!!
            list = ArrayList<ByteArray>()
            list.add(DxlMessage.bytesToGetProperty(mc.id,JointDynamicProperty.MINIMUMANGLE))
            list.add(DxlMessage.bytesToGetProperty(mc.id,JointDynamicProperty.MAXIMUMANGLE))
            responseCount = 2 // Status messages
        }
        else if(type.equals(RequestType.INITIALIZE_JOINTS)) {
            list = DxlMessage.byteArrayListToInitializePositions(configurationsByJoint)
            for (mc in configurationsByJoint.values) {
                mc.goalTorque = ConfigurationConstants.FULL_TORQUE*mc.maxTorque
            }
        }
        // One motor, all motors or all on a limb - one response per motor
        else if(type.equals(RequestType.READ_MOTOR_PROPERTY)) {
            val joint = request.joint
            val prop = request.jointDynamicProperty

            if( limb==Limb.NONE ) {
                if( joint==Joint.NONE ) {      // All joints - (MX12 not returned in list)
                    list = DxlMessage.byteArrayListToListProperty(prop, configurationsByJoint.values)
                    responseCount = configurationsByJoint.size
                }
                else {
                    val mc = configurationsByJoint[joint]
                    val lst = mutableListOf<MotorConfiguration>()
                    if(mc!=null ) {
                        lst.add(mc)
                        list = DxlMessage.byteArrayListToListProperty(prop, lst)
                        responseCount = 1
                    }
                    else {
                        LOGGER.severe(String.format("%s.messageToByteList: no motor configuration for %s on %s",
                                            CLSS,joint.name,controllerName))
                    }
                }
            }
            else {
                val configs: Map<Joint, MotorConfiguration> = configurationsForLimb(limb)
                list = DxlMessage.byteArrayListToListProperty(prop, configs.values)
                responseCount = configs.size
            }
        }
        else if( type.equals(RequestType.EXECUTE_POSE) ) {
            responseCount = 0 // AYNC WRITE, no responses
            val poseName: String = request.arg
            val index: Int = value.toInt()
            val poseid = Database.getPoseIdForName(poseName, index)
            // If the pose doesn't exist, just return an empty list
            if (poseid != SQLConstants.NO_POSE) {
                val configurations = Database.setMotorConfigurationsForPose(poseid,controllerName)
                if (configurations.size > 0) {
                    LOGGER.info(String.format("%s.messageToByteList (%s): set pose %s %d on %s with %d joints",
                        CLSS, controllerName, poseName, index, controllerName, configurations.size))
                    list = DxlMessage.byteArrayListToSetPose(poseid, configurations)
                }
            }
        }
        // Property value must be the same for every joint in limb
        // Those that don't make sense are trapped by the Dispatcher
        else if(type.equals(RequestType.SET_JOINT_POSITIONS)) {
            val propType = object : TypeToken<List<JointPosition>>() {}.type
            val positions = gson.fromJson<List<JointPosition>>(request.text,propType)
            val configurations = mutableListOf<MotorConfiguration>()
            for(pos in positions ) {
                val joint = Joint.fromString(pos.name)
                if( joint!=Joint.NONE ) {
                    val mc = RobotModel.motorsByJoint[joint]
                    mc!!.angle = pos.home    // This only works for "straighten"
                    configurations.add(mc)
                }
            }
            if (configurations.size > 0) {
                LOGGER.info(String.format("%s.messageToByteList (%s): set positions for %d joints",
                    CLSS, controllerName, configurations.size))
                list = DxlMessage.byteArrayListToSetPositions(configurations)
            }
            request.text=String.format("Positions are set")
        }
        else {
            LOGGER.severe(String.format("%s.messageToByteList: Unhandled message type %s",
                CLSS,type.name))
        }
        if(DEBUG) {
            var count = 1
            for (bytes in list) {
                LOGGER.info(String.format( "%s.messageToByteList: %s %s %d = \n%s",
                    CLSS,controllerName,request.type,count,DxlMessage.dump(bytes)))
                count = count + 1
            }
        }
        runBlocking{request.control.setResponseCountForController(controllerName,responseCount)}
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
        runBlocking{msg.control.setResponseCountForController(controllerName,responseCount)}   // No serial response to wait for ...
        if (msg.type==RequestType.INITIALIZE_JOINTS ||
            msg.type==RequestType.SET_LIMB_PROPERTY ||
            msg.type==RequestType.EXECUTE_POSE ) {
            msg.text = ""   // Random acknowledgement added later
        }
        else if(msg.type.equals(RequestType.SET_MOTOR_PROPERTY) ) {
            // Return message is added by normal command processing
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
        responder.reset()
    }

    private val CLSS = "MotorController"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    private val BAUD_RATE = 1000000

    override val controllerType = ControllerType.MOTOR

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_MOTOR)
        port = p
        requestQueue  = Channel(1)
        responseQueue = Channel(1)
        responseCount = 1
        responder = SerialResponder(name,requestQueue,responseQueue)
        configurationsByJoint = mutableMapOf<Joint, MotorConfiguration>()
        timeOfLastWrite = System.currentTimeMillis()
        running = false
        job = Job()
        gson = Gson()
        LOGGER.info(String.format("%s.init: created %s...", CLSS,name))
    }
}