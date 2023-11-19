/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.ToggleButton
import androidx.lifecycle.Lifecycle
import chuckcoughlin.bertspeak.BertSpeakActivity
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.IntentObserver
import chuckcoughlin.bertspeak.databinding.FragmentCoverBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import chuckcoughlin.bertspeak.service.FacilityState
import chuckcoughlin.bertspeak.service.TieredFacility
import chuckcoughlin.bertspeak.service.VoiceConstants
import chuckcoughlin.bertspeak.ui.VerticalSeekBar
import chuckcoughlin.bertspeak.ui.RendererFactory
import chuckcoughlin.bertspeak.ui.waveform.WaveformView


/**
 * This fragment presents a static "cover" with a waveform view of the voice signal
 * plus a volume bar.
 */
class CoverFragment (pos:Int): BasicAssistantFragment(pos), IntentObserver, OnDataCaptureListener,OnSeekBarChangeListener,ServiceConnection {
    override val name = CLSS
    private var service: DispatchService? = null
    private var visualizer: Visualizer? = null

    // This property is only valid between onCreateView and onDestroyView
    private lateinit var binding: FragmentCoverBinding
    private lateinit var seekBar: VerticalSeekBar
    private lateinit var waveformView: WaveformView

    // Inflate the view. It holds a fixed image of the robot
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        binding = FragmentCoverBinding.inflate(inflater, container, false)
        val bluetoothStatus = binding.bluetoothStatus  // ToggleButton
        val socketStatus = binding.socketStatus
        val voiceStatus = binding.voiceStatus
        bluetoothStatus.setClickable(false) // Not really buttons, just indicators
        socketStatus.isClickable = false
        voiceStatus.isClickable = false
        updateToggleButton(bluetoothStatus, FacilityState.IDLE)
        updateToggleButton(socketStatus, FacilityState.IDLE)
        updateToggleButton(voiceStatus, FacilityState.IDLE)
        val rendererFactory = RendererFactory()
        waveformView = binding.root.findViewById(R.id.waveform_view)
        waveformView.setRenderer(
            rendererFactory.createSimpleWaveformRenderer(Color.GREEN, Color.DKGRAY)
        )
        seekBar = binding.root.findViewById(R.id.verticalSeekbar)
        seekBar.setOnSeekBarChangeListener(this)
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
            visualizer!!.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), true, false)
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
            visualizer!!.setDataCaptureListener(null, 0, false, false)
            visualizer = null
        }
    }

    /**
     * Map current bluetooth action state to ToggleButton icon. Checked in this order ...
     * gray - active = false
     * green- checked = true
     * yellow - checked = false
     * red - enabled = false
     * @param state
     */
    private fun updateToggleButton(btn: ToggleButton, state: FacilityState) {
        Log.i(name, String.format("updateToggleButton:%s %s", btn.text, state.name))
        activity?.runOnUiThread(Runnable {
            btn.visibility = View.INVISIBLE
            when (state) {
                FacilityState.IDLE -> {
                    btn.isChecked = false
                    btn.isSelected = false
                }
                FacilityState.WAITING -> {
                    btn.isChecked = true
                    btn.isSelected = false
                }
                FacilityState.ACTIVE -> {
                    btn.isChecked = true
                    btn.isSelected = true
                }
                FacilityState.ERROR -> {
                    btn.isChecked = false
                    btn.isSelected = true
                }
            }
            btn.visibility = View.VISIBLE
        })
    }

    override fun initialize(list: List<Intent>) {
        for (intent in list) {
            if (intent.hasCategory(VoiceConstants.CATEGORY_FACILITY_STATE)) {
                val actionState = FacilityState.valueOf(
                    intent.getStringExtra(VoiceConstants.KEY_FACILITY_STATE)!!
                )
                val tf =
                    TieredFacility.valueOf(intent.getStringExtra(VoiceConstants.KEY_TIERED_FACILITY)!!)
                when (tf) {
                    TieredFacility.BLUETOOTH -> {
                        updateToggleButton(binding.bluetoothStatus, actionState)
                    }
                    TieredFacility.SOCKET -> {
                        updateToggleButton(binding.socketStatus, actionState)
                    }
                    else -> {
                        updateToggleButton(binding.voiceStatus, actionState)
                    }
                }
            }
        }
    }

    override fun update(intent: Intent) {
        if (intent.hasCategory(VoiceConstants.CATEGORY_FACILITY_STATE)) {
            val actionState =
                FacilityState.valueOf(intent.getStringExtra(VoiceConstants.KEY_FACILITY_STATE)!!)
            val tf =
                TieredFacility.valueOf(intent.getStringExtra(VoiceConstants.KEY_TIERED_FACILITY)!!)
            when (tf) {
                TieredFacility.BLUETOOTH -> {
                    updateToggleButton(binding.bluetoothStatus, actionState)
                }
                TieredFacility.SOCKET -> {
                    updateToggleButton(binding.socketStatus, actionState)
                }
                else -> {
                    updateToggleButton(binding.voiceStatus, actionState)
                }
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
    }
    companion object {
        const val CLSS = "CoverFragment"
        const val CAPTURE_SIZE = 256
    }



}
