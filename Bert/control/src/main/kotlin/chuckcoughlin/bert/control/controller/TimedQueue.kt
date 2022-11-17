/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.control.controller

import chuckcoughlin.bert.control.message.InternalMessageHolder
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * At the specified time, the next message is sent to the launcher.
 * Patterned after a watchdog timer which manages a collection of "watchdogs". The dogs
 * are sorted by expiration time. "petting" a dog resets the timeout
 * perhaps indefinitely. Once the petting stops, the dog's "evaluate"
 * method is invoked. There is always, at least one dog present in
 * the list, the IDLE dog.
 */
class TimedQueue(private val controller: InternalController) : LinkedList<InternalMessageHolder>(), Runnable {
    protected var stopped = true
    protected var timerThread: Thread? = null
    protected val idleMessage: InternalMessageHolder
    protected var currentTime: Long = 0

    /**
     * Add a new message (in a holder) to the list ordered by its absolute
     * execution time which must be already set.
     * The list is never empty, there is at least the IDLE message.
     * @param msg message to be added
     */
    fun addMessage(holder: InternalMessageHolder) {
        insertMessage(holder)
    }

    /**
     * Insert a new message into the list in execution order.
     * This list is assumed never to be empty
     */
    private fun insertMessage(holder: InternalMessageHolder) {
        var index = 0
        val iter: Iterator<InternalMessageHolder> = iterator()
        while (iter.hasNext()) {
            val im = iter.next()
            if (im.executionTime > holder.executionTime) {
                add(index, holder)
                val now = System.nanoTime() / 1000000
                LOGGER.info(
                    java.lang.String.format(
                        "%s.insertMessage(%d): %s scheduled in %d msecs position %d",
                        CLSS, holder.ID.id, holder.message.type.name,
                        holder.executionTime - now, index
                    )
                )
                if (index == 0) timerThread!!.interrupt() // We've replaced the head
                return
            }
            index++
        }
        addLast(holder)
    }

    /**
     * If top holder is the IDLE holder, then simply "pet" it. The IDLE
     * holder is distinguished by a null message.
     * Otherwise pop the top holder and inform the launcher to execute.
     */
    @Synchronized
    private fun fireExecutor() {
        val holder = removeFirst()
        if (holder!!.message == null) {
            val now = System.nanoTime() / 1000000
            holder.executionTime = now + holder.repeatInterval
            add(holder)
        }
        else {
            LOGGER.info(String.format("%s.fireExecutor: dispatching(%d) %s ...",
                    CLSS,holder.ID.id,holder.message.type.name))
            controller.dispatch(holder)
        }
        timerThread!!.interrupt()
    }

    /**
     * This is for a restart. Use a new thread.
     */
    @Synchronized
    fun start() {
        if (stopped) {
            clear()
            addFirst(idleMessage)
            stopped = false
            timerThread = Thread(this, CLSS)
            timerThread!!.isDaemon = true
            timerThread!!.start()
            LOGGER.info(String.format("%s.START timer thread %s (%d)",
                    name,
                    timerThread!!.name,
                    timerThread.hashCode()
                )
            )
        }
    }

    /**
     * On stop, set all the msgs to inactive.
     */
    @Synchronized
    fun stop() {
        if (!stopped) {
            stopped = true
            if (timerThread != null) {
                timerThread!!.interrupt()
            }
        }
    }

    /**
     * A timeout causes the head to be notified, then pops up the next dog.
     */
    @Synchronized
    override fun run() {
        while (!stopped) {
            val now = System.nanoTime() / 1000000 // Work in milliseconds
            val head = first
            val waitTime = (head.executionTime - now) as Long
            try {
                if (waitTime > 0) {
                    wait(waitTime)
                }
                currentTime = head.executionTime
                if (!stopped) fireExecutor()
            } // An interruption allows a recognition of re-ordering the queue
            catch (e: InterruptedException) {
                //LOGGER.info(String.format("%s.run: wait interrupted ---",getName()));
            } catch (ex: Exception) {
                LOGGER.log(
                    Level.SEVERE,
                    String.format("%s.Exception during timeout processing (%s)", CLSS, ex.localizedMessage),
                    ex
                )
            }
        }
    }

    companion object {
        private const val serialVersionUID = -5509446352724816963L
        private const val CLSS = "TimedQueue"
        private const val IDLE_DELAY = 60000 // One minute
        private val LOGGER = Logger.getLogger(CLSS)
    }
    init {
        idleMessage = InternalMessageHolder()
        idleMessage.shouldRepeat = true
        idleMessage.repeatInterval = IDLE_DELAY.toLong()
    }
}