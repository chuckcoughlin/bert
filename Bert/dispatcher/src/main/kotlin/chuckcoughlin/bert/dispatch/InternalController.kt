/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
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
 * The internal controller is used to implement delays in messages destined for the
 * motor controllers. The delays are both configured and designed to prevent commands
 * being sent too quickly. All messages are returned to the Dispatcher (which then
 * distributes them to the intended targets).
 *
 * In the future this controller will be used to delay movements that interfere with
 * other movements in progress, or stop movements that may cause collisions between
 * separate appendages.
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
    private val queues : MutableMap<Limb,SequentialQueue>
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
                        if(msg.type== RequestType.COMMAND)
                            LOGGER.info(String.format("%s.execute received: %s %s", CLSS, msg.type.name,msg.command.name))
                        else if(msg.type== RequestType.EXECUTE_POSE)
                            LOGGER.info(String.format("%s.execute received: %s (%s %2.0f)", CLSS, msg.type.name,msg.arg,msg.value))
                        else
                            LOGGER.info(String.format("%s.execute received: %s", CLSS, msg.type.name))
                    }
                    // The motor controller is ready to accept another command
                    if(msg.type.equals(RequestType.READY) ) {
                        SequentialQueue.ready = true
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
            for( queue in queues.values ) {
                queue.shutdown()
            }
        }
    }

    // Preprocess the message. It may result in several precursor requests. Messages with BITBUCKET as the
    // source will not produce a response to the original requester.
    suspend private fun handleRequest(request: MessageBottle) {
        // Find limb for a single joint
        if( request.limb==Limb.NONE && request.joint!=Joint.NONE ) {
            request.limb = RobotModel.limbsByJoint[request.joint]!!
        }
        // Read joint positions before freezing to update the current internal status directory.
        if(request.type==RequestType.SET_MOTOR_PROPERTY &&
            request.jointDynamicProperty == JointDynamicProperty.STATE &&
            request.value == ConfigurationConstants.ON_VALUE    )  {

            val msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.ANGLE
            msg.joint = request.joint
            msg.limb =  request.limb
            msg.source = ControllerType.BITBUCKET
            msg.control.delay =250 // 1/4 sec delay
            dispatchMessage(msg)
        }
        else if (request.type.equals(RequestType.SET_LIMB_PROPERTY) &&
            request.jointDynamicProperty.equals(JointDynamicProperty.STATE)  &&
            request.value == ConfigurationConstants.ON_VALUE    ) {

            val msg = MessageBottle(RequestType.READ_MOTOR_PROPERTY)
            msg.jointDynamicProperty = JointDynamicProperty.ANGLE
            msg.limb = request.limb
            msg.source = ControllerType.BITBUCKET
            dispatchMessage(msg)
        }
        else if (request.type == RequestType.EXECUTE_POSE ) {
            LOGGER.info(String.format("%s.handleRequest %s pose = %s %02f",
                        CLSS,request.type.name,request.arg,request.value))
            distributePose(request)
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
                msg.control.delay = pose.index.toLong()
                msg.source = ControllerType.BITBUCKET
                distributePose(msg)   // All responses will got to the bit bucket
            }
        }

        // Finally process the original message
        dispatchMessage(request)
    }

    // Duplicate the pose message for each limb. The final NONE limb
    // is the original message and serves to synchronize the pose
    private suspend fun distributePose(request:MessageBottle) {
        for( limb in Limb.values()) {
            var msg : MessageBottle
            if(!limb.equals(Limb.NONE)) {
                msg = request.clone()

                msg.source = ControllerType.BITBUCKET
            }
            else {
                msg = request
            }
            msg.limb = limb
            dispatchMessage(msg)
        }

    }

    /**
     * Forward to the dispatcher for actual processing (presumably by the MotorController).
     * Retain the source as the original source of the message. This allows the dispatcher to
     * forward the response to the proper requester.
     * @param msg
     */
    override suspend fun dispatchMessage(msg:MessageBottle) {
        if (DEBUG) LOGGER.info(String.format("%s.dispatchMessage sending to dispatcher: %s (%s)", CLSS, msg.type.name,msg.arg))
        // Mark dispatch time on motors
        val queue = queues[msg.limb]!!
        queue.addLast(msg)
    }


    private fun initializeQueues() {
        for( limb in Limb.values() ) {
            val queue = SequentialQueue(limb,toDispatcher,configurationsForLimb(limb))
            queues[limb] = queue
        }
    }
    private fun configurationsForLimb(limb: Limb): Map<Joint,MotorConfiguration> {
        val map: MutableMap<Joint,MotorConfiguration> = mutableMapOf<Joint,MotorConfiguration>()
        for (joint in RobotModel.motorsByJoint.keys) {
            val mc = RobotModel.motorsByJoint[joint]!!
            if (mc.limb.equals(limb) || limb.equals(Limb.NONE)) {
                map[joint]=mc
            }
        }
        return map
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
        queues = mutableMapOf<Limb,SequentialQueue>()
        initializeQueues()
        SequentialQueue.ready = true
    }
}