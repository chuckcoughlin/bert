/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.share.controller

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
    val isServer // True if this instance is owned by the server.
            : Boolean
    private var serverSocket: ServerSocket?
    private var socket: Socket?
    private var `in`: BufferedReader? = null
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
    fun create(): Boolean {
        var success = true
        var attempts = 1
        if (isServer) {
            while (true) {
                try {
                    serverSocket = ServerSocket(port)
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.create: %s as server listening on port %d",
                        chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, port))
                    socket = serverSocket!!.accept()
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(
                        String.format(
                            "%s.create: %s accepted connection on port %d after %d attempts",
                            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                            name,
                            port,
                            attempts
                        )
                    )
                    success = true
                    break
                } catch (ex: Exception) {
                    success = false
                    socket = null
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.severe(
                        String.format(
                            "%s.create: ERROR creating server socket %s (%s)",
                            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                            name,
                            ex.message
                        )
                    )
                    try {
                        Thread.sleep(chuckcoughlin.bert.share.controller.NamedSocket.Companion.SERVER_ATTEMPT_INTERVAL) // Something bad has happened, we don't want a hard loop
                    } catch (ignore: InterruptedException) {
                    }
                }
                attempts++
            }
        } else {
            // Keep attempting a connection until the server is ready
            while (true) {
                try {
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(
                        String.format(
                            "%s.create: %s attempting to connect to server %s on %d ...",
                            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                            name,
                            host,
                            port
                        )
                    )
                    socket = Socket(host, port)
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(
                        String.format(
                            "%s.create: new %s connection from %s on %d after %d attempts",
                            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                            name,
                            host,
                            port,
                            attempts
                        )
                    )
                    success = true
                    break
                } catch (ioe: IOException) {
                    // Get a "connection refused" when remote party is not running yet.
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(
                        String.format(
                            "%s.create: ERROR connecting to server socket %s:%d (%s)",
                            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                            host,
                            port,
                            ioe.message
                        )
                    )
                    try {
                        Thread.sleep(chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLIENT_ATTEMPT_INTERVAL)
                    } catch (ie: InterruptedException) {
                        if (attempts % chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLIENT_LOG_INTERVAL == 0) {
                            chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.warning(
                                String.format(
                                    "%s.create: ERROR creating client socket %s (%s)",
                                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                                    name,
                                    ioe.message
                                )
                            )
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
                `in` = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.startup: opened %s for read",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
            } catch (ex: Exception) {
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.startup: ERROR opening %s for read (%s)",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, ex.message))
            }
            try {
                out = PrintWriter(socket!!.getOutputStream(), true)
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.startup: opened %s for write",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
            } catch (ex: Exception) {
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.startup: ERROR opening %s for write (%s)",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, ex.message))
            }
        }
    }

    /**
     * Close IO streams. Closing the socket should interrupt any read.
     */
    fun shutdown() {
        chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.shutdown: %s closing sockets ...",
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
        try {
            if (socket != null) socket!!.close()
            if (serverSocket != null) serverSocket!!.close()
        } catch (ioe: IOException) {
        }
        chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.shutdown: %s closing in ...",
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
        if (`in` != null) {
            try {
                `in`!!.close()
            } catch (ignore: IOException) {
            }
            `in` = null
        }
        chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.shutdown: %s closing out ...",
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
        if (out != null) {
            out!!.close()
            out = null
        }
        chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.shutdown: %s complete.",
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
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
            if (`in` != null) {
                //LOGGER.info(String.format("%s.read: reading %s ... ",CLSS,name));
                var json = `in`!!.readLine()
                while (json == null) {
                    json = reread()
                }
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.read: %s got %s",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, json))
                if (json != null) bottle = MessageBottle.fromJSON(json)
            }
            else {
                LOGGER.warning(String.format(
                        "%s.read: Attempt to read from %s before port is open (ignored)",
                        chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                        name
                    )
                )
            }
        } catch (npe: NullPointerException) {
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.severe(String.format("%s.read: Exception reading from %s (%s)",
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, npe.localizedMessage))
        } catch (ioe: IOException) {
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.severe(String.format("%s.read: Exception reading from %s (%s)",
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, ioe.localizedMessage))
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
            if (`in` != null) {
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.readLine: reading %s ... ",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
                text = `in`!!.readLine()
                while (text == null) {
                    try {
                        Thread.sleep(10000L)
                    } catch (ignore: InterruptedException) {
                    }
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.readLine: got null, retrying",
                        chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS
                    ))
                    //text = reread();  // May not need
                    text = `in`!!.readLine()
                }
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.readLine: got %s",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, text))
            } else {
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.warning(
                    String.format(
                        "%s.readLine: Attempt to read from %s before port is open )ignored)",
                        chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                        name
                    )
                )
            }
        } catch (ioe: IOException) {
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.severe(
                String.format(
                    "%s.readLine: Exception reading from %s (%s)",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                    name,
                    ioe.localizedMessage
                )
            )
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
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.write: wrote %s %d bytes. ",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, json!!.length))
            } else {
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.warning(
                    String.format(
                        "%s.write: Attempt to write to %s before port is open (ignored)",
                        chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                        name
                    )
                )
            }
        } catch (ioe: Exception) {
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.severe(
                String.format(
                    "%s.write: Exception writing %d bytes (%s)",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                    json!!.length,
                    ioe.localizedMessage
                )
            )
        }
    }

    /**
     * Write plain text to the socket. (No added line-feed)
     */
    fun write(text: String) {
        try {
            if (out != null) {
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.write: wrote %s %d bytes (%s)",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, text.length, text))
                out!!.println(text) // Appends new-line
                out!!.flush()
            } else {
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.write: Attempt to write to %s before port is open (ignored)",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
            }
        } catch (ioe: Exception) {
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.severe(
                String.format(
                    "%s.write: Exception writing %d bytes (%s)",
                    chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS,
                    text.length,
                    ioe.localizedMessage
                )
            )
        }
    }

    /**
     * We've gotten a null when reading the socket. This means, as far as I can tell, that the other end has
     * shut down. Close the socket and re-open or re-listen/accept. We hang until this succeeds.
     * @return the next
     */
    private fun reread(): String? {
        var json: String? = null
        chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.reread: on port %s",
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
        if (`in` != null) try {
            `in`!!.close()
        } catch (ignore: IOException) {
        }
        if (socket != null) try {
            socket!!.close()
        } catch (ignore: IOException) {
        }
        if (serverSocket != null) try {
            serverSocket!!.close()
        } catch (ignore: IOException) {
        }
        create()
        try {
            `in` = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.reread: reopened %s for read",
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name))
            json = `in`!!.readLine()
        } catch (ex: Exception) {
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.reread: ERROR opening %s for read (%s)",
                chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, name, ex.message))
        }
        chuckcoughlin.bert.share.controller.NamedSocket.Companion.LOGGER.info(String.format("%s.reread: got %s",
            chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS, json))
        return json
    }

    companion object {
        private const val CLSS = "NamedSocket"
        private val LOGGER = Logger.getLogger(chuckcoughlin.bert.share.controller.NamedSocket.Companion.CLSS)
        private const val CLIENT_ATTEMPT_INTERVAL: Long = 5000 // 5 secs
        private const val CLIENT_LOG_INTERVAL = 10
        private const val SERVER_ATTEMPT_INTERVAL: Long = 15000 // 15 secs
    }
}