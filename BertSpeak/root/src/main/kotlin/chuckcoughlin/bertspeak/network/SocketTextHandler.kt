/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.command

import android.util.Log
import chuckcoughlin.bertspeak.common.MessageType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * The SocketMessageHandler handles input/output to/from an Android tablet via
 * a socket interface. The tablet handles speech-to-text and text-to-speech.
 * The robot system is the server.
 * @param sock for socket connection
 */
class SocketTextHandler(sock: Socket)  {
    private val socket = sock
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
    fun writeSocket(txt: String) :Boolean {
        var success:Boolean = true
        var text = txt.trim { it <= ' ' }
        var mtype = MessageType.ANS
        if( text.isNotEmpty()) {
            try {
                val msgtxt = String.format("%s:%s", mtype.name, text)
                if( DEBUG ) Log.i(CLSS,String.format("TABLET WRITE: %s.", msgtxt))
                output.println(msgtxt)
                output.flush()
            }
            catch (ex: Exception) {
                Log.i(CLSS,String.format(" EXCEPTION %s writing. Assume client is closed.", ex.localizedMessage))
                success = false
            }
        }
        return success
    }

    /**
     * The specified socket is assumed to be connected to the robot as a client.
     * Read a line of text from the socket. The read will block and wait for data
     * to appear. If we get a null, then close the socket and re-listen
     * @return a deferred value for use in a select() clause.
     */
    fun readSocket(): String? {
        Log.i(CLSS, String.format("readSocket: reading from socket ..."))
        val text = input.readLine() // Strips trailing new-line
        if( text==null || text.isEmpty() ) {
            Log.i(CLSS,"Received nothing on read. Assume client is closed.")
        }
        else {
            if( DEBUG ) Log.i(CLSS,String.format("TABLET READ: %s.", text))
        }
        return text
    }

    fun close() {
        input.close()
        output.close()

    }


    private val CLSS = "SocketMessageHandler"
    private val CLIENT_READ_ATTEMPT_INTERVAL: Long = 250  // msecs
    private val DEBUG: Boolean

    init {
        DEBUG = true
        input = BufferedReader(InputStreamReader(socket.getInputStream()))
        Log.i(CLSS,"init: opened socket for read")
        output = PrintWriter(socket.getOutputStream(), true)
        Log.i(CLSS,"init: opened socket for write")
    }
}
