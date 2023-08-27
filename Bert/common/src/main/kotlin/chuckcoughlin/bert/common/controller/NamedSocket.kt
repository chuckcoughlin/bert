/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.common.controller

import chuckcoughlin.bert.common.message.MessageBottle
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.logging.Logger

/**
 * The socket takes the name of the client. It encapsulates a bi-directional
 * connection between client and server used for passing "RequestBottles" and
 * "ResponseBottles". The Server process should instantiate the with "server" = "true".
 *
 * The file descriptors are opened on "startup" and closed on
 * "shutdown". Change listeners are notified (in a separate Thread) when the
 * socket is "ready".
 *
 * NOTE: setting a timeout on normal socket operations like accept() is
 * a bad idea. Next time around we end up with a "socket in use error".
 */
class NamedSocket {
    val name: String
    private val host: String
    private val port: Int
    val isServer : Boolean   // True if this instance is owned by the server.
    private var serverSocket: ServerSocket?
    private var socket: Socket?
    private var input: BufferedReader? = null
    private var out: PrintWriter? = null

    /**
     * Constructor: Use this constructor from the server process.
     * @param name identifier of the connection, the client name
     * @param port communication port number
     */
    constructor(name: String, port: Int) {
        this.name = name
        host = "" // Not needed
        this.port = port
        isServer = true
        socket = null
        serverSocket = null
    }

    /**
     * Constructor: Use this version for processes that are clients
     * @param launcher the parent application
     * @param hostname of the server process.
     * @param port communication port number
     */
    constructor(name: String, hostname: String, port: Int) {
        this.name = name
        host = hostname
        this.port = port
        isServer = false
        socket = null
        serverSocket = null // Not needed
    }

    /**
     * If we are a server, create a listener and wait to accept a connection.
     * There is no action for a client.
     */
    fun create():Boolean {
        var success = true
        var attempts = 1
        if (isServer) {
            while (true) {
                try {
                    serverSocket = ServerSocket(port)
                    LOGGER.info(String.format("%s.create: %s as server listening on port %d",
                        CLSS, name, port))
                    socket = serverSocket!!.accept()
                    LOGGER.info(String.format("%s.create: %s accepted connection on port %d after %d attempts",
                            CLSS,name,port,attempts))
                    break
                } 
                catch (ex: Exception) {
                    socket = null
                    LOGGER.severe(String.format("%s.create: ERROR creating server socket %s (%s)",
                            CLSS,name,ex.message))
                    try {
                        Thread.sleep(SERVER_ATTEMPT_INTERVAL) // Something bad has happened, we don't want a hard loop
                    }
                    catch (ignore: InterruptedException) {}
                }
                attempts++
            }
        }
        else {
            // Keep attempting a connection until the server is ready
            while (true) {
                try {
                    LOGGER.info(String.format("%s.create: %s attempting to connect to server %s on %d ...",
                            CLSS,name,host,port))
                    socket = Socket(host, port)
                    LOGGER.info(String.format("%s.create: new %s connection from %s on %d after %d attempts",
                            CLSS,name,host,port,attempts))
                    break
                } 
                catch (ioe: IOException) {
                    // Get a "connection refused" when remote party is not running yet.
                    LOGGER.info(String.format("%s.create: ERROR connecting to server socket %s:%d (%s)",
                            CLSS,host,port,ioe.message))
                    try {
                        Thread.sleep(CLIENT_ATTEMPT_INTERVAL)
                    } 
                    catch (ie: InterruptedException) {
                        if (attempts % CLIENT_LOG_INTERVAL == 0) {
                            LOGGER.warning( String.format("%s.create: ERROR creating client socket %s (%s)",
                                    CLSS,name,ioe.message))
                            success = false
                        }
                    }
                }
                attempts++
            }
        }
        return success
    }

    /**
     * This must not be called before the socket is created.
     * Open IO streams for reading and writing.
     */
    fun startup() {
        if (socket != null) {
            try {
                input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                LOGGER.info(String.format("%s.startup: opened %s for read",CLSS, name))
            }
            catch (ex: Exception) {
                LOGGER.info(String.format("%s.startup: ERROR opening %s for read (%s)",
                    CLSS, name, ex.message))
            }
            try {
                out = PrintWriter(socket!!.getOutputStream(), true)
                LOGGER.info(String.format("%s.startup: opened %s for write",
                    CLSS, name))
            } 
            catch (ex: Exception) {
                LOGGER.info(String.format("%s.startup: ERROR opening %s for write (%s)",
                    CLSS, name, ex.message))
            }
        }
    }

    /**
     * Close IO streams. Closing the socket should interrupt any read.
     */
    fun shutdown() {
        LOGGER.info(String.format("%s.shutdown: %s closing sockets ...",
            CLSS, name))
        try {
            if (socket != null) socket!!.close()
            if (serverSocket != null) serverSocket!!.close()
        } 
        catch (ioe: IOException) {}
        LOGGER.info(String.format("%s.shutdown: %s closing in ...",
            CLSS, name))
        if (input != null) {
            try {
                input!!.close()
            }
            catch (ignore: IOException) {
            }
            input = null
        }
        LOGGER.info(String.format("%s.shutdown: %s closing out ...",
            CLSS, name))
        if (out != null) {
            out!!.close()
            out = null
        }
        LOGGER.info(String.format("%s.shutdown: %s complete.",
            CLSS, name))
    }

    /**
     * Read from the socket. The read will block and wait for data to appear.
     * If we get a null, then close the socket and either re-open or re-listen
     * depending on whether or not this is the server side, or not.
     *
     * @return either a RequestBottle or a ResponseBottle as appropriate.
     */
    fun read(): MessageBottle? {
        var bottle: MessageBottle? = null
        try {
            if (input != null) {
                //LOGGER.info(String.format("%s.read: reading %s ... ",CLSS,name));
                var json = input!!.readLine()
                while (json == null) {
                    json = reread()
                }
                LOGGER.info(String.format("%s.read: %s got %s",
                    CLSS, name, json))
                bottle = MessageBottle.fromJSON(json)
            }
            else {
                LOGGER.warning(String.format("%s.read: Attempt to read from %s before port is open (ignored)",
                            CLSS,name))
            }
        }
        catch (npe: NullPointerException) {
            LOGGER.severe(String.format("%s.read: Exception reading from %s (%s)",
                CLSS, name, npe.localizedMessage))
        }
        catch (ioe: IOException) {
            LOGGER.severe(String.format("%s.read: Exception reading from %s (%s)",
                CLSS, name, ioe.localizedMessage))
        }
        return bottle
    }

    /**
     * Read a line of text from the socket. The read will block and wait for data to appear.
     * If we get a null, then close the socket and either re-open or re-listen
     * depending on whether or not this is the server side, or not.
     *
     * @return either a RequestBottle or a ResponseBottle as appropriate.
     */
    fun readLine(): String? {
        var text: String? = null
        try {
            if (input != null) {
                LOGGER.info(String.format("%s.readLine: reading %s ... ",
                    CLSS, name))
                text = input!!.readLine()
                while (text == null) {
                    try {
                        Thread.sleep(10000L)
                    }
                    catch (ignore: InterruptedException) {
                    }
                    LOGGER.info(String.format("%s.readLine: got null, retrying",CLSS))
                    //text = reread();  // May not need
                    text = input!!.readLine()
                }
                LOGGER.info(String.format("%s.readLine: got %s",CLSS, text))
            }
            else {
                LOGGER.warning(String.format("%s.readLine: Attempt to read from %s before port is open )ignored)",
                        CLSS,name))
            }
        }
        catch (ioe: IOException) {
            LOGGER.severe(String.format("%s.readLine: Exception reading from %s (%s)",
                        CLSS,name,ioe.localizedMessage))
        }
        return text
    }

    /**
     * Write the MessageBottle serialized as a JSON string to the socket.
     */
    fun write(bottle: MessageBottle?) {
        val json = bottle!!.toJSON()
        //byte[] bytes = json.getBytes();
        //int size = bytes.length;
        try {
            if (out != null) {
                out!!.println(json)
                out!!.flush()
                LOGGER.info(String.format("%s.write: wrote %s %d bytes. ",
                    CLSS, name, json.length))
            }
            else {
                LOGGER.warning(String.format("%s.write: Attempt to write to %s before port is open (ignored)",
                        CLSS,name))
            }
        }
        catch (ioe: Exception) {
            LOGGER.severe(String.format("%s.write: Exception writing %d bytes (%s)",
                    CLSS,json.length,ioe.localizedMessage))
        }
    }

    /**
     * Write plain text to the socket. (No added line-feed)
     */
    fun write(text: String) {
        try {
            if (out != null) {
                LOGGER.info(String.format("%s.write: wrote %s %d bytes (%s)",
                    CLSS, name, text.length, text))
                out!!.println(text) // Appends new-line
                out!!.flush()
            }
            else {
                LOGGER.info(String.format("%s.write: Attempt to write to %s before port is open (ignored)",
                    CLSS, name))
            }
        }
        catch (ioe: Exception) {
            LOGGER.severe(String.format("%s.write: Exception writing %d bytes (%s)",
                    CLSS, text.length,ioe.localizedMessage))
        }
    }

    /**
     * We've gotten a null when reading the socket. This means, as far as I can tell, that the other end has
     * shut down. Close the socket and re-open or re-listen/accept. We hang until this succeeds.
     * @return the next
     */
    private fun reread(): String? {
        var json: String? = null
        LOGGER.info(String.format("%s.reread: on port %s",CLSS, name))
        if (input != null) try {
            input!!.close()
        }
        catch (ignore: IOException) {}
        if (socket != null) try {
            socket!!.close()
        }
        catch (ignore: IOException) {}
        if (serverSocket != null) try {
            serverSocket!!.close()
        }
        catch (ignore: IOException) {}
        create()
        try {
            input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            LOGGER.info(String.format("%s.reread: reopened %s for read",
                CLSS, name))
            json = input!!.readLine()
        }
        catch (ex: Exception) {
            LOGGER.info(String.format("%s.reread: ERROR opening %s for read (%s)",
                CLSS, name, ex.message))
        }
        LOGGER.info(String.format("%s.reread: got %s", CLSS, json))
        return json
    }

    private val CLSS = "NamedSocket"
    private val LOGGER = Logger.getLogger(CLSS)
    private val CLIENT_ATTEMPT_INTERVAL: Long = 5000 // 5 secs
    private val CLIENT_LOG_INTERVAL = 10
    private val SERVER_ATTEMPT_INTERVAL: Long = 15000 // 15 secs
}