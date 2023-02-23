/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.logging.Logger

/**
 * The socket controller handles two-way communication across a NamedSocket.
 *
 * Depending on the presence of a hostname in the configuration, the connection is
 * be configured for either a server or client (hostname exists).
 */
open class SocketController : Controller {
    private val socket: NamedSocket
    private val server : Boolean  // True of this is created by the Dispatcher.
    protected val changeListeners: MutableList<SocketStateChangeListener> = ArrayList()
    private val dispatcher:Controller
    private var parentRequestChannel: Channel<MessageBottle>
    private var parentResponseChannel: Channel<MessageBottle>
    val scope = MainScope() // Uses Dispatchers.Main
    var ignoring : Boolean
    var running:Boolean
    var runner:Thread? = null

    /**
     * Constructor: Use this constructor from either server or client processes.
     *    "socket" and "port" are required parameters. If the socket is from a
     *    client then "hostname" is also required.
     * @param parent - the dispatcher
     * @param req - channel for requests from the parent (Dispatcher)
     * @param rsp - channel for responses sent to the parent (Dispatcher)
     */
    constructor(parent: Controller,req : Channel<MessageBottle>,rsp: Channel<MessageBottle>) {
        dispatcher = parent
        parentRequestChannel = req
        parentResponseChannel = rsp
    }

    fun addChangeListener(c: SocketStateChangeListener) {
        changeListeners.add(c)
    }

    fun removeChangeListener(c: SocketStateChangeListener) {
        changeListeners.remove(c)
    }

    /**
     * When running, this controller processes messages between the Dispatcher
     * and the user. A few messages are intercepted that cause a
     * quick shutdown. These are direct responses to user input, like "shutdown".
     */
    override suspend fun start() {
        running = true
        val rdr = BackgroundReader(socket)
        runner = Thread(rdr)
        runner!!.start()
        runBlocking<Unit> {
            launch {
                Dispatchers.IO
                while(running) {
                    select<Unit> {
                        /**
                         * These are responses coming from the Dispatcher
                         * Simply display them.
                         */
                        parentResponseChannel.onReceive() {
                            receiveResponse(it)
                        }
                        parentRequestChannel.onReceive() {
                            receiveRequest(it)
                        }
                    }
                }
            }
        }
    }

    override suspend fun stop() {
        scope.cancel()
        if (runner != null) {
            LOGGER.info(String.format("%s.stopping ... %s", CLSS, socket.name))
            runner!!.interrupt()
            runner = null
        }
        socket.shutdown()
    }

    /**
     * If the parent is a server (i.e. the launcher), then we get the request from the socket,
     * else it comes from the parent using a direct call.
     * @param request
     */
    fun receiveRequest(request: MessageBottle?) {
        if (request == null) return  // Can happen if socket closed (e.g. shutdown)
        if (server) {
            //launcher.handleRequest(request)
        } else {
            socket.write(request)
        }
    }

    /**
     * If the parent is a server (i.e. the launcher), then write the response to the socket.
     * Otherwise we are on the receiving end. Handle it.
     * @param response
     */
    fun receiveResponse(response: MessageBottle?) {
        if (server) {
            socket.write(response)
        }
        else {
            //launcher.handleResponse(response)
        }
    }
    // ===================================== Background Reader ==================================================
    /**
     * Perform a blocking read as a background thread.
     */
    inner class BackgroundReader(private val sock: NamedSocket) : Runnable {
        /**
         * Forever ...
         * 1) Read request/response from socket
         * 2) Invoke callback method on launcher or
         * local method, as appropriate.
         */
        override fun run() {
            sock.create()
            sock.startup()
            notifyChangeListeners(sock.name, SocketStateChangeEvent.Companion.READY)
            while (!Thread.currentThread().isInterrupted) {
                val msg = sock.read()
                if (msg == null) {
                    try {
                        Thread.sleep(CLIENT_READ_ATTEMPT_INTERVAL) // A read error has happened, we don't want a hard loop
                        continue
                    } catch (ignore: InterruptedException) {
                    }
                }
                if (sock.isServer) {
                    receiveRequest(msg)
                }
                else {
                    receiveResponse(msg)
                }
            }
            LOGGER.info(String.format("BackgroundReader,%s stopped", sock.name))
        }
    }

    // ===================================== Helper Methods ==================================================
    // Notify listeners in a separate thread
    protected fun notifyChangeListeners(name: String?, state: String?) {
        val event = SocketStateChangeEvent(this, name, state)
        if (changeListeners.isEmpty()) return  // Nothing to do
        val thread = Thread {
            for (l in changeListeners) {
                //log.infof("%s.notifying ... %s of %s",TAG,l.getClass().getName(),value.toString());
                l.stateChanged(event)
            }
        }
        thread.start()
    }

    private val CLSS = "SocketController"
    protected val CLIENT_READ_ATTEMPT_INTERVAL: Long = 15000 // 15 secs
    private val LOGGER = Logger.getLogger(CLSS)
    override var controllerName = CLSS

    init {
        controllerName = RobotModel.getControllerForType(ControllerType.TERMINAL)
        val socketName = RobotModel.getPropertyForController(controllerName,ConfigurationConstants.PROPERTY_SOCKET)
        val hostName = RobotModel.getPropertyForController(controllerName,ConfigurationConstants.PROPERTY_HOSTNAME)
        val port = RobotModel.getPropertyForController(controllerName,ConfigurationConstants.PROPERTY_PORT).toInt()
        if( hostName.equals(ConfigurationConstants.NO_VALUE)) {
            server = true
            socket = NamedSocket(socketName,port)
        }
        else {
            server = false
            socket = NamedSocket(socketName,hostName,port)
        }
        running = false
        ignoring = false
    }
}