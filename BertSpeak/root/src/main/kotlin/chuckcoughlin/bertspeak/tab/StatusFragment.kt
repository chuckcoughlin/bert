/**
 * Copyright 2019-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.databinding.FragmentStatusBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.data.TextDataObserver
import chuckcoughlin.bertspeak.ui.adapter.TextDataAdapter

/**
 * This fragment displays servo data from the robot in tabular form. Only
 * one table is displayed at a time and is completely replaced when the
 * next set of data are read. The table is dynamically sized to fit the list.
 */
class StatusFragment(pos:Int) : BasicAssistantFragment(pos), ServiceConnection, TextDataObserver {
    override val name : String
    private val layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: TextDataAdapter? = null
    private var rootView: View? = null
    private var logMessageView: RecyclerView? = null
    private val logView: TextView? = null


    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        super.onCreate(savedInstanceState)
        val binding = FragmentStatusBinding.inflate(inflater,container,false)
        binding.fragmentTablesText.setText(R.string.fragmentStatusLabel)
        logMessageView = binding.root.findViewById(R.id.logs_recycler_view)
        logMessageView!!.setHasFixedSize(true) // Refers to the size of the layout.
        val layoutManager = LinearLayoutManager(logMessageView!!.context)
        logMessageView!!.layoutManager = layoutManager
        adapter = TextDataAdapter(FixedSizeList<TextData>(BertConstants.NUM_LOG_MESSAGES))
        logMessageView!!.adapter = adapter
        val scrollPosition: Int = layoutManager.findFirstCompletelyVisibleItemPosition()
        logMessageView!!.scrollToPosition(scrollPosition)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        DispatchService.registerForLogs

    }

    override fun onResume() {
        super.onResume()
        Log.i(name, "onResume: registering adapter as observer")
        if (service != null) service?.getTextManager()?.registerTableViewer(this)
    }

    override fun onPause() {
        super.onPause()
        Log.i(name, "onPause: unregistering adapter as observer")
        if (service != null) {
            service?.getTextManager()?.unregisterTableViewer(this)
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

    // =================================== ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if (service != null) {
            service?.getTextManager()?.unregisterTableViewer(this)
            service = null
        }
    }

    // name.getClassName() contains the class of the service.
    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder: DispatchServiceBinder = bndr as DispatchServiceBinder
        service = binder.getService()
        service?.getTextManager()?.registerTableViewer(this)
    }

    override fun initialize() {}
    override fun update(msg: TextData) {
        //val text: String = msg.message
    }

    companion object {
        val CLSS = "StatusFragment"
    }
    init {
        this.name = CLSS
    }
}
