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
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import chuckcoughlin.bertspeak.databinding.FragmentPosesBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.data.TextDataObserver
import chuckcoughlin.bertspeak.ui.adapter.TextDataAdapter

/**
 * This fragment allows the user to select a pose stored in the robot, then
 * displays all the position settings in that pose.
 */
class PosesFragment(pos:Int) : BasicAssistantFragment(pos), ServiceConnection, TextDataObserver {
    override val name : String
    private val layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: TextDataAdapter? = null
    private var rootView: View? = null
    private var logMessageView: RecyclerView? = null
    private val logView: TextView? = null
    private var service: DispatchService? = null
    // This property is only valid between onCreateView and onDestroyView
    private lateinit var binding: FragmentPosesBinding

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        super.onCreate(savedInstanceState)
        binding = FragmentPosesBinding.inflate(inflater,container,false)

        return binding.root
    }

    // Bind to the DispatchService
    override fun onStart() {
        super.onStart()
        if (context != null) {
            val intent = Intent(context?.applicationContext, DispatchService::class.java)
            requireContext().applicationContext.bindService(intent, this, Context.BIND_AUTO_CREATE)
        }
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

    val CLSS = "PosesFragment"

    init {
        this.name = CLSS
    }
}
