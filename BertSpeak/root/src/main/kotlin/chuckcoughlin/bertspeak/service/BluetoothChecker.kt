/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.util.Log
import java.lang.Thread.UncaughtExceptionHandler

/**
 * Attempt to discover a Bluetooth network.
 * NOTE: Bluetooth is not supported on the emulator. The adapter will be null.
 */
class BluetoothChecker(private val handler: BluetoothHandler, private val deviceName: String?) {
    private var checkerThread: CheckerThread? = null
    fun beginChecking(bmgr: BluetoothManager) {
        if (checkerThread != null && checkerThread!!.isAlive && !checkerThread!!.isInterrupted) {
            Log.i(CLSS, "check already in progress ...")
            return
        }
        val errMsg = checkAdapter(bmgr.adapter)
        if (errMsg.isEmpty()) {
            checkerThread = CheckerThread(bmgr)
            checkerThread!!.start()
        }
        else {
            handler.handleBluetoothError(errMsg)
        }
    }

    fun stopChecking() {
        if (checkerThread != null && checkerThread!!.isAlive) {
            checkerThread!!.interrupt()
        }
    }

    // An empty string returned implies success, else an error message.
    // For now we always return an error
    private fun checkAdapter(adapter: BluetoothAdapter?): String {
        var errorMsg = ""
        if (adapter == null) {
            errorMsg = "There is no bluetooth network"
        }
        else if (deviceName.isNullOrEmpty()) {
            errorMsg = "No bluetooth device has been specified"
        }
        else {
            if (!adapter.isEnabled) {
                errorMsg = "The bluetooth network is not enabled"
            }
        }
        return errorMsg
    }

    /**
     * Check for the network in a separate thread.
     */
    private inner class CheckerThread(bmgr: BluetoothManager) : Thread() {
        private val adapter: BluetoothAdapter
        private var pairedDevices: Set<BluetoothDevice>
        override fun run() {
            var success = false
            var errorMsg = ""
            try {
                adapter.startDiscovery()
                var cycle = 0
                while (cycle < 10 && !success) {
                    pairedDevices = adapter.bondedDevices
                    for (bluetoothDevice in pairedDevices) {
                        Log.i(
                            CLSS, String.format("%s: discovered %s (%s %s)",
                                CLSS,bluetoothDevice.name, bluetoothDevice.type, bluetoothDevice.address )
                        )
                        if (bluetoothDevice.name.equals(deviceName, ignoreCase = true)) {
                            success = true
                            Log.i(
                                CLSS,String.format("%s: Matched %s (%s %s)",CLSS,bluetoothDevice.name,bluetoothDevice.type,
                                    bluetoothDevice.address)
                            )
                            handler.setBluetoothDevice(bluetoothDevice)
                            break
                        }
                    }
                    sleep(1000)
                    cycle++
                }
                adapter.cancelDiscovery()
            }
            catch( se: SecurityException) {
                errorMsg = String.format("%s user has rejected permission for %s",
                    se.localizedMessage,deviceName)
                Log.e(CLSS, errorMsg, se)
            }
            catch (ex: Throwable) {
                errorMsg = String.format("%s while searching Bluetooth devices for %s",
                    ex.localizedMessage,deviceName)
                Log.e(CLSS, errorMsg, ex)
            }
            if (success) {
                Log.i(CLSS, String.format("BluetoothChecker: Paired to %s", deviceName))
                handler.receiveBluetoothConnection()
            } else {
                handler.handleBluetoothError(errorMsg)
            }
        }

        init {
            adapter = bmgr.adapter
            pairedDevices = HashSet()
            isDaemon = true
            // don't require callers to explicitly kill all the old checker threads.
            uncaughtExceptionHandler = UncaughtExceptionHandler { _, ex ->
                val msg = String.format(
                    "There was an uncaught exception checking bluetooth: %s",
                    ex.localizedMessage
                )
                Log.e(CLSS, msg, ex)
                handler.handleBluetoothError(msg)
            }
        }
    }

    companion object {
        private const val CLSS = "BluetoothChecker"
    }
}
