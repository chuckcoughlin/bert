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
class CommandMessageHandler(sock: Socket)  {
    private val socket = sock
    private val translator: MessageTranslator
    private val input: BufferedReader
    private val output: PrintWriter
    private val parser: StatementParser
    private var suppressingErrors: Boolean

    /**
     * Extract text from the message and forward on to the tablet formatted appropriately.
     * For straight replies, the text is expected to be understandable, an error message or
     * a value that can be formatted into plain English.
     *
     * @param response
     * @return true on success
     */
    fun sendResponse(response: MessageBottle) :Boolean {
        var success = true
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
                success = false
            }
        }
        return success
    }

    /**
     * Perform a blocking read. The specified socket is assumed to be connected
     * to the tablet which is a client to the robot.
     * @return a deferred value for use in a select() clause.
     */
    fun receiveNetworkInput(): MessageBottle {
        val request: MessageBottle
        val text = readCommand()
        if( text== null ) {
            request = MessageBottle(RequestType.HANGUP)
        }
        else {
            request = processRequest(text)
        }
        return request
    }

    /**
     * Analyze text and use ANTLR to convert into a MessageBottle
     * Send to the dispatcher unless this can be handled locally
     *
     * There are only two kinds of messages (MSG,JSN) that result in actions. Anything
     * else is simply logged. Returning an empty string signals loss of the client.
     * Throw an exception to force closing of the socket.
     */
    @Synchronized private fun processRequest(txt:String) : MessageBottle {
        var msg = MessageBottle(RequestType.NONE)

        if (txt.length > BottleConstants.HEADER_LENGTH) {
            val hdr  = txt.substring(0, BottleConstants.HEADER_LENGTH - 1)
            val text = txt.substring(BottleConstants.HEADER_LENGTH)
            if( DEBUG ) LOGGER.info(String.format("TABLET READ: %s:%s.", hdr,text))
            if (hdr.equals(MessageType.MSG.name, ignoreCase = true)) {
                // We've stripped the header now analyze the rest.
                try {
                    if(DEBUG) LOGGER.info(String.format(" parsing %s: %s", hdr,text))
                    msg = parser.parseStatement(text)
                    if(DEBUG) LOGGER.info(String.format(" returned %s: %s (%s)", msg.type.name,msg.text,msg.error))
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
                msg = parser.parseStatement(text)
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
            msg.error = String.format("Received a short message from the tablet (%s)", txt)
        }
        msg.source = ControllerType.COMMAND.name
        if( msg.type == RequestType.NONE ) { // NONE simply doesn't get processed
            if(!msg.error.equals(BottleConstants.NO_ERROR)) {
                suppressingErrors = true // Suppress replies to consecutive syntax errors
                val text: String = translator.messageToText(msg)
                LOGGER.info(String.format("%s.SuppressedErrorMessage: %s", CLSS, text))
            }
            else {
                suppressingErrors = false
            }
        }
        return msg
    }

    /**
     * This is a substitute for readLine() which I've had much trouble with.
     * @return a line of text. Null indicates a closed stream
     */

    @Synchronized fun readCommand():String? {
        var text = StringBuffer()
        while(true) {
            val ch = input.read()
            if( ch<0 ) {
                return null
            }
            else if(ch==NL || ch==CR) {
                break
            }
            if(DEBUG) LOGGER.info(String.format("%s.readCommand: %c (%d)", CLSS, ch.toChar(),ch))
            text.append(ch.toChar())
        }
        return text.toString()
    }
    /* Send text directly to the socket with newline */
    fun sendText(text:String) {
        if(DEBUG) LOGGER.info(String.format("%s.sendText: %s.", CLSS, text))
        output.println(text)
        output.flush()
    }

    private val CLSS = "CommandMessageHandler"
    private val CLIENT_READ_ATTEMPT_INTERVAL: Long = 250  // msecs
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)
    private val CR = 13
    private val NL = 10

    init {
        translator = MessageTranslator()
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_COMMAND)
        input = BufferedReader(InputStreamReader(socket.getInputStream()))
        LOGGER.info(String.format("%s.startup: opened socket for read",CLSS))
        output = PrintWriter(socket.getOutputStream(), true)
        LOGGER.info(String.format("%s.startup: opened socket for write",CLSS))
        suppressingErrors = false
        parser = StatementParser()
    }
}