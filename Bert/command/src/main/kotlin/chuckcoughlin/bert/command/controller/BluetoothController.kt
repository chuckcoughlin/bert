/**
 * Copyright 2022-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command.controller

import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.controller.ControllerType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageType
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.speech.process.MessageTranslator
import chuckcoughlin.bert.speech.process.StatementParser
import java.util.logging.Logger

/**
 * The Bluetooth controller handles input/output to/from an Android tablet via
 * the blueserverd daemon. The tablet handles speech-to-text and text-to-speech.
 * This extends SocketController to handle translation of MessageBottle objects
 * to and from the simple text messages recognized by the tablet.
 */
class BluetoothController(launcher: MessageHandler?, port: Int) :
    SocketController(launcher, ControllerType.TABLET.name, "localhost", port), Controller {
    private val LOGGER = Logger.getLogger(CLSS)
    private val parser: StatementParser
    private val translator: MessageTranslator
    private var suppressingErrors: Boolean

    /**
     * Constructor: For this connection, we act as a client.
     * @param launcher the parent application
     * @param port for socket connection
     */
    init {
        parser = StatementParser()
        translator = MessageTranslator()
        suppressingErrors = false
    }

    fun start() {
        val rdr = BluetoothBackgroundReader(socket)
        runner = Thread(rdr)
        runner.start()
    }

    /**
     * Format a request from the spoken text arriving from the tablet.
     * Forward on to the launcher.
     */
    fun receiveRequest(request: MessageBottle) {
        launcher.handleRequest(request)
    }

    /**
     * Extract text from the message and forward on to the tablet formatted appropriately.
     * For straight replies, the text is expected to be understandable, an error message or a value that can
     * be formatted into plain English.
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
    private fun handleImmediateResponse(request: MessageBottle) {
        if (!request.type.equals(RequestType.PARTIAL)) {
            if (!suppressingErrors) receiveResponse(request) else {
                suppressingErrors = true // Suppress replies to consecutive syntax errors
                val text: String = translator.messageToText(request)
                LOGGER.info(String.format("%s.SuppressedErrorMessage: %s", CLSS, text))
            }
        }
    }
    // ===================================== Background Reader ==================================================
    /**
     * Perform a blocking read as a background thread. The specified socket
     * connects to the "blueserverd" daemon. We are receiving requests from
     * the tablet, but have connected as if we were a client.
     */
    inner class BluetoothBackgroundReader(s: NamedSocket) : Runnable {
        private val sock: NamedSocket

        init {
            sock = s
        }

        /**
         * Forever ...
         * 1) Read request from socket
         * 2) Analyze text and convert into MessageBottle
         * 3) Invoke callback method on launcher
         *
         * There is only one kind of message that we recognize. Anything
         * else is an error.
         */
        override fun run() {
            sock.create()
            sock.startup()
            notifyChangeListeners(sock.name, SocketStateChangeEvent.READY)
            while (!Thread.currentThread().isInterrupted) {
                var msg: MessageBottle? = null
                var text: String? = sock.readLine() // Strips trailing new-line
                if (text == null || text.isEmpty()) {
                    try {
                        Thread.sleep(CLIENT_READ_ATTEMPT_INTERVAL) // A read error has happened, we don't want a hard loop
                        continue
                    } catch (ignore: InterruptedException) {
                    }
                } else if (text.length > BottleConstants.HEADER_LENGTH) {
                    val hdr = text.substring(0, BottleConstants.HEADER_LENGTH - 1)
                    if (hdr.equals(MessageType.MSG.name, ignoreCase = true)) {
                        // Strip header then translate the rest.
                        try {
                            text = text.substring(BottleConstants.HEADER_LENGTH)
                            LOGGER.info(java.lang.String.format("%s parsing: %s", sock.getName(), text))
                            msg = parser.parseStatement(text)
                        }
                        catch (ex: Exception) {
                            msg = MessageBottle()
                            msg.type = RequestType.NOTIFICATION
                            msg.error = String.format("Parse failure (%s) on: %s", ex.localizedMessage, text)
                        }
                    }
                    else if (hdr.equals(MessageType.LOG.name(), ignoreCase = true)) {
                        LOGGER.info(java.lang.String.format("%s: %s", sock.getName(), text))
                        continue
                    }
                    else {
                        msg = MessageBottle()
                        msg.type = RequestType.NOTIFICATION
                        msg.error = String.format("Message has an unrecognized prefix (%s)", text)
                    }
                } else {
                    msg = MessageBottle()
                    msg.type = RequestType.NOTIFICATION
                    msg.error = String.format("Received a short message from the tablet (%s)", text)
                }
                if (msg == null) break // This happens on shutdown - I don't know how
                msg.assignSource(ControllerType.COMMAND.name())
                if (msg.type.equals(RequestType.NOTIFICATION) ||
                    msg.type.equals(RequestType.NONE) ||
                    msg.type.equals(RequestType.PARTIAL) || msg.error != null ) {
                    handleImmediateResponse(msg)
                }
                else {
                    suppressingErrors = false
                    receiveRequest(msg)
                }
            }
            LOGGER.info(java.lang.String.format("BluetoothBackgroundReader,%s stopped", sock.getName()))
        }
    }

    companion object {
        private const val CLSS = "BluetoothController"
    }
}