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
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.data.TextDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentPosesBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.adapter.TextDataAdapter

/**
 * This fragment allows the user to select a pose stored in the robot, then
 * displays all the position settings in that pose.
 */
class PosesFragment(pos:Int) : BasicAssistantFragment(pos), TextDataObserver {
    override val name : String
    private var adapter: TextDataAdapter
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


    override fun resetText(list:List<TextData>) {
        Log.i(name, "reset: message list now ...")
        adapter.resetList(list)
        adapter.reportDataSetChanged()
    }

    @Synchronized
    override fun updateText(msg: TextData) {
        Log.i(name, String.format("update: message = %s", msg.message))
        adapter.reportDataSetChanged()
    }

    val CLSS = "PosesFragment"

    init {
        name = CLSS
        adapter = TextDataAdapter(FixedSizeList<TextData>(BertConstants.NUM_POSES))
    }
}
