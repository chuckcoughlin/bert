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
@DelicateCoroutinesApi
class SequentialQueue(sender:Channel<MessageBottle>) : LinkedList<MessageBottle>() {
    private val channel = sender
    private var job: Job
    private var ready:Boolean

    /*
     * Coroutine to send  a message to the dispatcher with a proper delay to
     * account for joint travel time, if appropriate.
     * Calculate a time for the motion and also respect any user
     * defined delay.
     *
     * There may be a delay before the message is sent.
     * During this time ready should be false.
     */
    suspend fun dispatch(msg:MessageBottle) {
        val now=System.currentTimeMillis()
        var text = msg.joint.name
        if( text.equals(Joint.NONE.name)) text = msg.limb.name
        var earliestTime = computeEarliestTime(msg)
        LOGGER.info(String.format("%s.dispatch: %s on %s after %d msecs.", CLSS,msg.type.name,text,earliestTime-now))
        if(earliestTime > now) {
            delay(earliestTime-now)
        }
        else {
            earliestTime = now
        }
        setDispatchTimes(msg,earliestTime)
        channel.send(msg)
    }

    /**
     * Add the specified message to the end of the queue. Set the execution time
     * respecting the delay setting. Start the execute() function.
     */
    override fun addLast(msg: MessageBottle) {
        if( ready && size==0 ) {
            ready=false
            job = GlobalScope.launch(Dispatchers.IO) {
                dispatch(msg)
            }
        }
        else {
            super.addLast(msg)
        }
    }

    /**
     * We have received notice from the MotorGroupController that it is in a state
     * to receive the next command. If we have a request pending, send it.
     */
    fun markReady() {
        if( ready==true ) {
            LOGGER.info(String.format("%s.markReady: ERROR - controller is already ready",CLSS))
            return
        }
        if( size>0 ) {    // Dispatch waiting message
            ready = false
            val msg = removeFirst()
            job = GlobalScope.launch(Dispatchers.IO) {
                dispatch(msg)
            }
        }
        else {
            ready = true
        }
    }
    /**
     * Remove the next message from the queue in preparation for dispatching it.
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

    /**
     * Compute earliest time the command can be executed without interfering
     * with the previous command. NOTE: travel time can be excessive if the speed is incorrect.
     */
    private fun computeEarliestTime(msg:MessageBottle) : Long {
        var earliestTime = System.currentTimeMillis()
        for( mc in motorsForMessage(msg)) {
            val readyTime = mc.dispatchTime + mc.travelTime
            if( readyTime>earliestTime ) earliestTime = readyTime
        }
        earliestTime = earliestTime + msg.control.delay
        return earliestTime
    }
    private fun motorsForJoint(joint: Joint): List<MotorConfiguration> {
        val list: MutableList<MotorConfiguration> = mutableListOf<MotorConfiguration>()
        for (mc in RobotModel.motorsByJoint.values) {
            if (mc.joint==joint) {
                list.add(mc)
            }
        }
        return list
    }
    private fun motorsForLimb(limb: Limb): List<MotorConfiguration> {
        val list: MutableList<MotorConfiguration> = mutableListOf<MotorConfiguration>()
        for (mc in RobotModel.motorsByJoint.values) {
            if (mc.limb==limb || limb==Limb.NONE) {
                list.add(mc)
            }
        }
        return list
    }
    /**
     * Return a list of the motor configurations applicable for a message.
     * The list may be empty, especially if the message does not involve a write.
     */
    private fun motorsForMessage(msg:MessageBottle) : List<MotorConfiguration> {
        var list = listOf<MotorConfiguration>()
        if( msg.joint==Joint.NONE && msg.limb==Limb.NONE ) { // All joints
            list = RobotModel.motorsByJoint.values.toList()
        }
        else if( msg.limb!=Limb.NONE) {
            list = motorsForLimb(msg.limb)
        }
        else if( msg.limb!=Limb.NONE) {
            list = motorsForJoint(msg.joint)
        }
        return list
    }
    /**
     *
     */
    fun setDispatchTimes(msg:MessageBottle,executionTime:Long) {
        msg.control.executionTime = executionTime
        val list = motorsForMessage(msg)
        for(mc in list ) {
            mc.dispatchTime = executionTime
        }
    }

    private val CLSS="SequentialQueue"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG : Boolean
    private val POLL_INTERVAL = 100L // While waiting for ready


    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_INTERNAL)
        job=Job()
        job.cancel()
        ready = true  // Initially the motor controller is ready
    }
}