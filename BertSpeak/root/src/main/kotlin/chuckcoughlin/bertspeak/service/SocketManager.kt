/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.service.ManagerState.ACTIVE
import chuckcoughlin.bertspeak.service.ManagerState.ERROR
import chuckcoughlin.bertspeak.service.ManagerState.PENDING
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.start
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.util.UUID


/**
 * This is socket client across a wifi network to the robot which acts
 * as a server. The messages are simple text strings. We prepend message
 * type code, and append a new-line on write and expect these on read.
 *
 * The file descriptors are opened on "openConnections" and closed on
 * "shutdown". Change listeners are notified (in a separate Thread) when the
 * socket is "ready".
 *
 * @param handler the parent fragment
*/
class SocketManager(service:DispatchService): CommunicationManager {
    override val managerType = ManagerType.SOCKET
    override var managerState = ManagerState.OFF
    private val buffer: CharArray
    private var connected: Boolean
    private val dispatcher = service
    private lateinit var serverAddress: InetAddress
    private lateinit var reader: BufferedReader
    private lateinit var writer: PrintWriter
    private var socket: Socket
    private var running: Boolean
    private val host:String
    private val port:Int
    private var job: Job


    /* On start, the manager connects to the remote robot host
     * and establishes a connection as a client.
     */
    @DelicateCoroutinesApi
    override fun start() {
        Log.i(CLSS, "start ...")
        serverAddress = InetAddress.getByName(host)
        if( !serverAddress.isSiteLocalAddress ) {
            Log.i(CLSS, String.format("start: address resolution failed for %s", host))
            managerState = ERROR
            dispatcher.reportManagerState(managerType, managerState)
        }
        else {
            running = true
            execute()
        }
    }

    /**
     * While running, this manager processes messages between the tablet
     * and robot.
     */
    @DelicateCoroutinesApi
     fun execute(): Unit {
        Log.i(CLSS, String.format("execute: connecting to %s on %d",host,port))
        job = GlobalScope.launch(Dispatchers.IO) {
            while(running) {
                managerState = PENDING
                dispatcher.reportManagerState(managerType, managerState)
                socket = Socket(serverAddress, port)

                Log.i(CLSS, String.format("execute: defined client socket on %s", socket.localAddress.hostName))
                if(!defineReaderWriter(socket)) {
                    socket.close()
                    managerState = ERROR
                    dispatcher.reportManagerState(managerType, managerState)
                    running = false
                }
                else {
                    connected = true
                    managerState = ACTIVE
                    dispatcher.reportManagerState(managerType, managerState)
                    while(connected ) {
                        Log.i(CLSS, String.format("execute: reading from socket ..."))
                        connected = readSocket()
                    }
                    Log.i(CLSS,"execute: socket closed")
                    socket.close()
                }
            }
        }
    }
    /**
     * Open IO streams for reading and writing. The socket must exist.
     * @return an error description. Null if no error.
     */
    private fun defineReaderWriter(sock:Socket): Boolean {
        var success = false
        try {
            reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            Log.i(CLSS, String.format("defineReaderWriter: opened for read"))
            try {
                writer = PrintWriter(sock.getOutputStream(), true)
                Log.i(CLSS, String.format("defineReaderWriter: opened for write"))
                writer.println(String.format("%s:the tablet is connected", MessageType.LOG.name))
                writer.flush()
                success = true
            }
            catch (ex: Exception) {
                val reason = String.format("The tablet failed to open a socket for writing due to %s",ex.message)
                Log.i( CLSS,String.format("defineReaderWriter: ERROR opening socket for write (%s)",
                    CLSS,ex.message),ex)
                dispatcher.logError(managerType,reason)
            }
        }
        catch (ex: Exception) {
            val reason = String.format("The tablet failed to open a socket for reading due to %s",ex.message)
            Log.i(CLSS,String.format("defineReaderWriter: ERROR opening socket for read (%s)",CLSS,ex.message), ex)
            dispatcher.logError(managerType,reason)
        }
        return success
    }
    /**
     * Close IO streams.
     */
    override fun stop() {
        if( running ) {
            job.cancel()
            try {
                reader.close()
            }
            catch (ignore: IOException) {}
            try {
                writer.close()
            }
            catch (ignore: IOException) {}
            running = false
            if( !socket.isClosed ) {
                socket.close()
             }
        }
        managerState = ManagerState.OFF
        dispatcher.reportManagerState(managerType, managerState)
    }

    /**
     * Read a line of text from the socket. The read will block and wait for data to appear.
     * If we get a null, then close the socket and re-listen.
     *
     * @return connection status
     */
    @DelicateCoroutinesApi
    fun readSocket(): Boolean  {
        var text : String? =  reader.readLine()      // Does not include CR
        if( text!=null ) {
            Log.i(CLSS, String.format("read: returned: %s.", text))
            dispatcher.receiveText(text)
            return true
        }
        else {
            Log.i(CLSS,"received nothing on read. Assume server has stopped.")
            return false
        }
    }

    /**
     * Write plain text to the socket.
     */
    fun writeSocket(text: String) {
        if( text.isNotBlank()) {
            Log.i(CLSS, String.format("writeSocket: writing ... %s (%d bytes)",
                text, text.length + 1))
            writer.println(text) // Appends new-line
            writer.flush()
        }
        else {
            Log.i(CLSS, "writeSocket: atempt to write empty string, ignored")
        }
    }

    /**
     * We've gotten a null when reading the socket. This means, as far as I can tell, that the other end has
     * shut down. Close the socket and re-open or re-listen/accept. We hang until this succeeds.
     * @return the next text from the socket.
     */
    @DelicateCoroutinesApi
    private fun reread(): String {
        var text = ""
        Log.i(CLSS, String.format("reread: ..."))
        stop()
        start()
        try {
            reader = BufferedReader(InputStreamReader(socket.inputStream))
            Log.i(CLSS, String.format("reread: reopened socket for read"))
            text = reader.readLine()
        }
        catch(ex: Exception) {
            Log.i(CLSS,String.format("reread: ERROR opening for read (%s)", ex.message))
        }
        Log.i(CLSS, String.format("reread: got %s", text))
        return text
    }


    val CLSS = "SocketManager"
    val BUFFER_SIZE = 256
    val CLIENT_ATTEMPT_INTERVAL = 2000L  // 2 secs
    val CLIENT_LOG_INTERVAL = 10

    init {
        buffer = CharArray(BUFFER_SIZE)
        running  = false
        socket = Socket()
        connected = false
        job = Job()
        host  = DatabaseManager.getSetting(BertConstants.BERT_HOST_IP)
        port  = DatabaseManager.getSetting(BertConstants.BERT_PORT).toInt()
    }
}
