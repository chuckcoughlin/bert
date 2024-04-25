/**
 * Copyright 2019-2024. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bertspeak.service

import android.Manifest.permission
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.service.ManagerState.ACTIVE
import chuckcoughlin.bertspeak.service.ManagerState.ERROR
import chuckcoughlin.bertspeak.service.ManagerState.PENDING
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.start
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
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
    private var readerThread: ReaderThread? = null
    private var connected: Boolean
    private lateinit var device: BluetoothDevice
    private lateinit var socket: BluetoothSocket
    private var deviceName: String
    private var running: Boolean
    private var job: Job

    private var input: BufferedReader? = null
    private var output: PrintWriter? = null

    /**
     * This is required before we can be started
     */
    @Synchronized
    fun receivePairedDevice(dev:BluetoothDevice) {
        device = dev
        if(ActivityCompat.checkSelfPermission(dispatcher.context, permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(CLSS, "receivePairedDevice: Permission check for BluetoothConnect failed")
            return
        }
        deviceName = dev.name
        managerState = PENDING
        dispatcher.reportManagerState(managerType,managerState)
    }

    /* Do not start the manager until the bluetooth device has been defined.
     */
    override fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            connectSocket()
        }
        reason = openPorts()
        managerState = PENDING
        dispatcher.reportManagerState(managerType,managerState)
    }


    /**
     * We are a client. Attempt to connect to the server.
     * Set the connected flag
     */
    private suspend fun connectSocket() : Boolean {
        // Keep attempting a connection until the server is ready
        var attempts = 0
        var reason = ""
        while(!connected && reason.isBlank()) {
            try {
                var uuid: UUID? = null
                if(device.fetchUuidsWithSdp()) {
                    val uuids = device.uuids
                    Log.i(CLSS, String.format("connectSocket: %s returned %d service UUIDs",
                        deviceName, uuids.size))
                    for(id in uuids) {
                        uuid = id.uuid
                        Log.i(CLSS, String.format(
                            "connectSocket: %s: service UUID = %s", deviceName, uuid.toString()))
                    }
                    if(uuid == null) {
                        reason = String.format("There were no service UUIDs found on %s", deviceName)
                    }
                    else {
                        Log.i(CLSS, String.format("connectSocket: creating RFComm socket for %s ...",SERIAL_UUID))
                        socket = device.createRfcommSocketToServiceRecord(SERIAL_UUID)!!
                        Log.i(CLSS, String.format("connectSocket: attempting to connect to %s ...",
                            deviceName))
                        socket.connect()
                        connected = true
                        Log.i(CLSS, String.format("connectSocket: connected to %s after %d attempts",
                            deviceName, attempts))
                    }
                }
                else {
                    reason = String.format("The tablet failed to fetch service UUIDS to %s", deviceName)
                }
            }
            catch(se: SecurityException) {
                reason = String.format("connectSocket: security exception (%s)", se.localizedMessage)
            }
            // See: https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3
            catch(ioe: IOException) {
                reason = String.format("connectSocket: IOException connecting to socket (%s)", ioe.localizedMessage)
            }

            if(!connected) {
                delay(CLIENT_ATTEMPT_INTERVAL)

                if(attempts % CLIENT_LOG_INTERVAL == 0) {
                    val msg = String.format(
                        "The tablet failed to create a client socket to %s due to %s",
                        deviceName, ioe.message)
                    Log.w(CLSS, String.format("run: ERROR %s", reason))
                    dispatcher.logError(managerType, reason)
                }
            }
        }
        attempts++
        }
        // ========== End While ======================

        if( connected ) {
            managerState = ACTIVE
        }
        else if( reason.isNotBlank() ){
            Log.w(CLSS, String.format("connectSocket: ERROR %s", reason))
            dispatcher.logError(managerType,reason)
            managerState = ERROR
        }
        dispatcher.reportManagerState(managerType,managerState)

    }
    /**
     * While running, this controller processes messages between the Dispatcher
     * and a user terminal. A few messages are intercepted that totally local
     * in nature (SLEEP,WAKE).
     *
     * A response to a request that starts here will have the source as Termonal.
     * When the application is run autonomously (like as a system service), the
     * terminal is not used.
     */
    @DelicateCoroutinesApi
    suspend fun execute(): Unit = coroutineScope {
        if (!running) {
            LOGGER.info(String.format("%s.execute: started...", CLSS))
            running = true
            /* select() in coroutine to balance sends/receives from Dispatcher */
            job = scope.launch(Dispatchers.IO) {
                while (running) {
                    print(prompt)
                    select<MessageBottle> {

                        handleUserInput().onAwait{it}
                    } // End select
                }
            }
        }
        else {
            LOGGER.warning(String.format("%s.execute: attempted to start, but already running...", CLSS))
        }
    }

    private fun stopChecking() {
        if (connectionThread.isAlive) {
            connectionThread.interrupt()
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
            catch (ignore: IOException) {}
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
        managerState = ManagerState.OFF
        dispatcher.reportManagerState(managerType, managerState)
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
                    Log.i(CLSS, String.format( "write: writing ... %s (%d bytes) to %s.",
                            text, text.length + 1, deviceName))
                    output!!.println(text) // Appends new-line
                    output!!.flush()
                }
                else {
                    Log.e(CLSS, String.format("write: out stream error", deviceName))
                }
            }
            else if(!deviceName.equals(BertConstants.NO_DEVICE)) {
                Log.e(CLSS, String.format("write: Device(%s) output is undefined  before connection",
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
            Log.i(CLSS,String.format("reread: ERROR opening %s for read (%s)", deviceName, ex.message))
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
        connected = false
        deviceName = BertConstants.NO_DEVICE
        running  = false
        job = Job()
    }
}
