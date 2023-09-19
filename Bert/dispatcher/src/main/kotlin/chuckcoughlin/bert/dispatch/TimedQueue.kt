/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.message.ExecutionControl
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.JointDefinitionProperty
import kotlinx.coroutines.runBlocking
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
class TimedQueue(private val ic: InternalController) : LinkedList<MessageBottle>(), Runnable {
    protected var stopped = true
    protected var timerThread: Thread? = null
    protected val idleMessage: MessageBottle
    protected var currentTime: Long = 0

    /**
     * Add a new message to the list ordered by its absolute
     * execution time which must be already set.
     * The list is never empty, there is at least the IDLE message.
     * @param msg message to be added
     */
    fun addMessage(msg: MessageBottle) {
        insertMessage(msg)
    }

    /**
     * Insert a new message into the list in execution order.
     * This list is assumed never to be empty
     */
    private fun insertMessage(msg: MessageBottle) {
        var index = 0
        val iter: Iterator<MessageBottle> = iterator()
        while (iter.hasNext()) {
            val im = iter.next()
            if (im.control.executionTime > msg.control.executionTime) {
                add(index, msg)
                val now = System.nanoTime() / 1000000
                LOGGER.info(String.format(
                        "%s.insertMessage(%d): %s scheduled in %d msecs position %d",
                        CLSS, msg.control.id, msg.type.name,
                        msg.control.executionTime - now, index
                    )
                )
                if (index == 0) timerThread!!.interrupt() // We've replaced the head
                return
            }
            index++
        }
        addLast(msg)
    }

    /**
     * If top holder is the IDLE holder, then simply "pet" it. The IDLE
     * holder is distinguished by a null message.
     * Otherwise pop the top holder and inform the launcher to execute.
     */
    suspend private fun fireExecutor() {
        val msg = removeFirst()
        if (msg == null) {
            val now = System.nanoTime() / 1000000
            msg.control.executionTime = now + msg.control.repeatInterval
            add(msg)
        }
        else {
            LOGGER.info(String.format("%s.fireExecutor: dispatching(%s) %s ...",
                    CLSS, JointDefinitionProperty.ID,msg.type.name))
            ic.dispatchMessage(msg)
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
                    CLSS,timerThread!!.name,timerThread.hashCode()) )
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
    override fun run() {
        while (!stopped) {
            val now = System.nanoTime() / 1000000 // Work in milliseconds
            val head = first
            val waitTime = head.control.executionTime - now
            try {
                if (waitTime > 0) {
                    Thread.sleep(waitTime)
                }
                currentTime = head.control.executionTime
                if (!stopped) runBlocking{ fireExecutor() }
            } // An interruption allows a recognition of re-ordering the queue
            catch (e: InterruptedException) {
                //LOGGER.info(String.format("%s.run: wait interrupted ---",getName()));
            }
            catch (ex: Exception) {
                LOGGER.log(Level.SEVERE,String.format("%s.Exception during timeout processing (%s)",
                    CLSS, ex.localizedMessage), ex)
            }
        }
    }

    private val CLSS = "TimedQueue"
    private val IDLE_DELAY = 250      // Millisecs
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        idleMessage = MessageBottle(RequestType.IDLE)
        idleMessage.control.shouldRepeat = true
        idleMessage.control.repeatInterval = IDLE_DELAY.toLong()
    }
}