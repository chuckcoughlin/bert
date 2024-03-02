/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.dispatch

import chuckcoughlin.bert.common.message.MessageBottle
import java.util.*
import java.util.logging.Logger

/**
 * Holds a list of requests to be executed in sequence. In general,
 * requests on the same queue affect the same robot sub-chain of motors.
 *
 * The locked flag true means that there is a message from the queue
 * currently being executed by the dispatcher. This is used to prevent
 * immediate execution of a new request if inappropriate.
 */
class SequentialQueue : LinkedList<MessageBottle>() {
    var locked: Boolean
    var nextAllowedExecuteTime: Long

    /**
     * Add the specified message to the end of the queue. Set the execution time
     * respecting the delay setting.
     */
    override fun addLast(message: MessageBottle) {
        super.addLast(message)
        val now = System.currentTimeMillis()
        message.control.executionTime = (now + message.control.delay)
    }

    /**
     * Remove the next message from the queue in preparation for adding it
     * to the timed delay queue. Update the time at which we are allowed to
     * trigger the next message.
     */
    override fun removeFirst(): MessageBottle {
        val msg = super.removeFirst()!!
        val now = System.currentTimeMillis()
        if (nextAllowedExecuteTime < now) nextAllowedExecuteTime = now
        if (msg.control.executionTime < nextAllowedExecuteTime) {
            msg.control.executionTime = nextAllowedExecuteTime
        }
        nextAllowedExecuteTime = msg.control.executionTime + msg.control.delay
        return msg
    }

    companion object {
        const val CLSS = "SequentialQueue"
        val LOGGER = Logger.getLogger(CLSS)
    }

    init {
        nextAllowedExecuteTime = System.currentTimeMillis() // Work in milliseconds
        locked = false
    }
}