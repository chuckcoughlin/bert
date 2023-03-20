/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command.controller

import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.controller.NamedSocket
import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageType
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.speech.process.MessageTranslator
import chuckcoughlin.bert.speech.process.StatementParser
import java.util.logging.Logger

/**
 * The Bluetooth socket handles input/output to/from an Android tablet via
 * the blueserverd daemon. The tablet handles speech-to-text and text-to-speech.
 * For this connection, we act as a client.
 * @param sock for socket connection
 */
class BluetoothSocket(sock:NamedSocket)  {
    private val socket = sock
    private val parser: StatementParser
    private val translator: MessageTranslator
    private var suppressingErrors: Boolean

    /**
     * Extract text from the message and forward on to the tablet formatted appropriately.
     * For straight replies, the text is expected to be understandable, an error message or
     * a value that can be formatted into plain English.
     *
     * @param response
     */
    fun receiveResponse(response: MessageBottle) {
        var text: String = translator.messageToText(response)
        text = text.trim { it <= ' ' }
        socket.write(String.format("%s:%s", MessageType.ANS.name, text))
    }

    /**
     * The request can be handled immediately without being sent to the
     * dispatcher. A common scenario is a parsing error. For partial messages
     * we simply wait until the next parse.
     */
    private fun handleImmediateRequest(request: MessageBottle) {
        if (!request.type.equals(RequestType.PARTIAL)) {
            if (!suppressingErrors) receiveResponse(request)
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
     * There is only one kind of message that we recognize. Anything
     * else is an error.
     */
    fun receiveRequest() : MessageBottle {
        var msg: MessageBottle = MessageBottle(RequestType.NONE)
        while(true) {
            var text: String? = socket.readLine() // Strips trailing new-line
            if (text == null || text.isEmpty()) {
                try {
                    Thread.sleep(CLIENT_READ_ATTEMPT_INTERVAL) // A read error has happened, we don't want a hard loop
                    continue
                }
                catch (ignore: InterruptedException) {
                }
            }
            else if (text.length > BottleConstants.HEADER_LENGTH) {
                val hdr = text.substring(0, BottleConstants.HEADER_LENGTH - 1)
                if (hdr.equals(MessageType.MSG.name, ignoreCase = true)) {
                    // Strip header then translate the rest.
                    try {
                        text = text.substring(BottleConstants.HEADER_LENGTH)
                        LOGGER.info(String.format("%s parsing: %s", socket.name, text))
                        msg = parser.parseStatement(text)
                    }
                    catch (ex: Exception) {
                        msg = MessageBottle(RequestType.NOTIFICATION)
                        msg.error = String.format("Parse failure (%s) on: %s", ex.localizedMessage, msg.type.name)
                    }
                    break
                }
                else if (hdr.equals(MessageType.LOG.name, ignoreCase = true)) {
                    LOGGER.info(String.format("%s: %s", socket.name, text))
                    continue
                }
                else {
                    msg = MessageBottle(RequestType.NOTIFICATION)
                    msg.error = String.format("Message has an unrecognized prefix (%s)", text)
                    break
                }
            }
            else {
                msg = MessageBottle(RequestType.NOTIFICATION)
                msg.error = String.format("Received a short message from the tablet (%s)", text)
                break
            }
            msg.source = ControllerType.COMMAND.name
            if (msg.type == RequestType.NOTIFICATION ||
                msg.type == RequestType.NONE ||
                msg.type == RequestType.PARTIAL ||
                !msg.error.equals(BottleConstants.NO_ERROR) ) {
                handleImmediateRequest(msg)
                break
            }
            else {
                suppressingErrors = false
                msg = receiveRequest()
                break
            }
        }
        LOGGER.info(String.format("BluetoothBackgroundReader,%s stopped", socket.name))
        return msg
    }


    private val CLSS = "BluetoothSocket"
    private val CLIENT_READ_ATTEMPT_INTERVAL: Long = 250  // msecs
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        parser = StatementParser()
        translator = MessageTranslator()
        suppressingErrors = false
    }
}