/**
 * Copyright 2023-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
*/
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.data.TextDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentLogsBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.adapter.TextDataAdapter

/**
 * This fragment shows log messages originating in the robot.
 */
class LogsFragment(pos:Int) : BasicAssistantFragment(pos), TextDataObserver {

    override val name: String
    private var frozen: Boolean
    private val adapter: TextDataAdapter
    private lateinit var freezeButton: Button

    /**
     * Save the frozen state in the bundle
     */
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(CLSS, String.format("onCreateView: will display %d messages", adapter.itemCount))
        if (savedInstanceState != null) frozen =
            savedInstanceState.getBoolean(BertConstants.BUNDLE_FROZEN, false)
        val binding = FragmentLogsBinding.inflate(inflater,container,false)
        var logMessageView = binding.logsRecyclerView   // RecyclerView
        logMessageView.setHasFixedSize(true) // Refers to the size of the layout.
        val layoutManager = LinearLayoutManager(logMessageView.getContext())
        logMessageView.setLayoutManager(layoutManager)
        logMessageView.setAdapter(adapter)
        val scrollPosition: Int = layoutManager.findFirstCompletelyVisibleItemPosition()
        logMessageView.scrollToPosition(scrollPosition)
        var button = binding.logClearButton
        button.setOnClickListener { clearButtonClicked() }
        freezeButton = binding.logFreezeButton
        freezeButton.setOnClickListener { freezeButtonClicked() }
        updateUI()
        return binding.root
    }
    // Bind to the DispatchService
    override fun onStart() {
        super.onStart()
        DispatchService.registerForLogs(this)
    }

    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForLogs(this)
    }

    override fun onSaveInstanceState(stateToSave: Bundle) {
        super.onSaveInstanceState(stateToSave)
        stateToSave.putBoolean(BertConstants.BUNDLE_FROZEN, frozen)
    }

    //============================= Button Callbacks ================================
    //
    fun clearButtonClicked() {
        Log.i(name, "Clear button clicked")
        DispatchService.clear(MessageType.LOG)
        adapter.reportDataSetChanged()
    }

    fun freezeButtonClicked() {
        frozen = !frozen
        if (!frozen) {
            adapter.reportDataSetChanged()
        }
        updateUI()
    }

    private fun updateUI() {
        if (frozen) {
            freezeButton.setText(R.string.buttonThaw)
        }
        else {
            freezeButton.setText(R.string.buttonFreeze)
        }
    }

    override fun reset(list:List<TextData>) {
        Log.i(name, "reset: message list ...")
        adapter.resetList(list)
        adapter.reportDataSetChanged()
    }

    override fun update(msg: TextData) {
        Log.i(name, String.format("update: message = %s", msg.message))
        if (!frozen) {
            // This must take place on the UI thread
            adapter.insertMessage(msg)
            adapter.reportDataSetChanged()
        }
    }

    val CLSS = "LogsFragment"

    init {
        name = CLSS
        adapter = TextDataAdapter(FixedSizeList<TextData>(BertConstants.NUM_LOG_MESSAGES))
        frozen = false
    }
}
