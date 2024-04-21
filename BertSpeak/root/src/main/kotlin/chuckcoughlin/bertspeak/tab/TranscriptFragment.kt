/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
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
import chuckcoughlin.bertspeak.databinding.FragmentTranscriptBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.adapter.TextDataAdapter

/**
 * This fragment allows perusal of the robot's spoken interactions. Blue implies
 * text from the robot, pink is text from the user..
 */
class TranscriptFragment (pos:Int): BasicAssistantFragment(pos), TextDataObserver {
    override val name : String
    private var frozen = false
    // These property is only valid between onCreateView and onDestroyView
    val adapter: TextDataAdapter
    private lateinit var freezeButton: Button

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        if (savedInstanceState != null) frozen =
            savedInstanceState.getBoolean(BertConstants.BUNDLE_FROZEN, false)
        val binding = FragmentTranscriptBinding.inflate(inflater,container,false)
        var transcriptView = binding.transcriptRecyclerView
        transcriptView.setHasFixedSize(true) // Refers to the size of the layout.
        val layoutManager = LinearLayoutManager(transcriptView.getContext())
        transcriptView.setLayoutManager(layoutManager)
        transcriptView.setAdapter(adapter)
        val scrollPosition: Int = layoutManager.findFirstCompletelyVisibleItemPosition()
        transcriptView.scrollToPosition(scrollPosition)
        var button = binding.transcriptClearButton
        button.setOnClickListener { clearButtonClicked() }
        freezeButton = binding.transcriptFreezeButton
        freezeButton.setOnClickListener { freezeButtonClicked() }
        updateUI()
        return binding.root
    }

    // Bind to the DispatchService
    override fun onStart() {
        super.onStart()
        DispatchService.registerForTranscripts(this)
    }

    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForTranscripts(this)
    }

    override fun onDestroyView() {
        Log.i(name, "onDestroyView")
        super.onDestroyView()
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

    override fun resetText(list:List<TextData>) {
        Log.i(name, "reset: message list is now ...")
        adapter.resetList(list)
        adapter.reportDataSetChanged()
    }

    @Synchronized
    override fun updateText(msg: TextData) {
        Log.i(name, String.format("update: message = %s", msg.message))
        if (!frozen || frozen) {
            adapter.insertMessage(msg)
            adapter.reportDataSetChanged()
        }
    }

    val CLSS = "TranscriptFragment"

    init {
        name = CLSS
        adapter = TextDataAdapter(FixedSizeList<TextData>(BertConstants.NUM_TRANSCRIPT_MESSAGES))
    }
}
