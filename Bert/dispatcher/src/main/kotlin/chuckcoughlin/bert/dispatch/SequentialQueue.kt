/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
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
 * Holds a list of requests to be executed in sequence. There is a separate
 * queue for each robot limb. It should be noted that all joints on a given limb
 * are on the same robot sub-chain of motors.
 *
 * The nextAllowedExecutionTime takes into account the movement time
 * of the prior movement on the same limb..
 */
class SequentialQueue(lim: Limb,sender:Channel<MessageBottle>,configMap: Map<Joint,MotorConfiguration>) : LinkedList<MessageBottle>() {
    private val channel = sender
    private val limb = lim
    private val motorMap = configMap
    private var nextAllowedExecuteTime: Long
    private var job: Job

    /**
     * The co-routine runs until there are no more messages in the queue
     * and then quits.
     */
    fun start() {
        LOGGER.info(String.format("%s.start: %s = %d %s.", CLSS,limb.name,size,if(job.isActive) "ACTIVE" else "INACTIVE"))
        if( !job.isActive ) {
            job = GlobalScope.launch(Dispatchers.IO) {
                execute()
            }
        }
    }
    /*
     * Coroutine to send  messages in order to the dispatcher.
     * Calculate a time for the motion and also respect any user
     * defined delay.
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun execute() {
        while(size > 0) {
            val msg = removeFirst()
            val now=System.nanoTime() / 1000000
            LOGGER.info(String.format("%s.execute: %s on %s.", CLSS,msg.type.name,limb.name))
            while( !SequentialQueue.ready ) {
                delay(POLL_INTERVAL)
            }
            SequentialQueue.ready = false
            if(nextAllowedExecuteTime < now) nextAllowedExecuteTime=now
            nextAllowedExecuteTime = nextAllowedExecuteTime + msg.control.delay
            delay(nextAllowedExecuteTime-now)
            msg.control.executionTime = nextAllowedExecuteTime
            markDispatchTime(nextAllowedExecuteTime)
            nextAllowedExecuteTime = nextAllowedExecuteTime + travelTime(msg)
            channel.send(msg)
        }
    }

    /**
     * Add the specified message to the end of the queue. Set the execution time
     * respecting the delay setting. Start the execute() function.
     */
    override fun addLast(msg: MessageBottle) {
        if(DEBUG) LOGGER.info(String.format("%s.addLast: %s on %s.", CLSS,msg.type.name,limb.name))
        super.addLast(msg)
        start()
    }

    /**
     * Remove the next holder from the queue in preparation for adding it
     * to the timed delay queue. Update the time at which we are allowed to trigger the next message.
     */
    override fun removeFirst(): MessageBottle {
        val msg=super.removeFirst()
        return msg
    }

    fun shutdown() {
        if( job.isActive ) job.cancel()
    }

    // Compute the time the limb takes to move when executing
    // this request, if applicable.
    fun travelTime(msg:MessageBottle) : Long {
        var period = ConfigurationConstants.MIN_SERIAL_WRITE_INTERVAL

        if(msg.type.equals(RequestType.SET_LIMB_PROPERTY) &&
            !limb.equals(Limb.NONE)) {
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

    fun markDispatchTime(time:Long) {
        for(mc in motorMap.values ) {
            mc.commandTime = time
        }
    }

    private val CLSS="SequentialQueue"
    private val LOGGER = Logger.getLogger(CLSS)
    private val DEBUG : Boolean
    private val POLL_INTERVAL = 100L // While waiting for ready

    /**
     * Treat the ready flag as a class-side variable
     */
    companion object {
        var ready = true
    }

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_INTERNAL)
        nextAllowedExecuteTime=System.nanoTime() / 1000000 // Work in milliseconds
        job=Job()
        job.cancel()
    }
}