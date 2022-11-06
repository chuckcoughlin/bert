/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * MIT License
 */
package chuckcoughlin.bert.control.message

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.control.controller.QueueName

/**
 * This is a base class for carriers of MessageBottles as they are processed by an
 * InternalController. These come in two flavors - those that wait on a queue and
 * those wait on a timer. The queued messages may optionally have a timed delay as well.
 */
class InternalMessageHolder {
    /**
     * The delay interval is an idle interval between when this message is
     * first placed on the timer queue and when actually executes. Any time
     * spent waiting on the sequential queue is counted toward the delay
     * ("time served").
     */
    var delay: Long = 0

    /**
     * The execution time is earliest time at which this message is allowed
     * to be sent to the Dispatcher. The time is calculated as the message
     * is placed on the timer queue.
     */
    var executionTime: Long = 0     // ~msecs
    var repeatInterval: Long = 1000 // ~msecs
    private val message: MessageBottle?
    private val originalSource: String?
    private var queue: QueueName?
    private var repeat: Boolean

    /**
     * Constructor for the IDLE holder used by the timer. There is
     * no message.
     */
    constructor() {message = null,queue = null,delay,repeat = false,originalSource = null
    }

    /**
     * Constructor for a message that is timed, but is not restricted to
     * executing sequentially.
     * @param msg the ultimate message to execute
     * @param interval delay time ~msecs
     */
    constructor(msg: MessageBottle?, interval: Long) {
        message = msg
        queue = null
        delay = interval
        repeat = false
        originalSource = message.fetchSource()
        message.id = nextId
    }

    /**
     * Constructor for a message that is restricted to sequential execution
     * after existing messages on its same FIFO queue.
     * @param msg the message to execute when processing by the InternalController is complete.
     */
    constructor(msg: MessageBottle?, q: QueueName?) {
        message = msg
        delay = 0
        queue = q
        repeat = false
        originalSource = message.fetchSource()
        message.id = nextId
    }

    fun getMessage(): MessageBottle? {
        return message
    }

    fun getQueue(): QueueName? {
        return queue
    }

    fun shouldRepeat(): Boolean {
        return repeat
    }

    fun setShouldRepeat(flag: Boolean) {
        repeat = flag
    }

    fun reinstateOriginalSource() {
        message.assignSource(originalSource)
    }

    override fun toString(): String {
        return String.format("%s: %s expires in %d ms",
            CLSS,message.fetchRequestType().name(),executionTime - System.nanoTime() / 1000000 )
    }

    companion object {
        private const val serialVersionUID = 4356286171135500677L
        private const val CLSS = "InternalMessageHolder"
        private var id: Long = 0 // Sequential id for messages

        @get:Synchronized
        val nextId: Long
            get() = ++id
    }
}