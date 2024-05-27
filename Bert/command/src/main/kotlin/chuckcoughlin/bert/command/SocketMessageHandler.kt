/**
 * Copyright 2022-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageType
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import chuckcoughlin.bert.speech.process.MessageTranslator
import kotlinx.coroutines.*
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
class SocketMessageHandler(sock: Socket)  {
    private val socket = sock
    private val translator: MessageTranslator
    private val input: BufferedReader
    private val output: PrintWriter

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
    @DelicateCoroutinesApi
    fun receiveNetworkInput(): Deferred<String?> =
        GlobalScope.async(Dispatchers.IO) {
            val text: String? = input.readLine() // Strips trailing new-line
            if (text == null || text.isEmpty()) {
                LOGGER.info(String.format("Received nothing on read. Assume client is closed."))
            }
            text
        }

    /* Send text directly to the socket */
    fun sendText(text:String) {
        output.println(text)
        output.flush()
    }

    private val CLSS = "SocketMessageHandler"
    private val CLIENT_READ_ATTEMPT_INTERVAL: Long = 250  // msecs
    private val DEBUG: Boolean
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        translator = MessageTranslator()
        DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_COMMAND)
        input = BufferedReader(InputStreamReader(socket.getInputStream()))
        LOGGER.info(String.format("%s.startup: opened socket for read",CLSS))
        output = PrintWriter(socket.getOutputStream(), true)
        LOGGER.info(String.format("%s.startup: opened socket for write",CLSS))
    }
}