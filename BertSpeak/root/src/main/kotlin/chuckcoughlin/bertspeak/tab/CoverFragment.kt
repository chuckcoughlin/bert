/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.lifecycle.Lifecycle
import chuckcoughlin.bertspeak.BertSpeakActivity
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.IntentObserver
import chuckcoughlin.bertspeak.databinding.FragmentCoverBinding
import chuckcoughlin.bertspeak.service.ControllerState
import chuckcoughlin.bertspeak.service.ControllerType
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import chuckcoughlin.bertspeak.service.PermissionManager
import chuckcoughlin.bertspeak.service.VoiceConstants
import chuckcoughlin.bertspeak.ui.RendererFactory
import chuckcoughlin.bertspeak.ui.StatusImageButton
import chuckcoughlin.bertspeak.ui.VerticalSeekBar
import chuckcoughlin.bertspeak.ui.waveform.WaveformView


/**
 * This fragment presents a static "cover" with a waveform view of the voice signal
 * plus a volume bar.
 */
class CoverFragment (pos:Int): BasicAssistantFragment(pos), IntentObserver, OnClickListener,OnDataCaptureListener,OnSeekBarChangeListener,ServiceConnection {
    override val name = CLSS
    private var service: DispatchService? = null
    private var visualizer: Visualizer? = null

    // This property is only valid between onCreateView and onDestroyView
    private lateinit var binding: FragmentCoverBinding
    private lateinit var seekBar: VerticalSeekBar
    private lateinit var waveformView: WaveformView

    private var bluetoothButtonId: Int
    private var socketButtonId: Int
    private var voiceButtonId: Int

    // Inflate the view. It holds a fixed image of the robot
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        binding = FragmentCoverBinding.inflate(inflater, container, false)
        val bluetoothStatus = binding.bluetoothStatus  // ToggleButton
        val socketStatus = binding.socketStatus
        val voiceStatus = binding.voiceStatus
        bluetoothStatus.isClickable = true // Not really buttons, just indicators
        bluetoothStatus.setOnClickListener(this)
        bluetoothButtonId = bluetoothStatus.id
        socketStatus.isClickable = true
        socketButtonId = socketStatus.id
        voiceStatus.isClickable = true
        voiceButtonId = voiceStatus.id
        updateStatusButton(bluetoothStatus, ControllerState.OFF)
        updateStatusButton(socketStatus, ControllerState.OFF)
        updateStatusButton(voiceStatus, ControllerState.OFF)
        val rendererFactory = RendererFactory()
        waveformView = binding.root.findViewById(R.id.waveform_view)
        waveformView.setRenderer(
            rendererFactory.createSimpleWaveformRenderer(Color.GREEN, Color.DKGRAY)
        )
        seekBar = binding.root.findViewById(R.id.verticalSeekbar)
        seekBar.setOnSeekBarChangeListener(this)
        val pm = PermissionManager(requireActivity())
        pm.askForPermissions()
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
        Log.i(CLSS, String.format("onStart: main view is %d x %d ...",binding.root.height,binding.root.width ))
        val intent = Intent(activity, DispatchService::class.java)
        requireActivity().applicationContext?.bindService(intent, this, Context.BIND_AUTO_CREATE)
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
        requireActivity().applicationContext?.unbindService(this)
    }

    override fun onDestroyView() {
        Log.i(name, "onDestroyView: ...")
        super.onDestroyView()
    }

    private fun startVisualizer() {
        try {
            visualizer = Visualizer(0)
            visualizer!!.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), true, false)
            visualizer!!.captureSize = CAPTURE_SIZE
            visualizer!!.enabled = true
        }
        catch (ex: Exception) {  // This will fail in the emulator
            Log.i(name,String.format("startVisualizer: %s FAILED to start (%s).",name,ex.localizedMessage))
        }
    }

    private fun stopVisualizer() {
        if (visualizer != null) {
            visualizer!!.enabled = false
            visualizer!!.release()
            visualizer!!.setDataCaptureListener(null, 0, false, false)
            visualizer = null
        }
    }

    /**
     * Map current action state to StatusImageButton icon. Checked in this order ...
     * gray - active = false
     * green- checked = true
     * yellow - checked = false
     * red - enabled = false
     * @param state
     */
    private fun updateStatusButton(btn: StatusImageButton, state: ControllerState) {
        Log.i(name, String.format("updateStatusButton:%s", state.name))
        requireActivity().runOnUiThread(Runnable {
            btn.visibility = View.INVISIBLE
            btn.setButtonState(state)
            btn.visibility = View.VISIBLE
        })
    }

    override fun initialize(list: List<Intent>) {
        for (intent in list) {
            if (intent.hasCategory(VoiceConstants.CATEGORY_CONTROLLER_STATE)) {
                val actionState = ControllerState.valueOf(
                    intent.getStringExtra(VoiceConstants.KEY_CONTROLLER_STATE)!!
                )
                val type =
                    ControllerType.valueOf(intent.getStringExtra(VoiceConstants.KEY_CONTROLLER)!!)
                when (type) {
                    ControllerType.BLUETOOTH -> {
                        updateStatusButton(binding.bluetoothStatus, actionState)
                    }
                    ControllerType.SOCKET -> {
                        updateStatusButton(binding.socketStatus, actionState)
                    }
                    else -> {
                        updateStatusButton(binding.voiceStatus, actionState)
                    }
                }
            }
        }
    }

    /**
     * On start of the fragment, we've asked the user for any permissions that are not already set
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        Log.i(CLSS, String.format("onRequestPermissionsResult:%d",requestCode))
        if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted
        }
        else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
        }
    }
    override fun update(intent: Intent) {
        if (intent.hasCategory(VoiceConstants.CATEGORY_CONTROLLER_STATE)) {
            val actionState =
                ControllerState.valueOf(intent.getStringExtra(VoiceConstants.KEY_CONTROLLER_STATE)!!)
            val tf =
                ControllerType.valueOf(intent.getStringExtra(VoiceConstants.KEY_CONTROLLER)!!)
            when (tf) {
                ControllerType.BLUETOOTH -> {
                    updateStatusButton(binding.bluetoothStatus, actionState)
                }
                ControllerType.SOCKET -> {
                    updateStatusButton(binding.socketStatus, actionState)
                }
                else -> {
                    updateStatusButton(binding.voiceStatus, actionState)
                }
            }
        }
    }

    // ================== OnClickListener ===============
    // One of the status buttons has been clicked
    override fun onClick(v: View) {
        when(v.id) {
            bluetoothButtonId -> {
                Log.i(name, String.format("onClick:%s",ControllerType.BLUETOOTH.name))
            }
            socketButtonId -> {
                Log.i(name, String.format("onClick:%s",ControllerType.SOCKET.name))
            }
            voiceButtonId -> {
                Log.i(name, String.format("onClick:%s",ControllerType.VOICE.name))
            }

        }

    }

    // ================== OnDataCaptureListener ===============
    // This is valid only between view-create and destroy
    override fun onWaveFormDataCapture(thisVisualiser: Visualizer,waveform: ByteArray,samplingRate: Int) {
       if( lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            waveformView.setWaveform(waveform)
        }
    }

    override fun onFftDataCapture(thisVisualiser: Visualizer, fft: ByteArray, samplingRate: Int) {
        // NO-OP
    }

    // =================================OnSeekBarChangeListener =========================
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}


    override fun onStartTrackingTouch(seekBar: SeekBar?) {}


    override fun onProgressChanged(seekBar: SeekBar?, progress: Int,fromUser: Boolean) {
        //sliderText.setText("" + progress)
    }
    // ================================ ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if (service != null) service!!.statusManager.unregister(this)
        service = null
    }

    // name.getClassName() contains the class of the service.
    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder = bndr as DispatchServiceBinder
        service = binder.getService()
        service!!.statusManager.register(this)
        Log.i(CLSS,String.format("onServiceConnected: ...."))
    }
    companion object {
        const val CLSS = "CoverFragment"
        const val CAPTURE_SIZE = 256
    }

    init {
        bluetoothButtonId= 0
        socketButtonId= 0
        voiceButtonId= 0
    }
}
