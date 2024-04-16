/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context.BLUETOOTH_SERVICE
import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.data.StatusDataObserver
import chuckcoughlin.bertspeak.db.DatabaseManager

/**
 * Attempt to discover a Bluetooth network. This manager must be run on its own
 * private thread (co-routine).
 * NOTE: Bluetooth is not supported on the emulator. The adapter will be null.
 */
class DiscoveryManager(service:DispatchService): CommunicationManager {
    override val managerType = ManagerType.DISCOVERY
    override var managerState = ManagerState.OFF
    private val dispatcher = service
    private val observers: MutableMap<String, StatusDataObserver>
    private val bmgr: BluetoothManager
    private var bluetoothDevice: BluetoothDevice? = null
    private var deviceName : String?

     override fun start() {
        observers.clear()
        var errorMsg = checkAdapter(bmgr.adapter)
        if(errorMsg.isEmpty()) {
            var success = false
            try {
                bmgr.adapter.startDiscovery()
                var cycle = 0
                while (cycle < 10 && !success) {
                    try {
                        Thread.sleep(1000)
                    }
                    catch(ex:InterruptedException){}
                    bluetoothDevice = getPairedDevice()
                    if(bluetoothDevice!=null) {
                        success = true
                    }
                    cycle++
                }
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
                dispatcher.receivePairedDevice(bluetoothDevice!!)
                managerState = ManagerState.ACTIVE
                dispatcher.reportManagerState(managerType,managerState)
            }
            else {
                errorMsg = "Failed to find a paired bluetooth device"
                dispatcher.logError(managerType,errorMsg)
                managerState = ManagerState.ERROR
                dispatcher.reportManagerState(managerType,managerState)
            }
            bmgr.adapter.cancelDiscovery()
        }
        else {

        }
    }

    override fun stop() {
        observers.clear()
    }


    // An empty string returned implies success, else an error message.
    // For now we always return an error
    fun checkAdapter(adapter: BluetoothAdapter): String {
        var errorMsg = ""
        if (bmgr.adapter == null) {
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

    fun getPairedDevice() : BluetoothDevice? {
        var device:BluetoothDevice? = null
        val pairedDevices = bmgr.adapter.bondedDevices
        if( deviceName!=null ) {
            for(dev in pairedDevices) {
                Log.i(
                    CLSS, String.format("%s: discovered %s (%s %s)",
                        CLSS, dev.name, dev.type, dev.address) )
                if(dev.name.equals(deviceName, ignoreCase = true)) {
                    Log.i(
                        CLSS, String.format("%s: Matched %s (%s %s)", CLSS, dev.name,
                            dev.type,dev.address) )
                    device = dev
                    break
                }
            }
        }
        return device
    }

    val CLSS = "DiscoveryManager"
    val MAX_CYCLES = 10

    /**
     * Initialize the Bluetooth
     */
    init {
        bmgr = dispatcher.context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        deviceName = DatabaseManager.getSetting(BertConstants.BERT_PAIRED_DEVICE)
        observers = mutableMapOf<String,StatusDataObserver>()
    }
}
