/**
 * Copyright 2019-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.sql.db.Database
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
     * Send  a sequenced messsage to the dispatcher.
     *
     * There may be a delay before the message is sent.
     * During this time ready should be false.
     */
    suspend fun dispatch(msg:MessageBottle) {
        val now=System.currentTimeMillis()
        var text:String
        if( msg.type==RequestType.JSON ) text = msg.jtype.name
        else {
            text = msg.joint.name
            if (text.equals(Joint.NONE.name)) text = msg.limb.name
        }
        var executionTime = prepareConfigurationsForExecution(msg)
        LOGGER.info(String.format("%s.dispatch: %s on %s after %d msecs.", CLSS,msg.type.name,text,executionTime-now))
        if(executionTime > now) {
            delay(executionTime-now)
        }

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
     * Set target angles and speeds then calculate the earliest time without
     * conflict with existing motion. Mark dispatch times.
     * @return the latest dispatch time
     */
    private fun prepareConfigurationsForExecution(msg:MessageBottle) : Long {
        val list = mutableListOf<MotorConfiguration>()
        var executionTime = System.currentTimeMillis()
        val now = executionTime
        if (msg.type==RequestType.INITIALIZE_JOINTS) {
            for (mc in RobotModel.motorsByJoint.values) {
                list.add(mc)
            }
        }
        else if (msg.type==RequestType.EXECUTE_POSE) {
            val poseName: String = msg.arg
            val index: Int = msg.values[0].toInt()
            val poseid = Database.getPoseIdForName(poseName,index)
            val motors = Database.configureMotorsForPose( poseid)
            for( mc in motors ) {
                val tt = computeTravelTime(mc)
                if(mc.dispatchTime+tt > executionTime) executionTime = mc.dispatchTime+tt
                list.add(mc)
            }
        }
        else if (msg.type==RequestType.SET_LIMB_PROPERTY) {
            val prop = msg.jointDynamicProperty
            var value = msg.values[0]
            for (mc in RobotModel.motorsByJoint.values) {
                if (msg.limb==Limb.NONE ||  mc.limb==msg.limb) {
                    val tt = computeTravelTime(mc)
                    if(mc.dispatchTime+tt > executionTime) executionTime = mc.dispatchTime+tt
                    mc.setDynamicProperty(prop,value)
                    list.add(mc)
                }
            }
        }
        else if (msg.type==RequestType.SET_MOTOR_PROPERTY) {
            val prop = msg.jointDynamicProperty
            var value = msg.values[0]
            for (mc in RobotModel.motorsByJoint.values) {
                val tt = computeTravelTime(mc)
                if(mc.dispatchTime+tt > executionTime) executionTime = mc.dispatchTime+tt
                mc.setDynamicProperty(prop,value)
                list.add(mc)
            }
        }
        executionTime = executionTime + msg.control.delay
        for(mc in list) {
            mc.dispatchTime = executionTime
            if(DEBUG && executionTime-now>EXCESSIVE_DELAY ) {
                LOGGER.info(String.format("%s.prepareConfigurationsForExecution: %s EXCESSIVE delay = %d msecs.", CLSS,
                    mc.joint.name,now-mc.dispatchTime))
            }
        }
        return executionTime
    }

    private fun computeTravelTime(mc:MotorConfiguration) : Long {
        val tt = Math.abs(1000*(mc.targetAngle-mc.angle)/mc.speed).toLong()
        return tt
    }


    private val CLSS="SequentialQueue"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG : Boolean
    private val POLL_INTERVAL   = 100L  // While waiting for ready
    private val EXCESSIVE_DELAY = 5000L // Report when debugging


    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_INTERNAL)
        job=Job()
        job.cancel()
        ready = true  // Initially the motor controller is ready
    }
}