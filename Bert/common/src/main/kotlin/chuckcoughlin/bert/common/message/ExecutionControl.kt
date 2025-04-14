package chuckcoughlin.bert.common.message

import chuckcoughlin.bert.common.controller.ControllerType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ModuleLayer.Controller
import java.util.logging.Logger


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
     *                 messages this is simply the request creation time
     * delay         - a wait period from a message is ready to be executed and
     *                 when it is actually sent off.
     * to be sent to the Dispatcher. The time is calculated as the message
     * is placed on the timer queue.
     */
    var executionTime : Long   // ~msecs
    var repeatInterval: Long   // ~msecs
    var originalSource: ControllerType
    var shouldRepeat: Boolean
    /* While processing within the MotorControllers attach a serial message response count
     * to the request so that we can determine when the responses are complete for each
     * controller.
    */
    private var responseCount:MutableMap<String, Int>
    private var mutex: Mutex

    suspend fun decrementResponseCountForController(controller:String) =
        mutex.withLock {
            if(responseCount[controller]==null) {
                LOGGER.warning(String.format("%s.decrementResponseCountForController: Decrekment for %s requested before being set",CLSS,controller))
                0
            }
            else {
                responseCount[controller] = responseCount[controller]!! - 1
                responseCount[controller]!!
            }
        }

    suspend fun getResponseCountForController(controller:String) =
        mutex.withLock {
            if(responseCount[controller]==null) {
                LOGGER.warning(String.format("%s.getResponseCount: Response for %s requested before being set",CLSS,controller))
                0
            }
            else {
                responseCount[controller]!!
            }
        }

    suspend fun setResponseCountForController(controller:String,count:Int) {
        mutex.withLock {
            responseCount[controller] = count
        }
    }

    // Note: Mutex is not cloned
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
    private val LOGGER = Logger.getLogger(CLSS)

    var id: Long = 0 // Sequential id for messages
    @get:Synchronized
    private val nextId: Long
        get() = ++id

    init {
        executionTime  = System.currentTimeMillis()
        repeatInterval = 0
        responseCount  = mutableMapOf<String, Int>()
        shouldRepeat = false
        originalSource = ControllerType.UNDEFINED
        id = nextId
        mutex = Mutex()
    }
}