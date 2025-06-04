/**
 * Copyright 2019-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.data.JsonObserver
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.JsonType.LINK_LOCATIONS
import chuckcoughlin.bertspeak.data.StatusData
import chuckcoughlin.bertspeak.data.StatusObserver
import chuckcoughlin.bertspeak.databinding.FragmentPropertiesBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.service.ManagerType

/**
 * This fragment displays servo data from the robot in tabular form. Only
 * one table is displayed at a time and is completely replaced when the
 * next set of data are read.
 */
class MotorPropertiesFragment(pos:Int) : BasicAssistantFragment(pos), JsonObserver, StatusObserver {
    override val name : String


    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        super.onCreate(savedInstanceState)
        val binding = FragmentPropertiesBinding.inflate(inflater,container,false)
        //val tableLayout = binding.statusTableView

        var button = binding.propertiesRefreshButton
        button.setOnClickListener { refreshButtonClicked() }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        DispatchService.registerForStatus(this)
        DispatchService.registerForJson(this)
    }


    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForStatus(this)
        DispatchService.unregisterForJson(this)
    }


    override fun resetItem(map:Map<JsonType,String>) {
        val json = map.get(JsonType.MOTOR_PROPERTIES)
        if( json!=null ) {
            Log.i(name, "resetItem: ...")
        }
    }

    override fun updateItem(type:JsonType,json:String) {
        if( type==JsonType.MOTOR_PROPERTIES ) {
            Log.i(name, String.format("update: data = %s", json))
        }
    }
    fun refreshButtonClicked() {
        DispatchService.sendJsonRequest(JsonType.MOTOR_PROPERTIES)
    }
    // ===================== StatusDataObserver =====================
    override fun resetStatus(list: List<StatusData>) {
        for (ddata in list) {
            updateStatus(ddata)
        }
    }

    /**
     * When the SocketManager comes online, request a motor properties update.
     */
    override fun updateStatus(data: StatusData) {
        Log.i(name, String.format("update (%s):%s = %s",data.action,data.type,data.state))
        if (data.action.equals(DispatchConstants.ACTION_MANAGER_STATE)) {
            if( data.type== ManagerType.SOCKET && data.state== ManagerState.ACTIVE ) {
                DispatchService.sendJsonRequest(JsonType.MOTOR_PROPERTIES)
            }
        }
    }

    val CLSS = "MotorPropertiesFragment"

    init {
        name = CLSS
    }
}
