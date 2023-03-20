/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * MIT License
 */
package chuckcoughlin.bert.control.message

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.control.controller.QueueName

/**
 * This is a wrapper for carriers of MessageBottles as they are processed by an
 * InternalController. These come in two flavors - those that wait on a queue and
 * those wait on a timer. The queued messages may optionally have a timed delay as well.
 */
class InternalMessageHolder {
    /*
     * The delay interval is an idle interval between when this message is
     * first placed on the timer queue and when actually executes. Any time
     *  spent waiting on the sequential queue is counted toward the delay
     * ("time served").
    */
    var delay: Long
     /*
     * The execution time is the earliest time at which this message is allowed
     * to be sent to the Dispatcher. The time is calculated as the message
     * is placed on the timer queue.
     */
    var executionTime: Long   // ~msecs
    var repeatInterval: Long  // ~msecs
    var message: MessageBottle
    var originalSource: String
    var queue: QueueName
    var shouldRepeat: Boolean

    /**
     * Constructor for the IDLE holder used by the timer. The message
     * is of type NONE
     */
    constructor() {
        message = MessageBottle(RequestType.NONE)
    }

    /**
     * Constructor for a message that is delayed, but is not restricted to
     * executing sequentially.
     * @param msg the ultimate message to execute
     * @param interval delay time ~msecs
     */
    constructor(msg: MessageBottle, interval: Long) {
        message = msg
        delay = interval
        originalSource = message.source
    }

    /**
     * Constructor for a message that is restricted to sequential execution
     * after existing messages on its same FIFO queue.
     * @param msg the message to execute when processing by the InternalController is complete.
     */
    constructor(msg: MessageBottle, q: QueueName) {
        message = msg
        delay = 0
        queue = q
        originalSource = message.source
    }


    fun reinstateOriginalSource() {
        message.source = originalSource
    }

    override fun toString(): String {
        return String.format("%s: %s expires in %d ms",
            CLSS,message.type.name,executionTime - System.nanoTime() / 1000000 )
    }

    private val CLSS = "InternalMessageHolder"
    var id: Long = 0 // Sequential id for messages
    @get:Synchronized
    private val nextId: Long
        get() = ++id

    init {
        delay = 0
        executionTime  = 0
        repeatInterval = 0
        shouldRepeat = false
        originalSource = BottleConstants.NO_SOURCE
        queue = QueueName.GLOBAL
        id = nextId
    }
}