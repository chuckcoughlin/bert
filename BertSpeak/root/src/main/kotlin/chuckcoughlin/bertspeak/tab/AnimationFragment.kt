/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import chuckcoughlin.bertspeak.common.IntentObserver
import chuckcoughlin.bertspeak.databinding.FragmentAnimationBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import chuckcoughlin.bertspeak.service.VoiceConstants
import chuckcoughlin.bertspeak.ui.animate.AnimationView

/**
 * This fragment displays the robot position right/front/left and allows the uesr to
 * interactively move the limbs via touch gestures.
 */
class AnimationFragment (pos:Int): BasicAssistantFragment(pos), IntentObserver, ServiceConnection {
    override val name = CLSS
    private var service: DispatchService? = null
    private var visualizer: Visualizer? = null

    // This property is only valid between onCreateView and onDestroyView
    private lateinit var binding: FragmentAnimationBinding
    private lateinit var leftPanel: AnimationView
    private lateinit var frontPanel:AnimationView
    private lateinit var rightPanel:AnimationView

    // Inflate the view. It holds three AnimationView panels
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        binding = FragmentAnimationBinding.inflate(inflater, container, false)
        leftPanel = binding.animationViewLeft
        frontPanel = binding.animationViewFront
        rightPanel = binding.animationViewRight
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(name, "onViewCreated: ....")
    }
    /**
     * Bind to the DispatchService, start speech analyzer and enunciator
     */
    override fun onStart() {
        super.onStart()
        val intent = Intent(activity, DispatchService::class.java)
        activity?.applicationContext?.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        if (service != null) {
            Log.i(name, "onResume: registering as observer")
            service!!.statusManager.register(this)
        }
        startVisualizer()
    }

    override fun onPause() {
        super.onPause()
        if (service != null) {
            Log.i(name, "onPause: unregistering as observer")
            service!!.statusManager.unregister(this)
        }
        stopVisualizer()
    }

    override fun onStop() {
        super.onStop()
        activity?.applicationContext?.unbindService(this)
    }

    override fun onDestroyView() {
        Log.i(name, "onDestroyView: ...")
        super.onDestroyView()
    }

    private fun startVisualizer() {
        try {
            visualizer = Visualizer(0)
            //visualizer!!.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), true, false)
            visualizer!!.captureSize = CAPTURE_SIZE
            visualizer!!.enabled = true
        }
        catch (ex: Exception) {  // This will fail in the emulator
            Log.i(name,String.format("startVisualizer: FAILED to start (%s).",name,ex.localizedMessage))
        }
    }

    private fun stopVisualizer() {
        if (visualizer != null) {
            visualizer!!.enabled = false
            visualizer!!.release()
            //visualizer!!.setDataCaptureListener(null, 0, false, false)
            visualizer = null
        }
    }


    override fun initialize(list: List<Intent>) {
        for (intent in list) {
            if (intent.hasCategory(VoiceConstants.CATEGORY_CONTROLLER_STATE)) {
                /*
                val actionState = ControllerState.valueOf(
                    intent.getStringExtra(VoiceConstants.KEY_CONTROLLER_STATE)!!
                )
                val tf =
                    ControllerType.valueOf(intent.getStringExtra(VoiceConstants.KEY_CONTROLLER)!!)
                 */
            }
        }
    }

    override fun update(intent: Intent) {
        if (intent.hasCategory(VoiceConstants.CATEGORY_CONTROLLER_STATE)) {
            /*
            val actionState =
                ControllerState.valueOf(intent.getStringExtra(VoiceConstants.KEY_CONTROLLER_STATE)!!)
            val tf =
                ControllerType.valueOf(intent.getStringExtra(VoiceConstants.KEY_CONTROLLER)!!)
             */

        }
    }

    // =================================== ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if (service != null) service!!.statusManager.unregister(this)
        service = null
    }

    // name.getClassName() contains the class of the service.
    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder = bndr as DispatchServiceBinder
        service = binder.getService()
        service!!.statusManager.register(this)
    }
    companion object {
        const val CLSS = "CoverFragment"
        const val CAPTURE_SIZE = 256
    }



}
