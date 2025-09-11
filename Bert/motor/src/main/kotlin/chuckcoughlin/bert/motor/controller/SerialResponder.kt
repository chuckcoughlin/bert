/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.motor.controller

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.motor.dynamixel.DxlMessage
import jssc.SerialPortEvent
import jssc.SerialPortEventListener
import jssc.SerialPortException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.logging.Logger

/**
 * Handle callbacks from the serial port. Use channels to receive the original
 * request message and to transmit a response.
 *
 * Since the same request object is processed in parallel by multiple MotorControl
 * and SerialResponder objects, it is imperative that any object updates be synchronized.
 *
 * @param nam - the serial port name used to identify the instance
 * @param req - channel for requests from the parent motor controller
 * @param rsp - channel for responses sent to the parent motor controller
 */
class SerialResponder(nam:String,req: Channel<MessageBottle>,rsp:Channel<MessageBottle>) : SerialPortEventListener {
    private val name = nam
    private var inChannel = req
    private var outChannel = rsp
    private var remainder: ByteArray
    // The pending message allows the serial callback to accumulate results for the same message
    private var pending : MessageBottle? = null
    private var mutex: Mutex

    // ============================== SerialPortEventListener ===============================
    /**
     * Handle the response from the serial request. NOTE: Since multiple serialEvents
     * are updating the same request, any updates must be synchronized.
     */
    override fun serialEvent(event: SerialPortEvent) {
        if(DEBUG) LOGGER.info(String.format("%s.serialEvent for %s (pending %s)", CLSS, name,if(pending==null) "none" else "yes"))
        if (event.isRXCHAR()) {
            val request =
                if(pending==null) {
                    runBlocking(Dispatchers.IO) {inChannel.receive()}
                }
                else pending!!

            // The value is the number of bytes in the read buffer
            var responseCount = runBlocking{request.control.getResponseCountForController(name)}
            if(DEBUG) LOGGER.info(String.format("%s.serialEvent %s: expect %d %s msgs got %d bytes",
                CLSS,name,responseCount,request.type.name, event.eventValue) )
            try {
                var bytes = runBlocking{accumulateResults(event)}
                var nbytes = bytes.size
                if(DEBUG) LOGGER.info(String.format("%s(%s).serialEvent: contents =\n%s",CLSS,name, DxlMessage.dump(bytes)))
                val mlen: Int = DxlMessage.getMessageLength(bytes) // First message
                if (mlen < 0 || nbytes < mlen) {
                    LOGGER.warning( String.format("%s(%s).serialEvent Message too short (%d of %d), requires additional read",
                        CLSS,name,nbytes,mlen))
                    return
                }
                else if (DxlMessage.errorMessageFromStatus(bytes).isNotBlank()) {
                    LOGGER.severe( String.format("%s.serialEvent: %s ERROR: %s",
                        CLSS,name,DxlMessage.errorMessageFromStatus(bytes)) )
                    if (request.type.equals(RequestType.NONE)) return  // The original request was not supposed to have a response.
                }
                if(returnsStatusArray(request)){  // Some requests return a message for each motor, e.g. READ_MOTOR_PROPERTY
                    var nmsgs = nbytes / STATUS_RESPONSE_LENGTH
                    if (nmsgs > responseCount) nmsgs = responseCount
                    nbytes = nmsgs * STATUS_RESPONSE_LENGTH
                    if (nbytes < bytes.size) {
                        bytes = runBlocking{truncateByteArray(bytes, nbytes)}
                    }
                    val prop= request.jointDynamicProperty
                    val map = updateMotorStatusFromBytes(prop, bytes)
                    for (key in map.keys) {
                        val param = map[key]
                        val joint = RobotModel.motorsById[key]!!.joint
                        responseCount = runBlocking{request.control.decrementResponseCountForController(name)}
                        if(DEBUG)LOGGER.info(String.format("%s.serialEvent: %s received %s (%d remaining) = %s",
                            CLSS, name, joint.name, responseCount, param) )
                    }
                }
                else {
                    responseCount = runBlocking{request.control.decrementResponseCountForController(name)}
                }

                // No need to worry about concurrency if there is only one controller.
                if (responseCount <= 0) {
                    if(isSingleControllerRequest(request)) {
                        updateRequestFromBytes(request, bytes)
                    }
                    pending = null
                    runBlocking(Dispatchers.IO) { outChannel.send(request) }
                    if(DEBUG) LOGGER.info(String.format("%s.serialEvent: sent response %s.",CLSS,request.type.name))
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

    fun reset() {
        pending = null
    }
    // ============================= Private Helper Methods =============================
    /**
     * NOTE: Require same logic in MotorController, MotorGroupController and SerialResponder
     * @param msg the request
     * @return true if this is the type of request satisfied by a single controller.
     */
    private fun isSingleControllerRequest(msg: MessageBottle): Boolean {
        if( msg.type==RequestType.EXECUTE_ACTION ||
            msg.type==RequestType.GET_MOTOR_PROPERTY ||
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
        else if( msg.type.equals(RequestType.EXECUTE_POSE)   ) {
            if( !msg.limb.equals(Limb.NONE)) {   // Applies to only one limb
                return true
            }
        }
        else if( msg.type.equals(RequestType.JSON) ) {
            if( msg.jtype.equals(JsonType.MOTOR_GOALS) ||
                msg.jtype.equals(JsonType.MOTOR_LIMITS)) {
                return true
            }
        }
        return false
    }

    /**
     * @param msg the request
     * @return true if this is the type of message that returns a separate
     * status response for every motor referenced in the request.
     * (There may be only one). The object is to update the internal
     * motor configuration objects.
     */
    private fun returnsStatusArray(msg: MessageBottle): Boolean {
        if(  msg.type==RequestType.READ_MOTOR_PROPERTY ) {
            return true
        }
        return false
    }

    // We update the properties in the request from our serial message.
    // The properties must include motor type and orientation
    @Synchronized
    private fun updateRequestFromBytes(request: MessageBottle, bytes: ByteArray) {
        val type: RequestType = request.type
        val jtype: JsonType = request.jtype
        if (jtype.equals(JsonType.MOTOR_GOALS)) {
            val joint = request.joint
            val mc: MotorConfiguration = RobotModel.motorsByJoint[joint]!!
            DxlMessage.updateGoalsRequestFromBytes(mc, request, bytes)
        }
        else if (jtype.equals(JsonType.MOTOR_LIMITS)) {
            val joint = request.joint
            val mc: MotorConfiguration = RobotModel.motorsByJoint[joint]!!
            DxlMessage.updateLimitsRequestFromBytes(mc, request, bytes)
        }
        else if (type.equals(RequestType.GET_MOTOR_PROPERTY)) {
            val property = request.jointDynamicProperty
            val joint = request.joint
            val mc: MotorConfiguration = RobotModel.motorsByJoint[joint]!!
            DxlMessage.updatePropertyRequestFromBytes(property,mc,request, bytes)
        }
        // These messages do not get updated with serial status
        else if (type==RequestType.READ_MOTOR_PROPERTY ||
                 type==RequestType.SET_LIMB_PROPERTY ||
                 type==RequestType.SET_MOTOR_PROPERTY ) {
            val err: String = DxlMessage.errorMessageFromStatus(bytes)
            request.error = err
        }
        else {
            LOGGER.severe(String.format("%s.updateRequestFromBytes: Unhandled response for %s",
                CLSS,type.name))
        }
    }

    // The byte array contains the results of a request for status. It may be the concatenation
    // of several responses. Update global motor configurations accordingly.
    // 
    private fun updateMotorStatusFromBytes(property: JointDynamicProperty, bytes: ByteArray): Map<Int, String> {
        val props: MutableMap<Int, String> = HashMap()
        DxlMessage.updatePropertyInMotorsFromBytes(property, RobotModel.motorsById, bytes, props)
        return props
    }

    /**
     * Read from the event, prependinga any remainder from the last message.
     * Clear the remainder. Check for valid start character.
     * @param bytes
     * @return
     */
    private suspend fun accumulateResults(event:SerialPortEvent): ByteArray {
        mutex.withLock {
            val byteCount: Int = event.getEventValue()
            if( byteCount==0 ) return remainder

            var bytes: ByteArray = event.port.readBytes(byteCount)
            if( remainder.size==0 ) {
                bytes = DxlMessage.ensureLegalStart(bytes) // Logs any truncation
                return bytes
            }
            var aggregate = ByteArray(remainder.size + bytes.size)
            System.arraycopy(remainder, 0, aggregate, 0, remainder.size)
            System.arraycopy(bytes, 0, aggregate, remainder.size, bytes.size)
            remainder = ByteArray(0)
            aggregate = DxlMessage.ensureLegalStart(aggregate) // Logs any truncation
            return aggregate
        }
    }

    /**
     * Create a remainder from extra bytes at the end of the array.
     * Remainder should always be empty as we enter this routine.
     * @param bytes
     * @param nb count of bytes we used.
     * @return
     */
    private suspend fun truncateByteArray(bytes: ByteArray, nb: Int): ByteArray {
        mutex.withLock() {
            var nbytes = nb
            if (nbytes > bytes.size) nbytes = bytes.size
            if (nbytes == bytes.size) return bytes
            val copy = ByteArray(nbytes)
            System.arraycopy(bytes, 0, copy, 0, nbytes)
            remainder = ByteArray(0)
            return copy
        }
    }

    private val CLSS = "SerialResponder"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    private val STATUS_RESPONSE_LENGTH = 8 // byte count

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_SERIAL)
        LOGGER.info(String.format("%s.init: created...", CLSS))
        remainder = ByteArray(0)
        mutex = Mutex()
    }
}