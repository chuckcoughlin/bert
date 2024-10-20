/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.motor.dynamixel.DxlMessage
import jssc.SerialPortEvent
import jssc.SerialPortEventListener
import jssc.SerialPortException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.logging.Logger

/**
 * Handle callbacks from the serial port. Use channels to receive the original
 * request message and to transmit a response.
 *
 * Since the same request object is processed in parallel by multiple MotorControl
 * and SerialResponder objects, it is imperative that any object updates be synchronized.
 *
 * @param name - the serial port name used to identify the instance
 * @param req - channel for requests from the parent motor controller
 * @param rsp - channel for responses sent to the parent motor controller
 */
class SerialResponder(nam:String,req: Channel<MessageBottle>,rsp:Channel<MessageBottle>) : SerialPortEventListener {
    private val name = nam
    private var inChannel = req
    private var outChannel = rsp
    private var remainder: ByteArray? = null
    // The pending message allows the serial callback to accumulate results for the same message
    private var pending : MessageBottle? = null

    // ============================== SerialPortEventListener ===============================
    /**
     * Handle the response from the serial request. NOTE: Since multiple serialEvents
     * are updating the same request, any updates must be synchronized.
     */
    @Synchronized
    override fun serialEvent(event: SerialPortEvent) {
        LOGGER.info(String.format("%s.serialEvent for %s (pending %s)", CLSS, name,if(pending==null) "none" else "yes"))
        if (event.isRXCHAR()) {
            var request =
                if(pending==null) runBlocking(Dispatchers.IO) { inChannel.receive() }
                else pending!!

            // The value is the number of bytes in the read buffer
            val byteCount: Int = event.getEventValue()
            LOGGER.info(String.format("%s.serialEvent %s: expect %d %s msgs got %d bytes",
                    CLSS,name,request.control.responseCount[name]!!,request.type.name, byteCount) )
            if (byteCount > 0) {
                try {
                    var bytes: ByteArray = event.port.readBytes(byteCount)
                    bytes = prependRemainder(bytes)
                    bytes = DxlMessage.ensureLegalStart(bytes) // never null
                    var nbytes = bytes.size
                    LOGGER.info(String.format("%s(%s).serialEvent: contents =\n%s",CLSS,name, DxlMessage.dump(bytes)))
                    val mlen: Int = DxlMessage.getMessageLength(bytes) // First message
                    if (mlen < 0 || nbytes < mlen) {
                        LOGGER.warning( String.format("%s(%s).serialEvent Message too short (%d), requires additional read",
                                CLSS,name,nbytes))
                        return
                    }
                    else if (DxlMessage.errorMessageFromStatus(bytes).isNotBlank()) {
                        LOGGER.severe( String.format("%s.serialEvent: %s ERROR: %s",
                                CLSS,name,DxlMessage.errorMessageFromStatus(bytes)) )
                        if (request.type.equals(RequestType.NONE)) return  // The original request was not supposed to have a response.
                    }
                    if(returnsStatusArray(request)) {  // Some requests return a message for each motor
                        var nmsgs = nbytes / STATUS_RESPONSE_LENGTH
                        if (nmsgs > request.control.responseCount[name]!!) nmsgs = request.control.responseCount[name]!!
                        nbytes = nmsgs * STATUS_RESPONSE_LENGTH
                        if (nbytes < bytes.size) {
                            bytes = truncateByteArray(bytes, nbytes)
                        }
                        val prop= request.jointDynamicProperty
                        val map = updateStatusFromBytes(prop, bytes)
                        for (key in map.keys) {
                            val param = map[key]
                            val joint = RobotModel.motorsById[key]!!.joint
                            request.addJointValue(joint,prop, param!!.toDouble())
                            request.control.responseCount[name] = request.control.responseCount[name]!! - 1
                            LOGGER.info(String.format("%s.serialEvent: %s received %s (%d remaining) = %s",
                                            CLSS, name, joint.name, request.control.responseCount[name]!!, param) )
                        }
                    }
                    else {
                        request.control.responseCount[name] = request.control.responseCount[name]!! - 1
                    }
                    if (request.control.responseCount[name]!! <= 0) {
                        if (isSingleControllerRequest(request)) {
                            updateRequestFromBytes(request, bytes)
                        }
                        pending = null
                        runBlocking(Dispatchers.IO) { outChannel.send(request) }
                        LOGGER.info(String.format("%s.serialEvent: sent response %s.",CLSS,request.type.name))
                    }
                    else {
                        pending = request
                    }
                }
                catch (ex: SerialPortException) {
                    System.out.println(ex)
                }
            }
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
            msg.type.equals(RequestType.SET_LIMB_PROPERTY) ||
            msg.type.equals(RequestType.SET_MOTOR_PROPERTY) ) {
            return true
        }
        else if (msg.type.equals(RequestType.LIST_MOTOR_PROPERTY) &&
            !msg.limb.equals(Limb.NONE)) {
            return true
        }
        return false
    }

    /**
     * @param msg the request
     * @return true if this is the type of message that returns a separate
     * status response for every motor referenced in the request.
     * (There may be only one).
     */
    private fun returnsStatusArray(msg: MessageBottle): Boolean {
        return if(  msg.type.equals(RequestType.LIST_MOTOR_PROPERTY) ||
                    msg.type.equals(RequestType.READ_MOTOR_PROPERTY)) {
            true
        }
        else {
            false
        }
    }

    // We update the properties in the request from our serial message.
    // The properties must include motor type and orientation
    @Synchronized
    private fun updateRequestFromBytes(request: MessageBottle, bytes: ByteArray) {
        val type: RequestType = request.type
        if (type.equals(RequestType.GET_GOALS)) {
            val joint = request.joint
            val mc: MotorConfiguration? = RobotModel.motorsByJoint[joint]
            DxlMessage.updateGoalsFromBytes(mc!!, request, bytes)
        }
        else if (type.equals(RequestType.GET_LIMITS)) {
            val joint = request.joint
            val mc: MotorConfiguration? = RobotModel.motorsByJoint[joint]
            DxlMessage.updateLimitsFromBytes(mc!!, request, bytes)
        }
        else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val joint = request.joint
            val mc: MotorConfiguration? = RobotModel.motorsByJoint[joint]
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
        DxlMessage.updateParameterArrayFromBytes(property, RobotModel.motorsById, bytes, props)
        return props
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

    private val CLSS = "SerialResponder"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    private val STATUS_RESPONSE_LENGTH = 8 // byte count

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SERIAL)
        LOGGER.info(String.format("%s.init: created...", CLSS))
    }
}