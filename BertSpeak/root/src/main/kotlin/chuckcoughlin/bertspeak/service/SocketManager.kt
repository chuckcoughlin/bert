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
import kotlinx.coroutines.delay
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.io.BufferedReader
import java.io.InputStreamReader
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
 * @param handler the parent fragment
*/
class SocketManager(service:DispatchService): CommunicationManager {
    override val managerType = ManagerType.SOCKET
    override var managerState = ManagerState.OFF
    private val buffer: CharArray
    private val dispatcher = service
    private lateinit var serverAddress: InetAddress
    private var textToSend: CompletableDeferred<String>
    private var socket: Socket?
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
                try {
                    socket = Socket(serverAddress, port)
                    Log.i(CLSS,String.format("execute: defined client socket for %s %d",serverAddress.hostName,port))
                    var connected = true
                    val handler = SocketTextHandler(socket!!)
                    managerState = ACTIVE
                    dispatcher.reportManagerState(managerType, managerState)
                    while (connected) {
                        select<Unit> {
                            handler.readSocket().onAwait() {
                                Log.i(CLSS,"readSocket returned")
                                if(it.isNotEmpty()) {
                                    dispatcher.receiveText(it)
                                }
                                else {
                                    connected = false
                                }
                            }
                            textToSend.onAwait() {
                                connected = handler.writeSocket(it)
                            }
                        }
                    }
                    Log.i(CLSS, "execute: socket closed")
                    socket!!.close()
                }
                catch(ex:Exception) {
                    Log.w(CLSS, String.format("execute: error creating socket %s %d (%s)",serverAddress.hostName,port,ex.localizedMessage))
                    managerState = ERROR
                    dispatcher.reportManagerState(managerType, managerState)
                }
                delay(CLIENT_ATTEMPT_INTERVAL)
            }
        }
    }

    fun receiveTextToSend(text:String) {
        textToSend.complete(text)
    }
    /**
     * Close IO streams.
     */
    override fun stop() {
        if( running ) {
            job.cancel()
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
        textToSend = CompletableDeferred<String>("")
        job = Job()
        host  = DatabaseManager.getSetting(BertConstants.BERT_HOST_IP)
        port  = DatabaseManager.getSetting(BertConstants.BERT_PORT).toInt()
    }
}
