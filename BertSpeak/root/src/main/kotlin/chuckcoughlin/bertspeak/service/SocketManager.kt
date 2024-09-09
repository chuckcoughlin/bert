/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bertspeak.network.SocketTextHandler
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.service.ManagerState.ACTIVE
import chuckcoughlin.bertspeak.service.ManagerState.PENDING
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress


/**
 * This is socket client connects across a wifi network to the robot which acts
 * as a server. The messages are simple text strings. We prepend message
 * type code and semi-colon as a header, plus a new-line at the end. This allows
 * the reader to parse commands separately.
 *
 * The file descriptors are opened on "openConnections" and closed on
 * "shutdown". Change listeners are notified (in a separate Thread) when the
 * socket is "ready".
 *
 * @param service the dispatcher
*/
class SocketManager(service:DispatchService): CommunicationManager {
    override val managerType = ManagerType.SOCKET
    override var managerState = ManagerState.OFF
    private val buffer: CharArray
    private val dispatcher = service
    private lateinit var serverAddress: SocketAddress
    private var socket: Socket
    private var connected: Boolean
    private var running: Boolean
    private val host:String
    private val port:Int
    private var job: Job
    private val writeChannel: Channel<String>

    /* On start, the manager connects to the remote robot host
     * and establishes a connection as a client.
     */
    @DelicateCoroutinesApi
    override fun start() {
        Log.i(CLSS, "start ...")
        serverAddress = InetSocketAddress(host,port)
        running = true
        job = GlobalScope.launch(Dispatchers.IO) {
            Log.i(CLSS, String.format("start: launch execution"))
            execute()
        }
    }

    /**
     * While running, this manager processes messages between the tablet
     * and robot.
     */
    @DelicateCoroutinesApi
    private suspend fun execute() {
        Log.i(CLSS, String.format("execute: connecting to %s on %d",host,port))
        while(running) {
            managerState = PENDING
            dispatcher.reportManagerState(managerType, managerState)
            try {
                Log.i(CLSS,String.format("execute: defined client socket for %s %d",host,port))
                socket = Socket()
                socket.connect(serverAddress,CONNECTION_TIMEOUT)
                val handler = SocketTextHandler(socket)
                connected = true
                sendStartupMessage(handler)
                managerState = ACTIVE
                dispatcher.reportManagerState(managerType, managerState)
                while(connected) {
                    Log.i(CLSS, "execute: selecting ...")
                    select<String> {
                        handleResponse(handler).onAwait{it} // Accept from socket (robot)
                        writeChannel.onReceive() {
                            handleRequest(it,handler)       // Forward to socket (robot)
                            it
                        }
                    }  // End select
                }
                if( !socket.isClosed ) socket.close()
            }
            // On error we retry. Perhaps robot not ready.
            catch(ex:Throwable) {
                Log.w(CLSS, String.format("execute: error creating socket %s %d (%s)",host,port,ex.localizedMessage))
                try {
                    Thread.sleep(SOCKET_RETRY_INTERVAL)
                }
                catch(ie:InterruptedException) {}
            }
        }
    }

    /**
     * We have received a message to send to the robot by forwarding it to the socket.
     */
    private fun handleRequest(msg:String,handler:SocketTextHandler)  {
        handler.writeSocket(msg)
    }

    /**
     * Wait to receive a message from the socket. Forward it to the dispatcher.
     */
    @DelicateCoroutinesApi
    fun handleResponse(handler:SocketTextHandler): Deferred<String> =
        GlobalScope.async(Dispatchers.IO) {
            val txt = handler.readSocket()
            if( txt.isEmpty() ) {
                connected = false
                Log.i(CLSS, "execute: socket closed")
            }
            else {
                Log.i(CLSS, String.format("execute: read socket returned %s", txt))
                dispatcher.receiveMessage(txt)
            }
            txt
        }

    suspend fun receiveTextToSend(text:String) {
        writeChannel.send(text)
    }
    /**
     * Close IO streams.
     */
    override fun stop() {
        if(!socket.isClosed) socket.close()
        if( running ) {
            job.cancel()
        }
        managerState = ManagerState.OFF
        dispatcher.reportManagerState(managerType, managerState)
    }

    /** Send a startup message directly to the socket **/
    private fun sendStartupMessage(handler:SocketTextHandler) {
        handler.writeSocket(String.format("%s:%s",MessageType.LOG.name,START_MESSAGE))
    }

    val CLSS = "SocketManager"
    val BUFFER_SIZE = 256
    val CLIENT_ATTEMPT_INTERVAL = 2000L  // 2 secs
    val CONNECTION_TIMEOUT      = 4000   // 4 secs
    val SOCKET_RETRY_INTERVAL   = 30000L // 30 secs
    val START_MESSAGE           = "The tablet is connected"

    init {
        buffer = CharArray(BUFFER_SIZE)
        connected  = false
        running  = false
        socket   = Socket()
        job = Job()
        host  = DatabaseManager.getSetting(BertConstants.BERT_HOST_IP)
        port  = DatabaseManager.getSetting(BertConstants.BERT_PORT).toInt()
        writeChannel = Channel<String>()
    }
}
