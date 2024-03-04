/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.controller.MessageController
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.logging.Logger

/**
 * After a specified delay, the next message is returned.
 * There is at least one message present in
 * the queue, the HEARTBEAT.
 */
class TimedQueue(private val controller: MessageController) : MutableList<MessageBottle> by mutableListOf() {
    protected var channel: Channel<Long>    // Signal mechanism
    protected var delayTime: Long     // For next message
    protected var running : Boolean
    val heartbeat:MessageBottle
    var job: Job = Job()

    @DelicateCoroutinesApi
    suspend fun execute() = coroutineScope {
        running = true
        addMessage(heartbeat)    // Guarantee there is always at least one
        job = launch(Dispatchers.Main) { runner() }
    }
    /**
     * Add a new message to the list ordered by its absolute execution time
     * which must be already set. The queue is longest delay until shortest.
     * The list is never empty, there is at least the HEARTBEAT message.
     * @param msg message to be added
     * @return the position of the new message in the queue. If the
     *         result is zero, then the new message is next to be
     *         executed.
     */

    suspend fun addMessage(msg: MessageBottle) {
        msg.control.executionTime = System.currentTimeMillis() + msg.control.delay
        var index = 0
        val now = System.currentTimeMillis()
        val iter: Iterator<MessageBottle> = iterator()
        while (iter.hasNext()) {
            val im = iter.next()
            if (msg.control.executionTime < im.control.executionTime  ) {
                add(index, msg)
                if( DEBUG ) {
                    LOGGER.info(String.format("%s.addMessage: %s scheduled in %d msecs position %d",
                            CLSS, msg.type.name, msg.control.executionTime - now, index))
                }
                return
            }
            index++
        }
        // If this is the first messaage in the queue change the delay time
        add(msg)
        delayTime = msg.control.executionTime - now
        channel.send(delayTime)
    }

    suspend fun handleCompletion() {
        val msg = last()
        if (DEBUG) LOGGER.info(String.format(
            "%s.handleCompletion: executing %s ...", CLSS, msg.type.name ))
        removeLast()
        if (msg.control.shouldRepeat) {

            val cln = msg.clone()
            addMessage(cln)
        }
    }

    suspend fun runner() {
        while(running) {
            if (DEBUG) LOGGER.info(String.format("%s.runner: started ...", CLSS ))
            withTimeoutOrNull(delayTime) {
                handleCompletion()
                channel.receive()
            }
        }
    }

    /**
     * On stop, set all the msgs to inactive.
     */
    @Synchronized
    fun stop() {
        running = false
        job.cancel()
    }

    private val CLSS = "TimedQueue"
    private val DEBUG: Boolean
    private val HEARTBEAT_INTERAL = 10000L // 10 secs
    private val POLL_INTERVAL = 10000L
    private val LOGGER = Logger.getLogger(CLSS)

    init{
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_INTERNAL)
        channel = Channel<Long>()
        delayTime = HEARTBEAT_INTERAL
        running = false
        heartbeat = MessageBottle(RequestType.HEARTBEAT)
        heartbeat.text = "heartbeat"
        heartbeat.source = ControllerType.INTERNAL.name
        heartbeat.control.repeatInterval = HEARTBEAT_INTERAL
        heartbeat.control.shouldRepeat = true
        heartbeat.control.delay = HEARTBEAT_INTERAL
        heartbeat.control.executionTime = System.currentTimeMillis()
    }
}