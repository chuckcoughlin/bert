/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.controller.MessageController
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import kotlinx.coroutines.*
import java.util.logging.Logger

/**
 * After a specified delay, the next message is returned.
 * There is always, at least one message present in
 * the queue, the HEARTBEAT.
 */
class TimedQueue(private val controller: MessageController) : MutableList<MessageBottle> by mutableListOf() {
    protected var stopped = false
    val heartbeat:MessageBottle
    @DelicateCoroutinesApi
    private val scope = GlobalScope // For long-running coroutines
    var job: Job = Job()

    @DelicateCoroutinesApi
    suspend fun execute() {
        job = scope.launch {
            try {
                scope.launch {
                    var period = POLL_INTERVAL // If queue is empty

                    if( size>0 ) {
                        LOGGER.info(String.format("%s.execute: delaying with %d msgs", CLSS,size ))
                        val msg = get(0)
                        period = msg.control.executionTime - System.currentTimeMillis()
                        LOGGER.info(String.format("%s.execute: delaying %d msecs", CLSS,period ))
                        handleCompletion(msg)
                    }

                    delay(period)

                    LOGGER.info(String.format("%s.execute...", CLSS))
                }
            }
            catch (ex: CancellationException) {
                    LOGGER.info(String.format("%s.execute: Cancellation exception.", CLSS))
            }
        }
    }
    /**
     * Add a new message to the list ordered by its absolute
     * execution time which must be already set.
     * The list is never empty, there is at least the HEARTBEAT message.
     * @param msg message to be added
     * @return the position of the new message in the queue. If the
     *         result is zero, then the new message is next to be
     *         executed.
     */
    @Synchronized
    fun addMessage(msg: MessageBottle,cancelScope:Boolean) {
        msg.control.executionTime = System.currentTimeMillis()+msg.control.delay
        var index = 0
        val iter: Iterator<MessageBottle> = iterator()
        while (iter.hasNext()) {
            val im = iter.next()
            if (im.control.executionTime > msg.control.executionTime) {
                add(index, msg)
                if( DEBUG ) {
                    val now = System.nanoTime() / 1000000
                    LOGGER.info(String.format("%s.addMessage: %s scheduled in %d msecs position %d",
                            CLSS, msg.type.name, msg.control.executionTime - now, index))
                }
                return
            }
            index++
        }
        add(msg)
        if(index==1 && cancelScope) job.cancelChildren()
    }

    @Synchronized
    fun handleCompletion(msg:MessageBottle) {
        remove(msg)
        if (msg.control.shouldRepeat) {
            if (DEBUG) LOGGER.info(String.format(
                    "%s.handleCompletion: repeat message %s ...", CLSS, msg.type.name ))
            val cln = msg.clone()
            addMessage(cln,false)
        }
    }


    /**
     * On stop, set all the msgs to inactive.
     */
    @Synchronized
    fun stop() {
        stopped = true
        job.cancel()
    }

    private val CLSS = "TimedQueue"
    private val DEBUG = true
    private val HEARTBEAT_INTERAL = 10000L // 10 secs
    private val POLL_INTERVAL = 10000L
    private val LOGGER = Logger.getLogger(CLSS)

    init{
        stopped = false
        heartbeat = MessageBottle(RequestType.HEARTBEAT)
        heartbeat.text = "heartbeat"
        heartbeat.source = ControllerType.INTERNAL.name
        heartbeat.control.repeatInterval = HEARTBEAT_INTERAL
        heartbeat.control.shouldRepeat = true
        heartbeat.control.delay = HEARTBEAT_INTERAL
        heartbeat.control.executionTime = System.currentTimeMillis()
        addMessage(heartbeat,false)
    }
}