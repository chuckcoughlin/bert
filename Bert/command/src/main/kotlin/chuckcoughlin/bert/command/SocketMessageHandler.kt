/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command

import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageType
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.speech.process.MessageTranslator
import chuckcoughlin.bert.speech.process.StatementParser
import kotlinx.coroutines.CompletableDeferred
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.logging.Logger

/**
 * The SocketMessageHandler handles input/output to/from an Android tablet via
 * a socket interface. The tablet handles speech-to-text and text-to-speech.
 * The robot system is the server.
 * @param sock for socket connection
 */
class SocketMessageHandler(sock: Socket,cxn:CompletableDeferred<Boolean>)  {
    private val socket = sock
    private val connected = cxn
    private val parser: StatementParser
    private val translator: MessageTranslator
    private var suppressingErrors: Boolean
    private val input: BufferedReader
    private val output: PrintWriter

    /**
     * Extract text from the message and forward on to the tablet formatted appropriately.
     * For straight replies, the text is expected to be understandable, an error message or
     * a value that can be formatted into plain English.
     *
     * @param response
     */
    fun sendResponse(response: MessageBottle) {
        var text: String = translator.messageToText(response)
        text = text.trim { it <= ' ' }
        var mtype = MessageType.ANS
        if( response.type.equals(RequestType.LIST_MOTOR_PROPERTIES) ||
            response.type.equals(RequestType.LIST_MOTOR_PROPERTY  ) ) mtype = MessageType.JSN

        if( text.isNotEmpty()) {
            try {
                val msgtxt = String.format("%s:%s", mtype.name, text)
                LOGGER.info(String.format("writing to tablet: %s", msgtxt))
                output.write(msgtxt)
                output.flush()
            }
            catch (ex: Exception) {
                LOGGER.info(String.format(" EXCEPTION %s writing. Assume client is closed.", ex.localizedMessage))
                connected.complete(false)
            }
        }
    }

    /**
     * The request can be handled immediately without being sent to the
     * dispatcher. A common scenario is a parsing error. For partial messages
     * we simply wait until the next parse.
     */
    private fun handleImmediateRequest(request: MessageBottle) {
        if (!request.type.equals(RequestType.PARTIAL)) {
            if (!suppressingErrors) sendResponse(request)
            else {
                suppressingErrors = true // Suppress replies to consecutive syntax errors
                val text: String = translator.messageToText(request)
                LOGGER.info(String.format("%s.SuppressedErrorMessage: %s", CLSS, text))
            }
        }
    }
    /**
     * Perform a blocking read as a background thread. The specified socket
     * connects to the "blueserverd" daemon. We are receiving requests from
     * the tablet, but have connected as if we were a client.
     *
     * Forever ...
     * 1) Read request from socket
     * 2) Analyze text and use ANTLR to convert into MessageBottle
     * 3) Parent sends to the dispatcher
     *
     * There is only two kinds of messages (MSG,JSN) that we recognize. Anything
     * else is an error. Returning an empty string signals loss of the client.
     * Throw an exception to force closing of the socket.
     */
    fun receiveRequest() : MessageBottle {
        var msg: MessageBottle = MessageBottle(RequestType.NONE)
        var text: String? = input.readLine() // Strips trailing new-line
        if (text == null || text.isEmpty()) {
            LOGGER.info(String.format(" received nothing on read. Assume client is closed."))
            connected.complete(false)
        }
        else if (text.length > BottleConstants.HEADER_LENGTH) {
            val hdr = text.substring(0, BottleConstants.HEADER_LENGTH - 1)
            text = text.substring(BottleConstants.HEADER_LENGTH)
            if (hdr.equals(MessageType.MSG.name, ignoreCase = true)) {
                // Strip header then translate the rest.
                try {
                    LOGGER.info(String.format(" parsing MSG: %s", text))
                    msg = parser.parseStatement(text)
                }
                catch (ex: Exception) {
                    msg = MessageBottle(RequestType.NOTIFICATION)
                    msg.error = String.format("Parse failure (%s) on: %s", ex.localizedMessage, msg.type.name)
                }
            }
            else if (hdr.equals(MessageType.JSN.name, ignoreCase = true)) {
                LOGGER.info(String.format(" parsing JSN: %s", text))
                msg.error = String.format("JSON messages are not recognized from the tablet")
            }
            // Simply send tablet log messages to our logger
            else if (hdr.equals(MessageType.LOG.name, ignoreCase = true)) {
                LOGGER.info(String.format("Tablet LOG: %s",text))
            }
            else {
                msg = MessageBottle(RequestType.NOTIFICATION)
                msg.error = String.format("Message has an unrecognized prefix (%s)", hdr)
            }
        }
        else {
            msg = MessageBottle(RequestType.NOTIFICATION)
            msg.error = String.format("Received a short message from the tablet (%s)", text)
        }
        msg.source = ControllerType.COMMAND.name
        if (msg.type == RequestType.NOTIFICATION ||
            msg.type == RequestType.NONE ||
            msg.type == RequestType.PARTIAL ||
            !msg.error.equals(BottleConstants.NO_ERROR) ) {
            handleImmediateRequest(msg)
        }
        else {
            suppressingErrors = false
        }

        LOGGER.info(String.format("%s stopped", CLSS))
        return msg
    }


    private val CLSS = "SocketMessageHandler"
    private val CLIENT_READ_ATTEMPT_INTERVAL: Long = 250  // msecs
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        parser = StatementParser()
        translator = MessageTranslator()
        suppressingErrors = false
        input = BufferedReader(InputStreamReader(socket.getInputStream()))
        LOGGER.info(String.format("%s.startup: opened socket for read",CLSS))
        output = PrintWriter(socket.getOutputStream(), true)
        LOGGER.info(String.format("%s.startup: opened socket for write",CLSS))
    }
}