/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.dispatch
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.controller.MessageController
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.*
import chuckcoughlin.bert.sql.db.Database
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.logging.Logger

/**
 * The internal controller is used to synchronize messages to subsystems that
 * cannot handle requests in parallel, like the serial  motor controller and
 * Chat GPT interface. When a message is sent to the Dispatcher, the next queued
 * message is held back until a READY message is received from the service.
 * The queue can also be used to implement programmed delays.
 *
 * Messages are executed in the order they are received.
 */
@DelicateCoroutinesApi
class InternalController(req: Channel<MessageBottle>,rsp: Channel<MessageBottle>) :
                                                        MessageController {
    private val scope = GlobalScope     // For long-running coroutines
    private var toDispatcher   = req    // Internal->Dispatcher  (dispatcher gets results)
    private var fromDispatcher = rsp    // Dispatcher->Internal
    private var running:Boolean
    private var index:Long          // Sequence of a message
    private var job:Job
    private val motorQueue : SequentialQueue
    private val internetQueue : SequentialQueue
    @DelicateCoroutinesApi
    override suspend fun execute() {
        if (!running) {
            LOGGER.info(String.format("%s.execute: started...", CLSS))
            running = true
            /* Coroutine to accept requests from the Dispatcher.
             * Requests are processed in the order they are received.
             */
            job = scope.launch(Dispatchers.IO) {
                LOGGER.info(String.format("%s.execute: launched...", CLSS))
                while (running) {
                    val msg = fromDispatcher.receive()
                    if (DEBUG) {
                        if( msg.type==RequestType.COMMAND)
                            LOGGER.info(String.format("%s.execute received: %s %s", CLSS, msg.type.name,msg.command.name))
                        else if( msg.type==RequestType.EXECUTE_POSE)
                            LOGGER.info(String.format("%s.execute received: %s (%s %2.0f)", CLSS, msg.type.name,msg.arg,msg.values[0]))
                        else
                            LOGGER.info(String.format("%s.execute received: %s", CLSS, msg.type.name))
                    }
                    // The motor controller is ready to accept another command - the message does not propagate
                    if( msg.type==RequestType.READY && msg.source==ControllerType.MOTOR) {
                        motorQueue.markReady()
                    }
                    else if( msg.type==RequestType.READY && msg.source==ControllerType.INTERNET) {
                        internetQueue.markReady()
                    }
                    else {
                        handleRequest(msg)
                    }
                }
            }
        }
        else {
            LOGGER.warning(String.format("%s.execute: attempted to start, but already running...", CLSS))
        }
    }

    override suspend fun shutdown() {
        if (DEBUG) println(String.format("%s.shutdown: shutting down ... ", CLSS))
        if( running ) {
            running = false
            job.cancel()
            if (DEBUG) println(String.format("%s.shutdown: cancelled job ", CLSS))
            motorQueue.shutdown()
            internetQueue.shutdown()
        }
    }

    // Preprocess the message. It may result in several precursor requests. Messages with BITBUCKET as the
    // source will not produce a response to the original requester.
    suspend private fun handleRequest(request: MessageBottle) {

        // Read joint positions before freezing to update the current internal status directory.
        if(request.type==RequestType.SET_MOTOR_PROPERTY &&
            request.jointDynamicProperty == JointDynamicProperty.STATE &&
            request.values[0]==ConfigurationConstants.ON_VALUE    )  {

            val msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.ANGLE
            msg.joint = request.joint
            msg.limb =  request.limb
            msg.source = ControllerType.BITBUCKET
            msg.control.delay =250 // 1/4 sec delay
            dispatchMessage(msg)
        }
        // Make sure the motor is engaged before moving
        else if(request.type==RequestType.SET_MOTOR_PROPERTY &&
            request.jointDynamicProperty == JointDynamicProperty.ANGLE   )  {

            val msg = request.clone()
            msg.jointDynamicProperty = JointDynamicProperty.STATE
            msg.values[0] = ConfigurationConstants.ON_VALUE
            msg.source = ControllerType.BITBUCKET
            msg.control.delay =250 // 1/4 sec delay
            dispatchMessage(msg)
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY) &&
            request.jointDynamicProperty.equals(JointDynamicProperty.STATE)  &&
            request.values[0] == ConfigurationConstants.ON_VALUE    ) {

            val msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.ANGLE
            msg.limb = request.limb
            msg.source = ControllerType.BITBUCKET
            dispatchMessage(msg)
        }
        else if (request.type == RequestType.EXECUTE_POSE ) {
            LOGGER.info(String.format("%s.handleRequest %s pose = %s %2.0f (%s)",
                        CLSS,request.type.name,request.arg,request.values[0],request.source))
            request.limb = Limb.NONE   // Just used to synchronize
            if( Database.poseExists(request.arg,request.values[0].toInt())) {
                distributePose(request)
            }
            else {
                request.error = String.format("pose \"%s %d\" does not exist",request.arg,request.values[0].toInt())
            }
        }
        else if (request.type == RequestType.EXECUTE_ACTION ) {
            // An action requires executing a series of poses with intervening delay
            val poseList = Database.getPosesForAction(request.arg)
            LOGGER.info(String.format("%s.handleRequest %s = %s",
                    CLSS,request.type.name,request.arg))
            for(pose in poseList) {
                LOGGER.info(String.format("%s     got %s %d", CLSS,pose.name,pose.index))
                var msg = MessageBottle(RequestType.EXECUTE_POSE)
                msg.arg = pose.name
                msg.values[0] = pose.index.toDouble()
                msg.control.delay = pose.index.toLong()
                msg.source = ControllerType.BITBUCKET
                distributePose(msg)   // All responses will go to the bit bucket
            }
            request.limb = Limb.NONE   // Message is used to synch the MotorGroupController
        }
        else if (request.type == RequestType.RESET ) {
            motorQueue.reset()   // Then proceed to reset controllers
            internetQueue.reset()
        }
        // Finally process the original message
        dispatchMessage(request)
    }

    // Duplicate the pose message for each limb. The NONE limb
    // is the original message and serves to synchronize the pose.
    private suspend fun distributePose(request:MessageBottle) {
        for( limb in Limb.values()) {
            if( limb!=Limb.NONE ) {
                val msg = request.clone()
                msg.limb = limb
                msg.text = ""
                msg.source = ControllerType.BITBUCKET
                dispatchMessage(msg)
            }
        }
    }

    /**
     * Forward to the dispatcher for actual processing (presumably by the MotorController).
     * Retain the source as the original source of the message. This allows the dispatcher to
     * forward the response to the proper requester.
     * @param msg
     */
    override suspend fun dispatchMessage(msg:MessageBottle) {
        if (DEBUG) {
            if( msg.type==RequestType.EXECUTE_POSE ) LOGGER.info(String.format("%s.dispatchMessage: %s (%s %d) on %s",
                                                        CLSS, msg.type.name,msg.arg,msg.values[0].toInt(),msg.limb.name))
            else if( msg.type==RequestType.INTERNET ) LOGGER.info(String.format("%s.dispatchMessage: %s (%s)",
                CLSS, msg.type.name,msg.text))
            else           LOGGER.info(String.format("%s.dispatchMessage: %s - %s %s %s", CLSS, msg.type.name,msg.jointDynamicProperty,msg.joint,msg.limb))
        }
        if(msg.type==RequestType.INTERNET) {
            internetQueue.addLast(msg)
        }
        else {
            motorQueue.addLast(msg)
        }
    }

    private val CLSS = "InternalController"
    private val DEBUG : Boolean
    private val LATENCY = 100L     // Estimated time between dispatch here and receipt by motor
    private val LOGGER = Logger.getLogger(CLSS)
    override val controllerName = CLSS
    override val controllerType = ControllerType.INTERNAL

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_INTERNAL)
        running = false
        index = 0
        job = Job() // Parent job
        motorQueue    = SequentialQueue(toDispatcher)
        internetQueue = SequentialQueue(toDispatcher)
    }
}