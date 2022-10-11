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
import android.widget.TextView
import androidx.annotation.NonNull
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
import java.security.AccessController.getContext

/**
 * This fragment displays data from the robot in tabular form. Only
 * one table is displayed at a time and is completely replaced when the
 * next table is read. The table is dynamically sized to fit the data.
 */
class TablesTabFragment : BasicAssistantFragment(), ServiceConnection, TextMessageObserver {
    private val layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: TextMessageAdapter? = null
    private var rootView: View? = null
    private var logMessageView: RecyclerView? = null
    private val logView: TextView? = null
    private var service: DispatchService? = null
    fun onCreateView(
        @NonNull inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        rootView = inflater.inflate(R.layout.fragment_tables_tab, container, false)
        val textView: TextView = rootView.findViewById<TextView>(R.id.fragmentTablesText)
        textView.setText(R.string.fragmentTableTabLabel)
        logMessageView = rootView!!.findViewById(R.id.logs_recycler_view)
        logMessageView.setHasFixedSize(true) // Refers to the size of the layout.
        val layoutManager = LinearLayoutManager(logMessageView.getContext())
        logMessageView.setLayoutManager(layoutManager)
        adapter = TextMessageAdapter(FixedSizeList<TextMessage>(BertConstants.NUM_LOG_MESSAGES))
        logMessageView.setAdapter(adapter)
        val scrollPosition: Int = layoutManager.findFirstCompletelyVisibleItemPosition()
        logMessageView.scrollToPosition(scrollPosition)
        return rootView
    }

    // Bind to the DispatchService
    override fun onStart() {
        super.onStart()
        if (getContext() != null) {
            val intent = Intent(getContext().getApplicationContext(), DispatchService::class.java)
            getContext().getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(name, "onResume: registering adapter as observer")
        if (service != null) service.getTextManager().registerTableViewer(this)
    }

    override fun onPause() {
        super.onPause()
        Log.i(name, "onPause: unregistering adapter as observer")
        if (service != null) {
            service.getTextManager().unregisterTableViewer(this)
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

    // =================================== ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if (service != null) service.getTextManager().unregisterTableViewer(this)
        service = null
    }

    // name.getClassName() contains the class of the service.
    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder: DispatchServiceBinder = bndr as DispatchServiceBinder
        service = binder.getService()
        service.getTextManager().registerTableViewer(this)
    }

    override fun initialize() {}
    override fun update(msg: TextMessage) {
        val text: String = msg.getMessage()
    }

    companion object {
        // =================================== TextMessageObserver ===============================
        val name = "TablesTabFragment"
            get() = Companion.field
    }
}
