/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.dispatch
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageHandler
import chuckcoughlin.bert.common.model.JointDefinitionProperty
import chuckcoughlin.bert.control.controller.QueueName
import chuckcoughlin.bert.control.controller.SequentialQueue
import chuckcoughlin.bert.control.message.InternalMessageHolder
import java.util.logging.Logger

/**
 * A timer controller accepts a RequestBottle and submits it to the parent
 * Dispatcher on a clocked basis. The initial implementation handles only a
 * single request.
 */
class InternalController(launcher: MessageHandler) {
    private val LOGGER = Logger.getLogger(CLSS)
    private val dispatcher: MessageHandler
    private val timedQueue: TimedQueue
    private val sequentialQueues: MutableMap<QueueName, SequentialQueue>
    private val pendingMessages: MutableMap<Long, InternalMessageHolder>


    /**
     * All requests to the InternalController are generated by the dispatcher or dispatcher's solver.
     * This class determines how we handle them. If there is no queue specified
     * then the message is placed directly on the timer queue.
     * @param request incoming message holder
     */
    @Synchronized
    fun receiveRequest(holder: InternalMessageHolder) {
        val now = System.nanoTime() / 1000000
        holder.executionTime = now + holder.delay
        val qn: QueueName = holder.queue
        var queue: SequentialQueue? = null
        if (qn != null) queue = sequentialQueues[qn]
        if (queue != null) {
            LOGGER.info(
                String.format("%s.receiveRequest %s on %s (%s)", CLSS, holder.getMessage().type,
                    holder.queue.name, if (queue.inProgress) "IN PROGRESS" else "IDLE"
                )
            )
            queue.addLast(holder)
            if (!queue.inProgress) {
                holder = queue.removeFirst()
                queue.inProgress = true
                sendToTimerQueue(queue, holder) // Just in case there's a required delay
            }
        }
        else {
            sendToTimerQueue(null, holder)
        }
    }

    /**
     * The dispatcher has received a response to one of our requests and has
     * forwarded it here. Possible dispositions:
     * 1) If message was from a queue, then we trigger processing of the next message on the queue, if any
     * 2) If the message was marked for repeating, then place it on the timer queue
     * 3) Discard the message,
     */
    fun receiveResponse(msg: MessageBottle) {
        var holder: InternalMessageHolder? = pendingMessages[msg.id]
        if (holder != null) {
            pendingMessages.remove(JointDefinitionProperty.ID.id)
            // Reinstate the original source in the message so the dispatcher can route appropriately	
            holder.reinstateOriginalSource()

            //LOGGER.info(String.format("%s.receiveResponse(%d) %s",CLSS,holder.getMessage().getId(),
            //		holder.getMessage().fetchRequestType().name()));
            val qn: QueueName = holder.queue
            var queue: SequentialQueue? = null
            if (qn != null) queue = sequentialQueues[qn]
            if (queue != null) {
                if (queue.isEmpty()) {
                    LOGGER.info(String.format("%s.receiveResponse(%d) %s on %s (empty)",
                        CLSS, holder.ID.id,holder.message.type.name, qn.name
                        )
                    )
                    queue.inProgress = false
                }
                else {
                    queue.inProgress = true
                    LOGGER.info(String.format("%s.receiveResponse(%d) %s on %s (%d queued)",
                            CLSS, holder.ID.id,holder.message.type.name, qn.name, queue.size
                        )
                    )
                    holder = queue.removeFirst()
                    sendToTimerQueue(queue, holder) // Just in case there's a required delay
                }
            }
            else {
                if (holder.shouldRepeat()) {
                    val now = System.nanoTime() / 1000000 // Work in milliseconds
                    holder.executionTime = (now + holder.repeatInterval)
                    sendToTimerQueue(null, holder)
                }
            }
            dispatcher.handleResponse(msg) // Will reply to the original source
        }
        else {
            LOGGER.info(
                String.format("%s.receiveResponse(%d) %s: not on pending queue for %s",
                    CLSS,msg.id,msg.type.name,msg.source)
            )
        }
    }

    /**
     * Called by the timer queue once the message is ready to execute. Forward to the
     * dispatcher for actual processing. Mark the source as INTERNAL so
     * that the dispatcher knows to return the message here once processing is complete.
     * @param holder
     */
    @Synchronized
    fun dispatch(holder: InternalMessageHolder?) {
        holder.message.source = ControllerType.INTERNAL.name
        dispatcher.handleRequest(holder.message)
    }

    fun start() {
        timedQueue.start()
    }

    fun stop() {
        timedQueue.stop()
    }

    /**
     * As far as the sequential queue is concerned, a message is pending
     * from the time it is placed on the timer queue until it returns
     * as a response from the Dispatcher
     * @param queue queue from which the message originated.
     * @param holder container for the message to be sent
     */
    private fun sendToTimerQueue(queue: SequentialQueue?, holder: InternalMessageHolder?) {
        if (queue != null) {
            pendingMessages[holder.ID.id] = holder
            // LOGGER.info(String.format("%s.sendToTimerQueue: %d from %s",CLSS,holder.getMessage().getId(),holder.getQueue().name()));
            queue.inProgress = true
        }
        timedQueue.addMessage(holder)
    }

    companion object {
        protected const val CLSS = "InternalController"
    }
    init {
        dispatcher = launcher
        timedQueue = TimedQueue(this)
        sequentialQueues = HashMap()
        pendingMessages = HashMap<Long, InternalMessageHolder?>()
        sequentialQueues[QueueName.GLOBAL] = SequentialQueue()
        sequentialQueues[QueueName.HEAD] = SequentialQueue()
        sequentialQueues[QueueName.LEFT_ARM] = SequentialQueue()
        sequentialQueues[QueueName.LEFT_LEG] = SequentialQueue()
        sequentialQueues[QueueName.RIGHT_ARM] = SequentialQueue()
        sequentialQueues[QueueName.RIGHT_LEG] = SequentialQueue()
    }
}