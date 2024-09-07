/**
 * Copyright 2019-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.data.GeometryData
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentStatusBinding
import chuckcoughlin.bertspeak.service.DispatchService

/**
 * This fragment displays servo data from the robot in tabular form. Only
 * one table is displayed at a time and is completely replaced when the
 * next set of data are read.
 */
class StatusFragment(pos:Int) : BasicAssistantFragment(pos), GeometryDataObserver {
    override val name : String


    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        super.onCreate(savedInstanceState)
        val binding = FragmentStatusBinding.inflate(inflater,container,false)
        //val tableLayout = binding.statusTableView

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        DispatchService.registerForGeometry(this)
    }


    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForGeometry(this)
    }


    override fun resetGeometry(geom: GeometryData) {
        Log.i(name, "resetGeometry: ...")
    }

    /**
     * Should not be called as we update these all at once.
     */
    override fun updateGeometry(geom: GeometryData) {
        Log.i(name, String.format("update: data = %s", geom))
    }

    val CLSS = "StatusFragment"

    init {
        name = CLSS
    }
}
