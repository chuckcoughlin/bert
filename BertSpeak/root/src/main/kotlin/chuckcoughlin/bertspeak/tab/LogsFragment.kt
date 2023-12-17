/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
*/
package chuckcoughlin.bertspeak.tab

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.databinding.FragmentLogsBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import chuckcoughlin.bertspeak.speech.TextMessage
import chuckcoughlin.bertspeak.speech.TextMessageObserver
import chuckcoughlin.bertspeak.ui.list.LogMessageAdapter

/**
 * This fragment shows log messages originating in the robot.
 */
class LogsFragment(pos:Int) : BasicAssistantFragment(pos), ServiceConnection, TextMessageObserver {
    override val name:String
    private var service: DispatchService? = null
    private var frozen = false
    // These properties only valid between onCreateView and onDestroyView
    private lateinit var adapter: LogMessageAdapter
    private lateinit var binding: FragmentLogsBinding

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        adapter = LogMessageAdapter(FixedSizeList<TextMessage>(BertConstants.NUM_LOG_MESSAGES))
        Log.i(CLSS, String.format("onCreateView: will display %d messages", adapter.itemCount))
        if (savedInstanceState != null) frozen =
            savedInstanceState.getBoolean(BertConstants.BUNDLE_FROZEN, false)
        binding = FragmentLogsBinding.inflate(inflater,container,false)
        var logMessageView = binding.logsRecyclerView   // RecyclerView
        logMessageView.setHasFixedSize(true) // Refers to the size of the layout.
        val layoutManager = LinearLayoutManager(logMessageView.getContext())
        logMessageView.setLayoutManager(layoutManager)
        logMessageView.setAdapter(adapter)
        val scrollPosition: Int = layoutManager.findFirstCompletelyVisibleItemPosition()
        logMessageView.scrollToPosition(scrollPosition)
        var button = binding.logClearButton
        button.setOnClickListener { clearButtonClicked() }
        button = binding.logFreezeButton
        button.setOnClickListener { freezeButtonClicked() }
        updateUI()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(name, "onViewCreated: ....")
    }
    // Bind to the DispatchService
    override fun onStart() {
        super.onStart()
        val intent = Intent(requireContext().applicationContext, DispatchService::class.java)
        requireContext().applicationContext.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        if (service != null) {
            Log.i(name, "onResume: registering as observer")
            service?.getTextManager()?.registerLogViewer(this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (service != null) {
            Log.i(name, "onPause: unregistering as observer")
            service?.getTextManager()?.unregisterLogViewer(this)
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().applicationContext.unbindService(this)
    }

    override fun onDestroyView() {
        Log.i(name, "onDestroyView")
        super.onDestroyView()
    }

    override fun onSaveInstanceState(stateToSave: Bundle) {
        super.onSaveInstanceState(stateToSave)
        stateToSave.putBoolean(BertConstants.BUNDLE_FROZEN, frozen)
    }

    //======================================== Button Callbacks ======================================
    //
    fun clearButtonClicked() {
        Log.i(name, "Clear button clicked")
        if (service != null) {
            service?.getTextManager().getLogs().clear()
            adapter.reportDataSetChanged()
        }
    }

    fun freezeButtonClicked() {
        frozen = !frozen
        if (service != null) {
            if (!frozen) {
                adapter.reportDataSetChanged()
            }
        }
        updateUI()
    }

    private fun updateUI() {
        val button = binding.logFreezeButton
        if (frozen) {
            button.setText(R.string.buttonThaw)
        }
        else {
            button.setText(R.string.buttonFreeze)
        }
    }

    // =================================== ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if (service != null) {
            service?.getTextManager()?.unregisterLogViewer(this)
            service = null
        }
    }

    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder: DispatchServiceBinder = bndr as DispatchServiceBinder
        service = binder.getService()
        adapter.resetList(service?.getTextManager()?.getLogs()?.toList())
        service?.getTextManager()?.registerLogViewer(this)
    }

    override fun initialize() {
        Log.i(name, "initialize: message list ...")
        for (m in service?.getTextManager()?.getLogs()!!) {
            Log.i(name, String.format("initialize: \t%s", m.message))
        }
        adapter.reportDataSetChanged()
    }

    override fun update(msg: TextMessage) {
        Log.i(name, String.format("update: message = %s", msg.message))
        if (!frozen) {
            // This must take place on the UI thread
            requireActivity().runOnUiThread {
                adapter.reportItemInserted()
            }
        }
    }

    val CLSS = "LogsFragment"
    init {
        name = CLSS
    }
}
