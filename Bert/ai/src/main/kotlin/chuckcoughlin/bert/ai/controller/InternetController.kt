/**
 * Copyright 2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 *
 */
package chuckcoughlin.bert.ai.controller

import chuckcoughlin.bert.common.controller.Controller
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import io.ktor.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.logging.Logger

/**
 * Handle requests directed to the internet, specifically Open AI. The internal controller
 * has synchronized requests, so that there is only one active at a time. The OpenAi object
 * is used translate the requests into HTTP.
 *
 * @param req - channel for requests from the parent (motor manager)
 * @param rsp - channel for responses sent to the parent (motor manager)
 */
class InternetController(req: Channel<MessageBottle>,rsp:Channel<MessageBottle>) : Controller {
    @DelicateCoroutinesApi
    private val scope = GlobalScope // For long-running coroutines
    private var running:Boolean
    private var job: Job
    private var parentRequestChannel = req
    private var parentResponseChannel = rsp


    /**
     * Send a system request to initialize OpenAI
     */
    @DelicateCoroutinesApi
    override suspend fun execute() {
        LOGGER.info(String.format("%s.execute: %s initializing OpenAi)",CLSS, controllerName))

        // Simply run in a loop processing AI requests.
        running = true
        job = scope.launch(Dispatchers.IO) {
            while (running) {
                processRequest()
            }
        }
    }

    override suspend fun shutdown() {
        if (running) {
            running = false
            job.cancel()
        }
    }

    /**
     * This method waits for the next request in the inout. It then processes the message synchronously
     * until the response is returned from the OpenAI service (or timeout).
     */
    suspend fun processRequest() {
        val request = parentRequestChannel.receive()    //  Waits for message
        if(DEBUG) {
            LOGGER.info(String.format("%s.processRequest:%s processing %s (%s)",CLSS,
                    controllerName, request.type.name, request.text))
        }

        // For now simply send the request as the response
        LOGGER.info(String.format("%s.processRequest: %s got response", CLSS, controllerName))
        val chatRequest = OpenAI.createChatRequest(request)
        val client: HttpClient = OpenAI.createHttpRequest()
        val response = OpenAI.executeRequest(client,chatRequest)
        val resMsg = OpenAI.updateRequestMessage(request,response)
        client.close()
        parentResponseChannel.send(resMsg)
    }

    // ============================= Private Helper Methods =============================


    private val CLSS = "InternetController"
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    override val controllerName = CLSS
    override val controllerType = ControllerType.INTERNET

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_OPEN_AI)
        running = false
        job = Job()
        LOGGER.info(String.format("%s.init: created...", CLSS))
}
}