/**
 * Copyright 2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.network

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
class SocketTextHandler(sock: Socket) {
    private val socket = sock
    private val input: BufferedReader
    private val output: PrintWriter

    /**
     * Send a text request or status to the robot formatted appropriately.
     * The robot expects a message in the form TYP: payload
     *
     * We detect socket closure on input.
     *
     * @param txt
     * @return true on success
     */

    fun writeSocket(txt: String) {
        var text = txt.trim()
        if(text.isNotEmpty()) {
            try {
                if(DEBUG) Log.i(CLSS, String.format("TABLET WRITE: %s.", text))
                output.println(text)
                output.flush()
            }
            catch(ex: Exception) {
                Log.i(CLSS, String.format(" EXCEPTION %s writing. Assume client is closed.", ex.localizedMessage))
            }
        }
    }

    /**
     * The member socket is assumed to be connected to the robot as a client.
     * Read a line of text from the socket. The read will block and wait for data
     * to appear. If we get a null, then close the socket and re-listen
     * @return a deferred value for use in a select() clause.
     */
    fun readSocket(): String {
        Log.i(CLSS, String.format("readSocket: reading from socket ..."))
        val text = readCommand() // Strips trailing new-line
        if(DEBUG) Log.i(CLSS, String.format("TABLET READ: %s.", text))
        return text
    }

    /**
     * This is a substitute for readLine() which I've had much trouble with.
     * Read text until new-line or carriage-return.
     * @return a line of text. Empty indicates a closed stream.
     */
    @Synchronized fun readCommand():String {
        var text = StringBuffer()
        while(true) {
            val ch = input.read()
            if( ch<0 ) {
                return ""
            }
            else if(ch==NL || ch==CR) {
                if(text.length==0) text.append(" ")
                break
            }
            if(DEBUG) Log.i(CLSS,String.format("readCommand: %c (%d)",ch.toChar(),ch))
            text.append(ch.toChar())
        }
        return text.toString()
    }

    private val CLSS = "SocketTextHandler"
    private val CLIENT_READ_ATTEMPT_INTERVAL: Long = 250  // msecs
    private val DEBUG: Boolean
    private val CR = 13
    private val NL = 10

    init {
        DEBUG = true
        input = BufferedReader(InputStreamReader(socket.getInputStream()))
        Log.i(CLSS,"init: opened socket for read")
        output = PrintWriter(socket.getOutputStream(), true)
        Log.i(CLSS,"init: opened socket for write")
    }
}
