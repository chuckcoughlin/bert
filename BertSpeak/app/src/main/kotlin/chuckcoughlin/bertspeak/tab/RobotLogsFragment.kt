/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
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
import chuckcoughlin.bertspeak.logs.TextMessageAdapter
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import chuckcoughlin.bertspeak.speech.TextMessage
import chuckcoughlin.bertspeak.speech.TextMessageObserver

/**
 * This fragment shows log messages originating in the robot.
 */
class RobotLogsFragment : BasicAssistantFragment(), ServiceConnection, TextMessageObserver {
    private var adapter: TextMessageAdapter? = null
    private var rootView: View? = null
    private var logMessageView: RecyclerView? = null
    private var service: DispatchService? = null
    private var frozen = false
    fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) frozen =
            savedInstanceState.getBoolean(BertConstants.BUNDLE_FROZEN, false)
        adapter = TextMessageAdapter(FixedSizeList<TextMessage>(BertConstants.NUM_LOG_MESSAGES))
        rootView = inflater.inflate(R.layout.fragment_robot_logs, container, false)
        logMessageView = rootView!!.findViewById(R.id.logs_recycler_view)
        logMessageView.setHasFixedSize(true) // Refers to the size of the layout.
        val layoutManager = LinearLayoutManager(logMessageView.getContext())
        logMessageView.setLayoutManager(layoutManager)
        logMessageView.setAdapter(adapter)
        val scrollPosition: Int = layoutManager.findFirstCompletelyVisibleItemPosition()
        logMessageView.scrollToPosition(scrollPosition)
        var button = rootView!!.findViewById<Button>(R.id.logClearButton)
        button.setOnClickListener { clearButtonClicked() }
        button = rootView!!.findViewById(R.id.logFreezeButton)
        button.setOnClickListener { freezeButtonClicked() }
        updateUI()
        return rootView
    }

    // Bind to the DispatchService
    override fun onStart() {
        super.onStart()
        val intent = Intent(getContext().getApplicationContext(), DispatchService::class.java)
        getContext().getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        if (service != null) {
            Log.i(name, "onResume: registering as observer")
            service.getTextManager().registerLogViewer(this)
        }
    }

    override fun onPause() {
        super.onPause()
        if (service != null) {
            Log.i(name, "onPause: unregistering as observer")
            service.getTextManager().unregisterLogViewer(this)
        }
    }

    override fun onStop() {
        super.onStop()
        if (getContext() != null) getContext().getApplicationContext().unbindService(this)
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
            service.getTextManager().getLogs().clear()
            adapter.notifyDataSetChanged()
        }
    }

    fun freezeButtonClicked() {
        frozen = !frozen
        if (service != null) {
            if (!frozen) {
                adapter.notifyDataSetChanged()
            }
        }
        updateUI()
    }

    private fun updateUI() {
        val button = rootView!!.findViewById<Button>(R.id.logFreezeButton)
        if (frozen) {
            button.setText(R.string.buttonThaw)
        } else {
            button.setText(R.string.buttonFreeze)
        }
    }

    // =================================== ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if (service != null) service.getTextManager().unregisterLogViewer(this)
        service = null
    }

    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder: DispatchServiceBinder = bndr as DispatchServiceBinder
        service = binder.getService()
        adapter.resetList(service.getTextManager().getLogs())
        service.getTextManager().registerLogViewer(this)
    }

    override fun initialize() {
        Log.i(name, "initialize: message list is now ...")
        for (m in service.getTextManager().getLogs()) {
            Log.i(name, String.format("initialize: \t%s", m.getMessage()))
        }
        adapter.notifyDataSetChanged()
    }

    override fun update(msg: TextMessage) {
        Log.i(name, String.format("update: message = %s", msg.getMessage()))
        if (!frozen) {
            adapter.notifyItemInserted(0)
            logMessageView.scrollToPosition(0)
        }
    }

    companion object {
        // =================================== TextMessageObserver ===============================
        val name = "RobotLogsFragment"
            get() = Companion.field
    }
}
