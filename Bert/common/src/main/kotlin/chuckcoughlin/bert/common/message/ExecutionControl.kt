package chuckcoughlin.bert.common.message


/**
 * This is a nested class for MessageBottles that contains control parameters for processing
 * by an InternalController. The processing comes in two flavors - messages that wait on a queue and
 * those wait on a timer. The queued messages may optionally have a timed delay as well.
 *
 * @param delay an idle interval between when this message is
 * first placed on the timer queue and when it should execute (~msecs). Any time
 *  spent waiting on the sequential queue is counted toward the delay
 * ("time served").
*/
data class ExecutionControl(var delay: Long) : Cloneable {
     /*
     * The execution time is the earliest time at which this message is allowed
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