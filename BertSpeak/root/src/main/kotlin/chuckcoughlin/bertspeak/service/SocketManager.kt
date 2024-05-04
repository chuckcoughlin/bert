/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.service

import android.Manifest.permission
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
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
import java.lang.Thread.UncaughtExceptionHandler
import java.net.InetAddress
import java.net.Socket
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
    private var textToSend: CompletableDeferred<String>
    private val dispatcher = service
    private var connected: Boolean
    private lateinit var reader: BufferedReader
    private lateinit var writer: PrintWriter
    private lateinit var socket: Socket
    private var running: Boolean
    private val host:String
    private val port:Int
    private var job: Job


    /* On start, the manager connects to the remote robot host
     * and establishes a connection
     */
    @DelicateCoroutinesApi
    override fun start() {
        Log.i(CLSS, "start ...")
        val serverAddress = InetAddress.getByName(host)
        if( !serverAddress.isSiteLocalAddress ) {
            Log.i(CLSS, String.format("start: address resolution failed for %s", host))
            managerState = ERROR
            dispatcher.reportManagerState(managerType, managerState)
            return
        }

        Log.i(CLSS, String.format("start: defined client socket on %s", socket.localAddress.hostName))
        managerState = PENDING
        dispatcher.reportManagerState(managerType, managerState)

        socket = Socket(serverAddress,port)
        if(!defineReaderWriter(socket)) {
            socket.close()
            managerState = ERROR
            dispatcher.reportManagerState(managerType, managerState)
            return
        }

        Log.i(CLSS, String.format("start: defined client socket on %s", socket.localAddress.hostName))
        managerState = ACTIVE
        dispatcher.reportManagerState(managerType, managerState)

        job = GlobalScope.launch(Dispatchers.IO) {
                running = true
                execute()
        }
    }

    /**
     * Open IO streams for reading and writing. The socket must exist.
     * @return an error description. Null if no error.
     */
    private fun defineReaderWriter(sock:Socket): Boolean {
        var success = false
        try {
            reader = BufferedReader(InputStreamReader(sock.inputStream))
            Log.i(CLSS, String.format("openPorts: opened for read"))
            try {
                writer = PrintWriter(sock.outputStream, true)
                Log.i(CLSS, String.format("openPorts: opened %s for write"))
                write(String.format("%s:the tablet is connected", MessageType.LOG.name))
                success = true
            }
            catch (ex: Exception) {
                val reason = String.format("The tablet failed to open a socket for writing due to %s",
                    ex.message)
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
     * While running, this controller processes messages between the Dispatcher
     * and a user terminal. A few messages are intercepted that totally local
     * in nature (SLEEP,WAKE).
     *
     * A response to a request that starts here will have the source as Termonal.
     * When the application is run autonomously (like as a system service), the
     * terminal is not used.
     */
    @DelicateCoroutinesApi
    suspend fun execute(): Unit = coroutineScope {
        Log.i(CLSS,"execute: started...")
        while (running) {
            select<Unit> {
                read().onAwait(){}
                textToSend.onAwait{ write(it) }
            }
        }
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
            try {
                if( connected ) {
                    socket.close()
                }
             }
            catch (_: IOException) {}
        }
        managerState = ManagerState.OFF
        dispatcher.reportManagerState(managerType, managerState)
    }

    /**
     * Read a line of text from the socket. The read will block and wait for data to appear.
     * If we get a null, then close the socket and either re-open or re-listen
     * depending on whether or not this is the server side, or not.
     *
     * @return the line of text
     */
    @DelicateCoroutinesApi
    fun read(): Deferred<String> =
        GlobalScope.async(Dispatchers.IO)  {
        var text: String
        try {
            Log.i(CLSS, "read: reading ... ")
            text = reader.readLine() // Does not include CR
            Log.i(CLSS, String.format("read: returning: %s", text))
        }
        catch (ioe: IOException) {
            Log.e(CLSS, String.format("read: Error reading from socket (%s)", ioe.localizedMessage))
            // Close and attempt to reopen port
            text = reread()
        }
        catch (npe: NullPointerException) {
            Log.e(CLSS, String.format( "read: Null pointer reading from socket (%s)",npe.localizedMessage))
            // Close and attempt to reopen port
            text = reread()
        }
        text
    }

    /**
     * Write plain text to the socket.
     */
    fun write(text: String)  {
        try {
            if (!writer.checkError()) {
                Log.i(CLSS, String.format( "write: writing ... %s (%d bytes)",
                    text, text.length + 1))
                writer.println(text) // Appends new-line
                writer.flush()
            }
            else {
                Log.e(CLSS, String.format("write: out stream error"))
            }
        }
        catch (ex: Exception) {
            Log.e(CLSS,String.format("write: Error writing %d bytes (%s)",
                text.length,ex.localizedMessage),ex)
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

    fun prepareTextToSend(text:String) {
        textToSend.complete(text)
    }

    val CLSS = "SocketManager"
    val BUFFER_SIZE = 256
    val CLIENT_ATTEMPT_INTERVAL = 2000L  // 2 secs
    val CLIENT_LOG_INTERVAL = 10

    // Well-known port for Bluetooth serial port service
    val SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    init {
        buffer = CharArray(BUFFER_SIZE)
        connected = false
        running  = false
        textToSend = CompletableDeferred<String>("")
        job = Job()
        host  = DatabaseManager.getSetting(BertConstants.BERT_HOST)
        port  = DatabaseManager.getSetting(BertConstants.BERT_PORT).toInt()
    }
}
