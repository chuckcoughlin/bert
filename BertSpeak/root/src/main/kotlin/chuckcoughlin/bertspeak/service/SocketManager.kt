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
    private var textToSend: CompletableDeferred<String>
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
                    managerState = ACTIVE
                    dispatcher.reportManagerState(managerType, managerState)
                    while(socket.isConnected) {
                        textToSend = CompletableDeferred<String>("")
                        Log.i(CLSS, String.format("execute: selecting a read or write"))
                        select<Unit> {
                            read().onAwait() { it->
                                if( it.isBlank() ) {
                                    socket.close()
                                    running = false
                                }
                            }
                            textToSend.onAwait { write(it) }
                        }
                    }
                    Log.i(CLSS,"execute: select completed")
                    socket.close()
                }
                delay(CLIENT_ATTEMPT_INTERVAL)
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
                if( !socket.isClosed ) {
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
     * If we get a null, then close the socket and re-listen.
     *
     * @return the line of text
     */
    @DelicateCoroutinesApi
    fun read(): Deferred<String> =
        GlobalScope.async(Dispatchers.IO)  {
        var text = ""
        try {
            Log.i(CLSS, "read: blocked on read ")
            //text = reader.readLine() // Does not include CR
            text = readNextLine(reader)
            Log.i(CLSS, String.format("read: returned: %s", text))
        }
        catch (ioe: IOException) {
            Log.e(CLSS, String.format("read: Error reading from socket (%s)", ioe.localizedMessage))
        }
        catch (npe: NullPointerException) {
            Log.e(CLSS, String.format( "read: Null pointer reading from socket (%s)",npe.localizedMessage))
        }
        catch (ex:Exception) {
            Log.e(CLSS, String.format( "read: Unhandled exception (%s)",ex.localizedMessage))
        }
        text
    }

    /**
     * Write plain text to the socket.
     */
    fun write(text: String)  {
        try {
            Log.i(CLSS, String.format( "write: writing ... %s (%d bytes)",
                text, text.length + 1))
            writer.println(text) // Appends new-line
            writer.flush()
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

    private fun readNextLine(reader: BufferedReader):String {
        val stringBuilder = StringBuilder()
        var c = reader.read().toChar()
        while( c!= '\n' ) {
            stringBuilder.append(c)
            Log.i(CLSS, String.format("readNextLine: got %c", c))
        }
        return stringBuilder.toString()
    }
    fun prepareTextToSend(text:String) {
        Log.i(CLSS, String.format("prepareTextToSend: writing %s", text))
        textToSend.complete(text)
    }

    val CLSS = "SocketManager"
    val BUFFER_SIZE = 256
    val CLIENT_ATTEMPT_INTERVAL = 2000L  // 2 secs
    val CLIENT_LOG_INTERVAL = 10

    init {
        buffer = CharArray(BUFFER_SIZE)
        running  = false
        socket = Socket()
        textToSend = CompletableDeferred<String>("")
        job = Job()
        host  = DatabaseManager.getSetting(BertConstants.BERT_HOST_IP)
        port  = DatabaseManager.getSetting(BertConstants.BERT_PORT).toInt()
    }
}
