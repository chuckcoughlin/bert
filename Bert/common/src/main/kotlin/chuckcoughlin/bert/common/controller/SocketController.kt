/**
 * Copyright 2022. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.MessageHandler
import java.util.logging.Logger

/**
 * The socket controller handles two-way communication across a NamedSocket.
 *
 * Depending on which constructor is used, the connection can be configured
 * for either a server or client.
 */
class SocketController : chuckcoughlin.bert.common.controller.Controller {
    private val LOGGER = Logger.getLogger(CLSS)
    protected val socket: chuckcoughlin.bert.common.controller.NamedSocket
    protected val launcher: MessageHandler
    protected var runner: Thread? = null
    private val server // True of this is created by the server process.
            : Boolean
    protected val changeListeners: MutableList<SocketStateChangeListener> = ArrayList()

    /**
     * Constructor: Use this constructor from the server process.
     * @param launcher the parent application, one of the independent processes
     * @param name the socket name
     * @param port communication port number
     */
    constructor(launcher: MessageHandler, name: String, port: Int) {
        this.launcher = launcher
        server = true
        socket = chuckcoughlin.bert.common.controller.NamedSocket(name, port)
    }

    /**
     * Constructor: Use this version for processes that are clients
     * @param launcher the parent application
     * @param hostname of the server process.
     * @param port communication port number
     */
    constructor(launcher: MessageHandler, name: String, hostname: String, port: Int) {
        this.launcher = launcher
        server = false
        socket = chuckcoughlin.bert.common.controller.NamedSocket(name, hostname, port)
    }

    fun addChangeListener(c: SocketStateChangeListener) {
        changeListeners.add(c)
    }

    fun removeChangeListener(c: SocketStateChangeListener) {
        changeListeners.remove(c)
    }

    override fun start() {
        val rdr = BackgroundReader(socket)
        runner = Thread(rdr)
        runner!!.start()
    }

    override fun stop() {
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
    override fun receiveRequest(request: MessageBottle) {
        if (server) {
            launcher.handleRequest(request)
        }
        else {
            socket.write(request)
        }
    }

    /**
     * If the parent is a server (i.e. the launcher), then write the response to the socket.
     * Otherwise we are on the receiving end. Handle it.
     * @param response
     */
    override fun receiveResponse(response: MessageBottle) {
        if (server) {
            socket.write(response)
        }
        else {
            launcher.handleResponse(response)
        }
    }
    // ===================================== Background Reader ==================================================
    /**
     * Perform a blocking read as a background thread.
     */
    inner class BackgroundReader(private val sock: chuckcoughlin.bert.common.controller.NamedSocket) : Runnable {
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
                    }
                    catch (ignore: InterruptedException) {
                    }
                }
                else {
                    if (sock.isServer) {
                        receiveRequest(msg)
                    }
                    else {
                        receiveResponse(msg)
                    }
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

    companion object {
        private const val CLSS = "SocketController"
        protected const val CLIENT_READ_ATTEMPT_INTERVAL: Long = 15000 // 15 secs
    }
}