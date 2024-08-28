/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.data.LogData
import chuckcoughlin.bertspeak.data.LogDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentPosesBinding
import chuckcoughlin.bertspeak.ui.adapter.LogDataAdapter

/**
 * This fragment allows the user to select a pose stored in the robot, then
 * displays all the position settings in that pose.
 */
class PosesFragment(pos:Int) : BasicAssistantFragment(pos), LogDataObserver {
    override val name : String
    private var adapter: LogDataAdapter
    private lateinit var refreshButton: Button

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        super.onCreate(savedInstanceState)
        val binding = FragmentPosesBinding.inflate(inflater,container,false)
        refreshButton = binding.poseRefreshButton
        refreshButton.setOnClickListener { refreshButtonClicked() }
        return binding.root
    }

    // Bind to the DispatchService
    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    //============================= Button Callbacks ================================
    //
    fun refreshButtonClicked() {
        Log.i(name, "Refresh button clicked")
        adapter.reportDataSetChanged()
    }


    override fun resetText(list:List<LogData>) {
        Log.i(name, "reset: message list now ...")
        adapter.resetList(list)
        adapter.reportDataSetChanged()
    }

    @Synchronized
    override fun updateText(msg: LogData) {
        Log.i(name, String.format("update: message = %s", msg.message))
        adapter.reportDataSetChanged()
    }

    val CLSS = "PosesFragment"

    init {
        name = CLSS
        adapter = LogDataAdapter(FixedSizeList<LogData>(BertConstants.NUM_POSES))
    }
}
