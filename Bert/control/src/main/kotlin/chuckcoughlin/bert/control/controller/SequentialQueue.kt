/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.control.controller

import chuckcoughlin.bert.control.message.InternalMessageHolder
import java.util.*
import java.util.logging.Logger

/**
 * Holds a list of requests to be executed in sequence. In general,
 * requests on the same queue affect the same robot sub-chain of motors.
 *
 * The inProgress flag true means that there is a message from the queue
 * currently being executed by the dispatcher. This is used to prevent
 * immediate execution of a new request if inappropriate.
 */
class SequentialQueue : LinkedList<InternalMessageHolder?>() {
    private val LOGGER = Logger.getLogger(CLSS)
    var isInProgress = false
    private var nextAllowedExecuteTime: Long = 0

    /**
     * Constructor:
     * @param launcher the launcher parent process
     */
    init {
        nextAllowedExecuteTime = System.nanoTime() / 1000000 // Work in milliseconds
    }

    /**
     * Add the specified message to the end of the queue. Set the execution time
     * respecting the delay setting.
     */
    override fun addLast(holder: InternalMessageHolder?) {
        super.addLast(holder)
        val now = System.nanoTime() / 1000000
        holder.setExecutionTime(now + holder.getDelay())
    }

    /**
     * Remove the next holder from the queue in preparation for adding it
     * to the timed delay queue. Update the time at which we are allowed to trigger the next message.
     */
    override fun removeFirst(): InternalMessageHolder? {
        val holder = super.removeFirst()!!
        val now = System.nanoTime() / 1000000
        if (nextAllowedExecuteTime < now) nextAllowedExecuteTime = now
        if (holder.executionTime < nextAllowedExecuteTime) {
            holder.executionTime = nextAllowedExecuteTime
        }
        nextAllowedExecuteTime = holder.executionTime + holder.message.getDuration()
        return holder
    }

    companion object {
        private const val serialVersionUID = -3633729383458991404L
        protected const val CLSS = "SequentialQueue"
    }
}