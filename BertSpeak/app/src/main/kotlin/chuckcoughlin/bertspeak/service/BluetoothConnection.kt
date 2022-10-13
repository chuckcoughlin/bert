/**
 * Copyright 2019. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.service

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import chuckcoughlin.bertspeak.common.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Exception
import java.lang.NullPointerException
import java.util.*

/**
 * This socket communicates across a Bluetooth network to the robot which acts
 * as a server. The messages are simple text strings. We append a new-line
 * on write and expect one on read.
 *
 * The file descriptors are opened on "openConnections" and closed on
 * "shutdown". Change listeners are notified (in a separate Thread) when the
 * socket is "ready".
 */
class BluetoothConnection(handler: BluetoothHandler) {
    private val handler: BluetoothHandler
    private var connectionThread: ConnectionThread? = null
    private var readerThread: ReaderThread? = null
    private var device: BluetoothDevice? = null
    private var socket: BluetoothSocket? = null
    private var input: BufferedReader? = null
    private var output: PrintWriter? = null
    private val buffer: CharArray

    fun openConnections(dev: BluetoothDevice) {
        device = dev
        if (connectionThread != null && connectionThread!!.isAlive && !connectionThread!!.isInterrupted) {
            Log.i(CLSS, "socket connection already in progress ...")
            return
        }
        connectionThread = ConnectionThread(device)
        connectionThread!!.start()
    }

    fun stopChecking() {
        if (connectionThread != null && connectionThread!!.isAlive) {
            connectionThread!!.interrupt()
        }
    }

    /**
     * Close IO streams.
     */
    fun shutdown() {
        stopChecking()
        if (input != null) {
            try {
                input!!.close()
            } catch (ignore: IOException) {
            }
            input = null
        }
        if (output != null) {
            output!!.close()
            output = null
        }
        try {
            if (socket != null) socket!!.close()
        }
        catch (ioe: IOException) {
        }
    }

    /**
     * Read a line of text from the socket. The read will block and wait for data to appear.
     * If we get a null, then close the socket and either re-open or re-listen
     * depending on whether or not this is the server side, or not.
     *
     * @return the line of text
     */
    fun read(): String? {
        var text: String? = null
        try {
            if (input != null) {
                Log.i(CLSS, String.format("read: reading ... "))
                text = input!!.readLine() // Does not include CR
                Log.i(CLSS, String.format("read: returning: %s", text))
            }
            else {
                Log.e(
                    CLSS,
                    String.format("read: Error reading from %s before connection", device!!.name)
                )
            }
        }
        catch (ioe: IOException) {
            Log.e(
                CLSS,
                String.format("read: Error reading from %s (%s)", device!!.name,ioe.localizedMessage)
            )
            // Close and attempt to reopen port
            text = reread()
        }
        catch (npe: NullPointerException) {
            Log.e(CLSS,
                String.format("read: Null pointer reading from %s (%s)",device!!.name, npe.localizedMessage))

            // Close and attempt to reopen port
            text = reread()
        }
        return text
    }

    /**
     * Start a thread that loops forever in a blocking read.
     */
    fun readInThread() {
        if (readerThread == null) {
            readerThread = ReaderThread()
            readerThread!!.start()
        }
    }

    /**
     * Write plain text to the socket.
     */
    fun write(text: String) {
        val deviceName =
            if (device == null) "No device" else if (device!!.name == null) "No name" else device!!.name
        try {
            if (output != null) {
                if (!output!!.checkError()) {
                    Log.i(
                        CLSS,
                        String.format(
                            "write: writing ... %s (%d bytes) to %s.",
                            text,
                            text.length + 1,
                            deviceName
                        )
                    )
                    output!!.println(text) // Appends new-line
                    output!!.flush()
                }
                else {
                    Log.e(CLSS, String.format("write: out stream error", deviceName))
                }
            }
            else {
                Log.e(
                    CLSS,
                    String.format("write: Error writing to %s before connection", deviceName)
                )
            }
        } catch (ex: Exception) {
            Log.e(
                CLSS, String.format(
                    "write: Error writing %d bytes to %s(%s)", text.length, deviceName,
                    ex.localizedMessage
                ), ex
            )
        }
    }

    /**
     * We've gotten a null when reading the socket. This means, as far as I can tell, that the other end has
     * shut down. Close the socket and re-open or re-listen/accept. We hang until this succeeds.
     * @return the next
     */
    private fun reread(): String? {
        var text: String? = null
        Log.i(CLSS, String.format("reread: on %s", device!!.name))
        shutdown()
        openConnections(device)
        try {
            input = BufferedReader(InputStreamReader(socket!!.inputStream))
            Log.i(CLSS, String.format("reread: reopened %s for read", device!!.name))
            text = input!!.readLine()
        }
        catch (ex: Exception) {
            Log.i(
                CLSS,String.format("reread: ERROR opening %s for read (%s)", device!!.name, ex.message)
            )
        }
        Log.i(CLSS, String.format("reread: got %s", text))
        return text
    }
    // ================================================= Connection Thread =========================
    /**
     * Check for the network in a separate thread.
     */
    private inner class ConnectionThread(private val device: BluetoothDevice?) : Thread() {
        /**
         * We are a client. Attempt to connect to the server.
         */
        override fun run() {
            var reason: String? = null

            // Keep attempting a connection until the server is ready
            var attempts = 0
            var logged = false
            while (true) {
                try {
                    var uuid: UUID? = null
                    if (device!!.fetchUuidsWithSdp()) {
                        val uuids = device.uuids
                        Log.i(
                            CLSS,
                            String.format(
                                "run: %s returned %d service UUIDs",device.name,uuids.size
                            )
                        )
                        for (id in uuids) {
                            uuid = id.uuid
                            if (!logged) Log.i(
                                CLSS,
                                String.format(
                                    "run: %s: service UUID = %s",device!!.name,uuid.toString()
                                )
                            )
                        }
                        if (uuid == null) {
                            reason = String.format("There were no service UUIDs found on %s",device.name)
                            Log.w(CLSS, String.format("run: ERROR %s", reason))
                            handler.handleSocketError(reason)
                            break
                        }
                        logged = true
                    }
                    else {
                        reason = String.format("The tablet failed to fetch service UUIDS to %s",device.name)
                        Log.w(CLSS, String.format("run: ERROR %s", reason))
                        handler.handleSocketError(reason)
                        break
                    }
                    Log.i(CLSS,String.format("run: creating insecure RFComm socket for %s ...",SERIAL_UUID)
                    )
                    socket = device.createInsecureRfcommSocketToServiceRecord(SERIAL_UUID)
                    Log.i(CLSS, String.format("run: attempting to connect to %s ...", device.name))
                    socket!!.connect()
                    Log.i(
                        CLSS,
                        String.format(
                            "run: connected to %s after %d attempts",
                            device.name,
                            attempts
                        )
                    )
                    reason = openPorts()
                    break
                } catch (ioe: IOException) {
                    Log.w(
                        CLSS,
                        String.format(
                            "run: IOException connecting to socket (%s)",
                            ioe.localizedMessage
                        )
                    )
                    // See: https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3
                    try {
                        sleep(CLIENT_ATTEMPT_INTERVAL)
                    }
                    catch (ie: InterruptedException) {
                        if (attempts % CLIENT_LOG_INTERVAL == 0) {
                            reason = String.format(
                                "The tablet failed to create a client socket to %s due to %s",
                                device!!.name,
                                ioe.message
                            )
                            Log.w(CLSS, String.format("run: ERROR %s", reason))
                            handler.handleSocketError(reason)
                        }
                    }
                }
                attempts++
            }
            if (reason == null) {
                handler.receiveSocketConnection()
            }
            else {
                handler.handleBluetoothError(reason)
            }
        }

        /**
         * Open IO streams for reading and writing. The socket must exist.
         * @return an error description. Null if no error.
         */
        private fun openPorts(): String? {
            var reason: String? = null
            if (socket != null) {
                try {
                    input = BufferedReader(InputStreamReader(socket!!.inputStream))
                    Log.i(CLSS, String.format("openPorts: opened %s for read", device!!.name))
                    try {
                        output = PrintWriter(socket!!.outputStream, true)
                        Log.i(CLSS, String.format("openPorts: opened %s for write", device.name))
                        write(String.format("%s:the tablet is connected", MessageType.LOG.name))
                    }
                    catch (ex: Exception) {
                        reason = String.format(
                            "The tablet failed to open a socket for writing due to %s",
                            ex.message
                        )
                        Log.i(
                            CLSS,
                            String.format(
                                "openPorts: ERROR opening %s for write (%s)",
                                CLSS,
                                device.name,
                                ex.message
                            ),
                            ex
                        )
                        handler.handleSocketError(reason)
                    }
                } catch (ex: Exception) {
                    reason = String.format(
                        "The tablet failed to open a socket for reading due to %s",
                        ex.message
                    )
                    Log.i(CLSS,String.format("openPorts: ERROR opening %s for read (%s)",CLSS,device!!.name,ex.message),
                        ex)
                    handler.handleSocketError(reason)
                }
            }
            return reason
        }

        init {
            isDaemon = true
            // don't require callers to explicitly kill all the old checker threads.
            uncaughtExceptionHandler = UncaughtExceptionHandler { thread, ex ->
                val msg = String.format("There was an uncaught exception creating socket connection: %s",ex.localizedMessage)
                Log.e(CLSS, msg, ex)
                handler.handleSocketError(msg)
            }
        }
    }
    // ================================================= Connection Thread =========================
    /**
     * Check for the network in a separate thread.
     */
    private inner class ReaderThread : Thread() {
        /**
         * Read in a separate thread. Read blocks.
         */
        override fun run() {
            while (true) {
                try {
                    val text = read() ?: break
                    handler.receiveText(text)
                    sleep(100)
                }
                catch (ex: InterruptedException) {}
            }
        }
    }

    companion object {
        private const val CLSS = "BluetoothConnection"
        private const val BUFFER_SIZE = 256
        private const val CLIENT_ATTEMPT_INTERVAL: Long = 2000 // 2 secs
        private const val CLIENT_LOG_INTERVAL = 10

        // Well-known port for Bluetooth serial port service
        private val SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    /**
     * Constructor: Use this version for processes that are clients
     * @param handler the parent fragment
     */
    init {
        this.buffer = CharArray(BUFFER_SIZE)
        this.handler = handler
    }
}
