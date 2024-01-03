/**
 * Copyright 2019-2023. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.service

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.service.ManagerState.ACTIVE
import chuckcoughlin.bertspeak.service.ManagerState.ERROR
import kotlinx.coroutines.NonCancellable.start
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Thread.UncaughtExceptionHandler
import java.util.UUID


/**
 * This socket communicates across a Bluetooth network to the robot which acts
 * as a server. The messages are simple text strings. We append a new-line
 * on write and expect one on read.
 *
 * The file descriptors are opened on "openConnections" and closed on
 * "shutdown". Change listeners are notified (in a separate Thread) when the
 * socket is "ready".
 *
 * Constructor: Use this version for processes that are clients
 * @param handler the parent fragment
*/
class SocketManager(service:DispatchService): CommunicationManager {
    override val managerType = ManagerType.SOCKET
    override var managerState = ManagerState.OFF
    private val buffer: CharArray
    private val dispatcher = service
    private var connectionThread: ConnectionThread? = null
    private var readerThread: ReaderThread? = null
    private lateinit var device: BluetoothDevice
    private var deviceName: String
    private var _socket: BluetoothSocket? = null
    private val socket get() = _socket!!        // Must always be protected by test for null
    private var input: BufferedReader? = null
    private var output: PrintWriter? = null

    /**
     * This is required before we can be started
     */
    @Synchronized
    fun receivePairedDevice(dev:BluetoothDevice) {
        device = dev
        deviceName = device.name
    }
    override fun start() {
            if (connectionThread != null && connectionThread!!.isAlive && !connectionThread!!.isInterrupted) {
                Log.i(CLSS, "socket connection already in progress ...")
                return
            }
            connectionThread = ConnectionThread(device)
            connectionThread!!.start()
    }

    private fun stopChecking() {
        if (connectionThread != null && connectionThread!!.isAlive) {
            connectionThread!!.interrupt()
        }
    }

    /**
     * Close IO streams.
     */
    override fun stop() {
        stopChecking()
        if (input != null) {
            try {
                input!!.close()
            }
            catch (ignore: IOException) {
            }
            input = null
        }
        if (output != null) {
            output!!.close()
            output = null
        }
        try {
            if (_socket != null) {
                _socket = null
                socket.close()
            }
        }
        catch (_: IOException) {
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
                Log.e(CLSS,
                    String.format("read: Error reading from %s before connection",
                    deviceName))
            }
        }
        catch (ioe: IOException) {
            Log.e(CLSS, String.format("read: Error reading from %s (%s)",
                    deviceName, ioe.localizedMessage))
            // Close and attempt to reopen port
            text = reread()
        }
        catch (npe: NullPointerException) {
            Log.e(CLSS, String.format( "read: Null pointer reading from %s (%s)",
                    deviceName, npe.localizedMessage))

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
        try {
            if (output != null) {
                if (!output!!.checkError()) {
                    Log.i(
                        CLSS, String.format(
                            "write: writing ... %s (%d bytes) to %s.",
                            text, text.length + 1, deviceName
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
                Log.e(CLSS, String.format(
                        "write: Device(%s) output is undefined  before connection",
                        deviceName))
            }
        }
        catch (ex: Exception) {
            Log.e(CLSS,String.format("write: Error writing %d bytes to %s(%s)",
                    text.length,deviceName,ex.localizedMessage),ex)
        }
    }

    /**
     * We've gotten a null when reading the socket. This means, as far as I can tell, that the other end has
     * shut down. Close the socket and re-open or re-listen/accept. We hang until this succeeds.
     * @return the next
     */
    private fun reread(): String? {
        var text: String? = null
        Log.i(CLSS, String.format("reread: on %s", deviceName))
        stop()
        start()
        try {
            input = BufferedReader(InputStreamReader(socket.inputStream))
            Log.i(CLSS, String.format("reread: reopened %s for read", deviceName))
            text = input!!.readLine()
        }
        catch(ex: Exception) {
            Log.i(CLSS,String.format("reread: ERROR opening %s for read (%s)", deviceName, ex.message)
            )
        }
        Log.i(CLSS, String.format("reread: got %s", text))
        return text
    }

    // =================== Connection Thread ==================
    /**
     * Check for the network in a separate thread.
     */
    private inner class ConnectionThread(val device: BluetoothDevice) : Thread() {
        /**
         * We are a client. Attempt to connect to the server.
         */
        override fun run() {
            var reason: String?

            // Keep attempting a connection until the server is ready
            var attempts = 0
            var logged = false
            while(true) {
                try {
                    var uuid: UUID? = null
                    if(device.fetchUuidsWithSdp()) {
                        val uuids = device.uuids
                        Log.i(CLSS, String.format(
                            "run: %s returned %d service UUIDs",
                            deviceName, uuids.size))
                        for(id in uuids) {
                            uuid = id.uuid
                            if(!logged) Log.i(CLSS, String.format(
                                    "run: %s: service UUID = %s",deviceName, uuid.toString()))
                        }
                        if(uuid == null) {
                            reason =
                                String.format("There were no service UUIDs found on %s", deviceName)
                            Log.w(CLSS, String.format("run: ERROR %s", reason))
                            dispatcher.logError(managerType,reason)
                            managerState = ManagerState.ERROR
                            dispatcher.reportManagerState(managerType,managerState)
                            break
                        }
                        logged = true
                    }
                    else {
                        reason = String.format(
                            "The tablet failed to fetch service UUIDS to %s",deviceName)
                        Log.w(CLSS, String.format("run: ERROR %s", reason))
                        dispatcher.logError(managerType,reason)

                        break
                    }
                    Log.i(CLSS,String.format("run: creating insecure RFComm socket for %s ...",
                            SERIAL_UUID))
                    _socket = device.createInsecureRfcommSocketToServiceRecord(SERIAL_UUID)
                    Log.i(CLSS, String.format( "run: attempting to connect to %s ...",
                            deviceName) )
                    socket.connect()
                    Log.i(CLSS,String.format("run: connected to %s after %d attempts",
                            deviceName,attempts))
                    reason = openPorts()
                    break
                }
                catch(se: SecurityException) {
                    Log.w(CLSS, String.format(
                            "run: SecurityException connecting to socket (%s)",
                            se.localizedMessage))
                }
                catch(ioe: IOException) {
                    Log.w(CLSS, String.format(
                        "run: IOException connecting to socket (%s)",
                        ioe.localizedMessage ) )
                    // See: https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3
                    try {
                        sleep(CLIENT_ATTEMPT_INTERVAL)
                    }
                    catch(ie: InterruptedException) {
                        if(attempts % CLIENT_LOG_INTERVAL == 0) {
                            reason = String.format(
                                "The tablet failed to create a client socket to %s due to %s",
                                deviceName, ioe.message
                            )
                            Log.w(CLSS, String.format("run: ERROR %s", reason))
                            dispatcher.logError(managerType,reason)
                        }
                    }
                }
                attempts++
            }
            if(reason == null) {
                managerState = ACTIVE
            }
            else {
                dispatcher.logError(managerType,reason)
                managerState = ACTIVE
            }
            dispatcher.reportManagerState(managerType,managerState)
        }

        /**
         * Open IO streams for reading and writing. The socket must exist.
         * @return an error description. Null if no error.
         */
        private fun openPorts(): String? {
            var reason: String? = null
            try {
                input = BufferedReader(InputStreamReader(socket.inputStream))
                Log.i(CLSS, String.format("openPorts: opened %s for read", deviceName))
                try {
                    output = PrintWriter(socket.outputStream, true)
                    Log.i(CLSS, String.format("openPorts: opened %s for write", deviceName))
                    write(String.format("%s:the tablet is connected", MessageType.LOG.name))
                }
                catch (ex: Exception) {
                    reason = String.format(
                        "The tablet failed to open a socket for writing due to %s",
                        ex.message
                    )
                    Log.i( CLSS,String.format("openPorts: ERROR opening %s for write (%s)",
                        CLSS,deviceName,ex.message),ex)
                    dispatcher.logError(managerType,reason)
                }
            }
            catch (ex: Exception) {
                reason = String.format(
                    "The tablet failed to open a socket for reading due to %s",
                    ex.message
                )
                Log.i(CLSS,String.format("openPorts: ERROR opening %s for read (%s)",CLSS,deviceName,ex.message),
                    ex)
                dispatcher.logError(managerType,reason)
            }
            return reason
        }

        init {
            isDaemon = true
            // don't require callers to explicitly kill all the old checker threads.
            uncaughtExceptionHandler = UncaughtExceptionHandler { _, ex ->
                val msg = String.format("There was an uncaught exception creating socket connection: %s",ex.localizedMessage)
                Log.e(CLSS, msg, ex)
                dispatcher.logError(managerType,msg)
            }
        }
    }
    // End of ConnectionThread

    // ================ Reader Thread =================
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
                    dispatcher.receiveText(text)
                    sleep(100)
                }
                catch (_: InterruptedException) {}
            }
        }
    }  // End of ReaderThread

    val CLSS = "SocketManager"
    val BUFFER_SIZE = 256
    val CLIENT_ATTEMPT_INTERVAL: Long = 2000 // 2 secs
    val CLIENT_LOG_INTERVAL = 10

    // Well-known port for Bluetooth serial port service
    val SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    init {
        buffer = CharArray(BUFFER_SIZE)
        deviceName = BertConstants.NO_DEVICE
    }
}
