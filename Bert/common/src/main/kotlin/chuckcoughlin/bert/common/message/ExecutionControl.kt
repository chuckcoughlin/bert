package chuckcoughlin.bert.common.message


/**
 * This is a nested class for MessageBottles containing cortrol parameters as they are processed
 * by an InternalController. The processing comes in two flavors - messages that wait on a queue and
 * those wait on a timer. The queued messages may optionally have a timed delay as well.
 */
class ExecutionControl {
    var controller: String   // Name of controller to handle request. Important for serial controllers
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
    var originalSource: String
    /* While processing within the MotorController attach a serial message response count to the
    * request so that we can determine when the response is complete.
    */
    var responseCount:Int
    var shouldRepeat: Boolean

    override fun toString(): String {
        return String.format("%s: message from %s expires in %d ms",
            CLSS,originalSource,executionTime - System.nanoTime() / 1000000 )
    }

    private val CLSS = "ExecutionControl"
    var id: Long = 0 // Sequential id for messages
    @get:Synchronized
    private val nextId: Long
        get() = ++id

    init {
        controller  = BottleConstants.NO_CONTROLLER
        delay   =  BottleConstants.NO_DELAY
        executionTime  = 0
        repeatInterval = 0
        responseCount  = 0
        shouldRepeat = false
        originalSource = BottleConstants.NO_SOURCE
        id = nextId
    }
}