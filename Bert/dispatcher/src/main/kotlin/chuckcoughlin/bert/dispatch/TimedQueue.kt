/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.message.MessageBottle
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.logging.Logger

/**
 * At the specified time, the next message is sent to the launcher.
 * Patterned after a watchdog timer which manages a collection of "watchdogs". The dogs
 * are sorted by expiration time. "petting" a dog resets the timeout
 * perhaps indefinitely. Once the petting stops, the dog's "evaluate"
 * method is invoked. There is always, at least one dog present in
 * the list, the IDLE dog.
 */
class TimedQueue(private val ic: InternalController) : LinkedList<MessageBottle>() {
    protected var stopped = true

    /**
     * Add a new message to the list ordered by its absolute
     * execution time which must be already set.
     * The list is never empty, there is at least the IDLE message.
     * @param msg message to be added
     * @return the position of the new message in the queue. If the
     *         result is zero, then the new message is next to be
     *         executed.
     */
    @Synchronized
    fun addMessage(msg: MessageBottle) : Int {
        var index = 0
        val iter: Iterator<MessageBottle> = iterator()
        while (iter.hasNext()) {
            val im = iter.next()
            if (im.control.executionTime > msg.control.executionTime) {
                add(index, msg)
                if( DEBUG ) {
                    val now = System.nanoTime() / 1000000
                    LOGGER.info(String.format("%s.insertMessage(%d): %s scheduled in %d msecs position %d",
                            CLSS, msg.control.id, msg.type.name,
                            msg.control.executionTime - now, index))
                }
                return index
            }
            index++
        }
        addLast(msg)
        return index
    }

    /*
     * Delay until the first message in the queue is ready, then return it.
     * Note that it is possible to provide a different "first" message while
     * waiting for the time to expire.
     *
     * If the queue is empty, simply delay forever
     */
    suspend fun removeNextReady() : MessageBottle {
        runBlocking {
            if(DEBUG) LOGGER.info(String.format("%s.removeNextReady: waiting ...",CLSS))
            while( isEmpty() || first.control.executionTime > System.currentTimeMillis()) {
                delay(POLL_INTERVAL)
            }
            if (first.control.shouldRepeat) {
                val msg = first.copy()
                msg.control.executionTime = System.nanoTime() + msg.control.repeatInterval
            }
        }
        if(DEBUG) LOGGER.info(String.format("%s.removeNextReady: returning",CLSS))
        return removeFirst()
    }
    /**
     * This is for a restart. Use a new thread.
     */
    @Synchronized
    fun start() {
        if (stopped) {
            clear()
            stopped = false
        }
    }

    /**
     * On stop, set all the msgs to inactive.
     */
    @Synchronized
    fun stop() {
        if (!stopped) {
            stopped = true
        }
    }


    private val CLSS = "TimedQueue"
    private val DEBUG = true
    private val POLL_INTERVAL = 100L
    private val LOGGER = Logger.getLogger(CLSS)

}