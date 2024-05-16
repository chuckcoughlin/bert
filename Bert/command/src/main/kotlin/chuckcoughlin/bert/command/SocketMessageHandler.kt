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
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
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

        if(text.isBlank()) text = response.error
        if( text.isNotEmpty()) {
            try {
                val msgtxt = String.format("%s:%s", mtype.name, text)
                if( DEBUG ) LOGGER.info(String.format("TABLET WRITE: %s.", msgtxt))
                output.println(msgtxt)
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
     * connects to the robot which acts as a server. We have connected as
     * a client.
     *.
     * 1) Read request from socket
     * 2) Analyze text and use ANTLR to convert into a MessageBottle
     * 3) Send to the robot
     *
     * There are only two kinds of messages (MSG,JSN) that result in actions. Anything
     * else is simply logged. Returning an empty string signals loss of the client.
     * Throw an exception to force closing of the socket.
     */
    fun processRequest() : MessageBottle {
        var msg = MessageBottle(RequestType.NONE)
        var text: String? = input.readLine() // Strips trailing new-line
        if (text == null || text.isEmpty()) {
            LOGGER.info(String.format("Received nothing on read. Assume client is closed."))
            connected.complete(false)
        }
        else if (text.length > BottleConstants.HEADER_LENGTH) {
            val hdr = text.substring(0, BottleConstants.HEADER_LENGTH - 1)
            text = text.substring(BottleConstants.HEADER_LENGTH)
            if( DEBUG ) LOGGER.info(String.format("TABLET READ: %s:%s.", hdr,text))
            if (hdr.equals(MessageType.MSG.name, ignoreCase = true)) {
                // We've stripped the header now analyze the rest.
                try {
                    LOGGER.info(String.format(" parsing %s: %s", hdr,text))
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
            // For now simply log responses from the tablet.
            else if (hdr.equals(MessageType.ANS.name, ignoreCase = true)) {
                LOGGER.info(String.format("Tablet ANS: %s",text))
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
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_COMMAND)
        parser = StatementParser()
        translator = MessageTranslator()
        suppressingErrors = false
        input = BufferedReader(InputStreamReader(socket.getInputStream()))
        LOGGER.info(String.format("%s.startup: opened socket for read",CLSS))
        output = PrintWriter(socket.getOutputStream(), true)
        LOGGER.info(String.format("%s.startup: opened socket for write",CLSS))
    }
}