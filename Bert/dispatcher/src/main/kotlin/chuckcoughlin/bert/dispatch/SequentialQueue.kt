/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.Limb
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import java.util.logging.Logger

/**
 * Holds a list of requests to be executed in sequence. There is a separate
 * queue for each robot limb. It should be noted that all joints on a given limb
 * are on the same robot sub-chain of motors.
 *
 * The inProgress flag true means that there is a message from the queue
 * currently being executed by the dispatcher. This is used to prevent
 * immediate execution of a new request if inappropriate.
 */
class SequentialQueue(lim: Limb,sender:Channel<MessageBottle>,) : LinkedList<MessageBottle>() {
    private val channel = sender
    private val limb = lim
    private var nextAllowedExecuteTime: Long=0
    private var job: Job

    /*
     * Coroutine to send  messages in order to the dispatcher.
     * Calculate a time for the motion and also respect any user
     * defined delay.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun suspend execute() {
        if(this.size > 0) {  // If there are no waiting messages, do nothing
            while(this.size > 0) {
                LOGGER.info(String.format("%s.execute: launched...", CLSS))
                job=GlobalScope.async(Dispatchers.IO) {
                    while(running) {
                        val msg=removeFirst()
                        val
                                channel.onSend {

                        }

                    }
                }
            }

            else {
                LOGGER.warning(String.format("%s.execute: attempted to start, but already running...", CLSS))
            }
        }
    }

    /**
     * Add the specified message to the end of the queue. Set the execution time
     * respecting the delay setting. Start the execute() function.
     */
    override fun addLast(msg: MessageBottle) {
        super.addLast(msg)
        val now=System.nanoTime() / 1000000
        msg.control.executionTime=now + msg.control.delay
        execute()
    }

    /**
     * Remove the next holder from the queue in preparation for adding it
     * to the timed delay queue. Update the time at which we are allowed to trigger the next message.
     */
    override fun removeFirst(): MessageBottle {
        val msg=super.removeFirst()
        val now=System.nanoTime() / 1000000
        if(nextAllowedExecuteTime < now) nextAllowedExecuteTime=now
        if(msg.control.executionTime < nextAllowedExecuteTime) {
            msg.control.executionTime=nextAllowedExecuteTime
        }
        nextAllowedExecuteTime=msg.control.executionTime
        delay(nextAllowedExecuteTime-now)
        return msg
    }

    fun shutdown() {
        if( job.isActive ) job.cancel()
    }

    private val CLSS="SequentialQueue"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        nextAllowedExecuteTime=System.nanoTime() / 1000000 // Work in milliseconds
        job=Job()
    }
}