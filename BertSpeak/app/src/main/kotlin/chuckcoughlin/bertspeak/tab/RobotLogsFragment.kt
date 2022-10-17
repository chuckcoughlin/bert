/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
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
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.databinding.FragmentCoverBinding
import chuckcoughlin.bertspeak.databinding.FragmentRobotLogsBinding
import chuckcoughlin.bertspeak.logs.TextMessageAdapter
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import chuckcoughlin.bertspeak.speech.TextMessage
import chuckcoughlin.bertspeak.speech.TextMessageObserver
import java.security.AccessController.getContext

/**
 * This fragment shows log messages originating in the robot.
 */
class RobotLogsFragment(pageNumber:Int) : BasicAssistantFragment(pageNumber), ServiceConnection, TextMessageObserver {
    override val name = CLSS
    private var adapter: TextMessageAdapter? = null
    private var service: DispatchService? = null
    private var frozen = false
    // This property is only valid between onCreateView and onDestroyView
    private lateinit var binding: FragmentRobotLogsBinding

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        if (savedInstanceState != null) frozen =
            savedInstanceState.getBoolean(BertConstants.BUNDLE_FROZEN, false)
        binding = FragmentRobotLogsBinding.inflate(inflater,container,false)
        adapter = TextMessageAdapter(FixedSizeList<TextMessage>(BertConstants.NUM_LOG_MESSAGES))
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

    // Bind to the DispatchService
    override fun onStart() {
        super.onStart()
        val intent = Intent(requireContext().getApplicationContext(), DispatchService::class.java)
        requireContext().getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE)
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
        if (getContext() != null) requireContext().getApplicationContext().unbindService(this)
    }

    override fun onDestroyView() {
        Log.i(name, "onDestroyView")
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(BertConstants.BUNDLE_FROZEN, frozen)
    }

    //======================================== Button Callbacks ======================================
    //
    fun clearButtonClicked() {
        Log.i(name, "Clear button clicked")
        if (service != null) {
            service?.getTextManager()?.getLogs()?.clear()
            adapter?.notifyDataSetChanged()
        }
    }

    fun freezeButtonClicked() {
        frozen = !frozen
        if (service != null) {
            if (!frozen) {
                adapter?.notifyDataSetChanged()
            }
        }
        updateUI()
    }

    private fun updateUI() {
        val button = binding.logFreezeButton
        if (frozen) {
            button.setText(R.string.buttonThaw)
        } else {
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
        adapter?.resetList(service?.getTextManager()?.getLogs()?.toList())
        service?.getTextManager()?.registerLogViewer(this)
    }

    override fun initialize() {
        Log.i(name, "initialize: message list is now ...")
        for (m in service?.getTextManager()?.getLogs()!!) {
            Log.i(name, String.format("initialize: \t%s", m.message))
        }
        adapter?.notifyDataSetChanged()
    }

    override fun update(msg: TextMessage) {
        Log.i(name, String.format("update: message = %s", msg.message))
        if (!frozen) {
            adapter?.notifyItemInserted(0)
            binding.logsRecyclerView.scrollToPosition(0)
        }
    }

    companion object {
        val CLSS = "RobotLogsFragment"
    }
}
