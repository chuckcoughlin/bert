/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.logging.Logger

/**
 * Holds a list of requests to be executed in sequence. All messages, regardless of
 * limb are in the same queue. It should be noted that all joints on a given limb
 * are on the same robot sub-chain of motors.
 *
 * The nextAllowedExecutionTime takes into account the movement time
 * of the prior command on the same limb.
 */
class SequentialQueue(sender:Channel<MessageBottle>) : LinkedList<MessageBottle>() {
    private val channel = sender
    private var job: Job
    private var ready:Boolean


    /*
     * Coroutine to send  a message to the dispatcher with a proper delay to
     * account for joint travel time, if appropriate.
     * Calculate a time for the motion and also respect any user
     * defined delay.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun dispatch(msg:MessageBottle) {
        val now=System.nanoTime() / 1000000
        val limb = msg.limb
        LOGGER.info(String.format("%s.dispatch: %s on %s.", CLSS,msg.type.name,limb.name))
        var earliestTime = computeEarliestTime(msg)
        if(earliestTime > now) {
            delay(earliestTime-now)
        }
        else {
            earliestTime = now
        }
        setExecutionTimes(msg,earliestTime)
        channel.send(msg)
        ready = false
    }

    /**
     * Add the specified message to the end of the queue. Set the execution time
     * respecting the delay setting. Start the execute() function.
     */
    override fun addLast(msg: MessageBottle) {
        //if(DEBUG) LOGGER.info(String.format("%s.addLast: %s on %s.", CLSS,msg.type.name,limb.name))
        super.addLast(msg)
    }

    /**
     * We have received notice from the MotorGroupController that it is in a state
     * to receive the next command. If we have a request pending, send it.
     */
    fun markReady() {
        if( ready==true || job.isActive ) {
            LOGGER.info(String.format("%s.markReady: ERROR - controller is already ready",CLSS))
        }
        ready = true

        if( size>0 ) {
            val msg = removeFirst()
            job = GlobalScope.launch(Dispatchers.IO) {
                dispatch(msg)
            }
        }
    }
    /**
     * Remove the next holder from the queue in preparation for adding it
     * to the timed delay queue. Update the time at which we are allowed to trigger the next message.
     */
    override fun removeFirst(): MessageBottle {
        val msg = super.removeFirst()
        return msg
    }

    /**
     * Clear the message queue and assume a READY state
     */
    fun reset() {
        clear()
        ready = true
    }

    fun shutdown() {
        if( job.isActive ) job.cancel()
    }


    private fun computeEarliestTime(msg:MessageBottle) : Long {
        var earliestTime = System.currentTimeMillis()

        return earliestTime
    }
    private fun configurationForJoint(joint: Joint): List<MotorConfiguration> {
        val list: MutableList<MotorConfiguration> = mutableListOf<MotorConfiguration>()
        for (joint in RobotModel.motorsByJoint.keys) {
            val mc = RobotModel.motorsByJoint[joint]!!
            if (mc.joint==joint) {
                list.add(mc)
            }
        }
        return list
    }
    private fun configurationsForLimb(limb: Limb): List<MotorConfiguration> {
        val list: MutableList<MotorConfiguration> = mutableListOf<MotorConfiguration>()
        for (joint in RobotModel.motorsByJoint.keys) {
            val mc = RobotModel.motorsByJoint[joint]!!
            if (mc.limb.equals(limb) || limb.equals(Limb.NONE)) {
                list.add(mc)
            }
        }
        return list
    }
    /**
     * Return a list of the motor configurations applicable for a message.
     * The list may be empty, especially if the message does not involve a write.
     */
    private fun listConfigurationsForMessage(msg:MessageBottle) : List<MotorConfiguration> {
        var list = listOf<MotorConfiguration>()
        if( msg.type==RequestType.SET_MOTOR_PROPERTY) {
            list = configurationForJoint(msg.joint)
        }
        else if(msg.type==RequestType.EXECUTE_POSE ) {
            if( msg.limb!=Limb.NONE) {
                list = configurationsForLimb(msg.limb)
            }
        }
        else {
            list = mutableListOf<MotorConfiguration>() // Empty
        }
        return list
    }
    /**
     *
     */
    fun setExecutionTimes(msg:MessageBottle,executionTime:Long) {
        msg.control.executionTime = executionTime
        val list = listConfigurationsForMessage(msg)
        for(mc in list ) {
            mc.commandTime = executionTime
        }
    }
    // Compute the time the limb takes to move when executing
    // this request, if applicable.
    fun travelTime(msg:MessageBottle) : Long {
        var period = ConfigurationConstants.MIN_SERIAL_WRITE_INTERVAL
        var maxTime = System.currentTimeMillis() + period
        val list = 

        if(msg.type.equals(RequestType.SET_LIMB_PROPERTY) &&
            msg.limb!=Limb.NONE) {
            for( mc in motorMap.values ) {
                if(mc.travelTime>period) period = mc.travelTime
            }
        }
        else if(msg.type.equals(RequestType.SET_MOTOR_PROPERTY) &&
                msg.jointDynamicProperty.equals(JointDynamicProperty.ANGLE) ) {
            for( mc in motorMap.values ) {
                if( mc.joint.equals(msg.joint)) {
                    if (mc.travelTime > period) period = mc.travelTime
                    break
                }
            }
        }
        // This applies to the NONE queue - and may interfere with other limbs
        else if(msg.type.equals(RequestType.EXECUTE_POSE) ) {

        }
        if(DEBUG) LOGGER.info(String.format("%s.travelTime: on %s = %d.", CLSS,limb.name,period))
        return period
    }

    /**
     * These are the earliest times that the controllers
     * could have been last dispatched. Set all motors
     */
    fun markDispatchTimes() {
        val now = System.currentTimeMillis()
        for(mc in RobotModel.motorsByJoint.values ) {
            mc.commandTime = now
        }
    }

    private val CLSS="SequentialQueue"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG : Boolean
    private val POLL_INTERVAL = 100L // While waiting for ready


    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_INTERNAL)
        markDispatchTimes()
        job=Job()
        job.cancel()
        ready = true  // Initially the motor controller is ready
    }
}