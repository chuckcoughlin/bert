/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bert.command.SocketTextHandler
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.service.ManagerState.ACTIVE
import chuckcoughlin.bertspeak.service.ManagerState.ERROR
import chuckcoughlin.bertspeak.service.ManagerState.PENDING
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.net.InetAddress
import java.net.Socket


/**
 * This is socket client across a wifi network to the robot which acts
 * as a server. The messages are simple text strings. We prepend message
 * type code, and append a new-line on write and expect these on read.
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
    private var handler : SocketTextHandler?
    private lateinit var serverAddress: InetAddress
    private var socket: Socket?
    private var running: Boolean
    private val host:String
    private val port:Int
    private var readjob: Job
    private var writejob: Job
    private val writeChannel: Channel<String>


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
    fun execute() {
        Log.i(CLSS, String.format("execute: connecting to %s on %d",host,port))
        while(running) {
            managerState = PENDING
            dispatcher.reportManagerState(managerType, managerState)
            try {
                socket = Socket(serverAddress, port)
                Log.i(CLSS,String.format("execute: defined client socket for %s %d",serverAddress.hostName,port))
                var connected = true
                val handler = SocketTextHandler(socket!!)
                readjob = GlobalScope.launch(Dispatchers.IO) {
                    while(connected) {
                        val text = handler.readSocket()
                        if( text==null || text.isEmpty() ) {
                            connected = false
                            handler.close()
                            Log.i(CLSS, "execute: socket closed")
                        }
                        else {
                            Log.i(CLSS, String.format("execute: read socket returned %s", text))
                            dispatcher.receiveText(text)
                        }
                    }
                }
                writejob = GlobalScope.launch(Dispatchers.IO) {
                    while(connected) {
                        val text = writeChannel.receive()
                        handler.writeSocket(text)
                        Log.i(CLSS, "writeSocket returned")
                    }
                }
                managerState = ACTIVE
                dispatcher.reportManagerState(managerType, managerState)
            }
            catch(ex:Exception) {
                Log.w(CLSS, String.format("execute: error creating socket %s %d (%s)",serverAddress.hostName,port,ex.localizedMessage))
                managerState = ERROR
                dispatcher.reportManagerState(managerType, managerState)
            }
        }
    }


    suspend fun receiveTextToSend(text:String) {
        writeChannel.send(text)
    }
    /**
     * Close IO streams.
     */
    override fun stop() {
        if( handler!=null ) {
            handler!!.close()
            handler = null
        }
        if(socket!=null && !socket!!.isClosed) socket!!.close()
        if( running ) {
            readjob.cancel()
            writejob.cancel()
        }
        managerState = ManagerState.OFF
        dispatcher.reportManagerState(managerType, managerState)
    }


    val CLSS = "SocketManager"
    val BUFFER_SIZE = 256
    val CLIENT_ATTEMPT_INTERVAL = 2000L  // 2 secs

    init {
        buffer = CharArray(BUFFER_SIZE)
        running  = false
        socket   = null
        handler  = null
        readjob = Job()
        writejob = Job()
        host  = DatabaseManager.getSetting(BertConstants.BERT_HOST_IP)
        port  = DatabaseManager.getSetting(BertConstants.BERT_PORT).toInt()
        writeChannel = Channel<String>()
    }
}
