package chuckcoughlin.bert.common.message


/**
 * This is a nested class for MessageBottles that contains control parameters for processing
 * by an InternalController. Messages that wait on a queue (one queue per limb) and
 * then optionally  wait for a specified delay.
 *
 * @param delay an idle interval between when this message is
 * first placed on the sequential queue and when it should execute (~msecs). Any time
 *  spent waiting on the sequential queue is counted toward the delay
 * ("time served").
*/
data class ExecutionControl(var delay: Long) : Cloneable {
     /*
     * executionTime - the time at which the message leaves the internal controller
     *                 and is sent to the serial controller. For non-serial
     *                 messges this is simply the request creation time
     * delay         - a wait period from a message is ready to be executed and
     *                 when it is actually sent off.
     * to be sent to the Dispatcher. The time is calculated as the message
     * is placed on the timer queue.
     */
    var executionTime : Long   // ~msecs
    var repeatInterval: Long   // ~msecs
    var originalSource: String
    /* While processing within the MotorController attach a serial message response count to the
    * request so that we can determine when the response is complete.
    */
    var responseCount:MutableMap<String, Int>
    var shouldRepeat: Boolean

    override public fun clone(): ExecutionControl {
        val copy = ExecutionControl(delay)
        copy.executionTime  = executionTime   // Current time
        copy.repeatInterval = repeatInterval
        copy.originalSource = originalSource
        copy.responseCount  = mutableMapOf<String, Int>()
        for(key in responseCount.keys) {
            copy.responseCount[key] = responseCount[key]!!.toInt()
        }
        copy.shouldRepeat   = shouldRepeat
        return copy
    }

    override fun toString(): String {
        return String.format("%s: message from %s expires in %d ms",
            CLSS,originalSource,executionTime - (System.currentTimeMillis()) )
    }

    private val CLSS = "ExecutionControl"
    var id: Long = 0 // Sequential id for messages
    @get:Synchronized
    private val nextId: Long
        get() = ++id

    init {
        executionTime  = System.currentTimeMillis()
        repeatInterval = 0
        responseCount  = mutableMapOf<String, Int>()
        shouldRepeat = false
        originalSource = BottleConstants.NO_SOURCE
        id = nextId
    }
}