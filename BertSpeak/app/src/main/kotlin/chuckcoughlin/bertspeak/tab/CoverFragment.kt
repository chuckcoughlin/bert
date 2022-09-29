/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 * For visualizer code see: https://github.com/StylingAndroid/
 */
package chuckcoughlin.bertspeak.tab

import chuckcoughlin.bertspeak.common.IntentObserver
import android.media.audiofx.Visualizer.OnDataCaptureListener
import chuckcoughlin.bertspeak.service.DispatchService
import android.media.audiofx.Visualizer
import chuckcoughlin.bertspeak.bert.waveform.WaveformView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.service.FacilityState
import chuckcoughlin.bertspeak.service.VoiceConstants
import chuckcoughlin.bertspeak.service.TieredFacility
import android.os.IBinder
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import android.content.*
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.*
import chuckcoughlin.bertspeak.bert.waveform.RendererFactory
import java.lang.Exception

/**
 * This fragment presents a static "cover" with no dynamic content.
 */
class CoverFragment : BasicAssistantFragment(), IntentObserver, OnDataCaptureListener,
    ServiceConnection {
    private var bluetoothStatus: ToggleButton? = null
    private var socketStatus: ToggleButton? = null
    private var voiceStatus: ToggleButton? = null
    private var service: DispatchService? = null
    private var visualizer: Visualizer? = null
    private var waveformView: WaveformView? = null

    // Inflate the view. It holds a fixed image of the robot
    fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(Companion.name, "onCreateView: ....")
        val view = inflater.inflate(R.layout.fragment_cover, container, false)
        val label = view.findViewById<TextView>(R.id.fragmentCoverText)
        label.setText(getString(R.string.fragmentCoverLabel))
        label.textSize = 36f
        val imageView = view.findViewById<ImageView>(R.id.fragmentCoverImage)
        imageView.setImageResource(R.drawable.recliner)
        bluetoothStatus = view.findViewById(R.id.bluetooth_status)
        socketStatus = view.findViewById(R.id.socket_status)
        voiceStatus = view.findViewById(R.id.voice_status)
        bluetoothStatus.setClickable(false) // Not really buttons, just indicators
        socketStatus.setClickable(false)
        voiceStatus.setClickable(false)
        updateToggleButton(bluetoothStatus, FacilityState.IDLE)
        updateToggleButton(socketStatus, FacilityState.IDLE)
        updateToggleButton(voiceStatus, FacilityState.IDLE)
        waveformView = view.findViewById(R.id.waveform_view)
        val rendererFactory = RendererFactory()
        waveformView.setRenderer(
            rendererFactory.createSimpleWaveformRenderer(
                Color.GREEN,
                Color.DKGRAY
            )
        )
        return view
    }

    /**
     * Bind to the DispatchService, start speech analyzer and enunciator
     */
    fun onStart() {
        super.onStart()
        val intent = Intent(getActivity(), DispatchService::class.java)
        getActivity().getApplicationContext().bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun onResume() {
        super.onResume()
        if (service != null) {
            Log.i(Companion.name, "onResume: registering as observer")
            service!!.statusManager!!.register(this)
        }
        startVisualizer()
    }

    fun onPause() {
        super.onPause()
        if (service != null) {
            Log.i(Companion.name, "onPause: unregistering as observer")
            service!!.statusManager!!.unregister(this)
        }
        stopVisualizer()
    }

    fun onStop() {
        super.onStop()
        getActivity().getApplicationContext().unbindService(this)
    }

    fun onDestroyView() {
        Log.i(Companion.name, "onDestroyView: ...")
        super.onDestroyView()
    }

    private fun startVisualizer() {
        try {
            visualizer = Visualizer(0)
            visualizer!!.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), true, false)
            visualizer!!.captureSize = CAPTURE_SIZE
            visualizer!!.enabled = true
        } catch (ex: Exception) {  // This will fail in the emulator
            Log.i(
                Companion.name,
                String.format(
                    "startVisualizer: FAILED to start (%s).",
                    Companion.name,
                    ex.localizedMessage
                )
            )
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
    private fun updateToggleButton(btn: ToggleButton?, state: FacilityState) {
        Log.i(Companion.name, String.format("updateToggleButton:%s %s", btn!!.text, state.name))
        getActivity().runOnUiThread(Runnable {
            btn.visibility = View.INVISIBLE
            if (state == FacilityState.IDLE) {
                btn.isChecked = false
                btn.isSelected = false
            } else if (state == FacilityState.WAITING) {
                btn.isChecked = true
                btn.isSelected = false
            } else if (state == FacilityState.ACTIVE) {
                btn.isChecked = true
                btn.isSelected = true
            } else if (state == FacilityState.ERROR) {
                btn.isChecked = false
                btn.isSelected = true
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
                if (tf == TieredFacility.BLUETOOTH) {
                    updateToggleButton(bluetoothStatus, actionState)
                } else if (tf == TieredFacility.SOCKET) {
                    updateToggleButton(socketStatus, actionState)
                } else {
                    updateToggleButton(voiceStatus, actionState)
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
            if (tf == TieredFacility.BLUETOOTH) {
                updateToggleButton(bluetoothStatus, actionState)
            } else if (tf == TieredFacility.SOCKET) {
                updateToggleButton(socketStatus, actionState)
            } else {
                updateToggleButton(voiceStatus, actionState)
            }
        }
    }

    // =================================== OnDataCaptureListener ===============================
    override fun onWaveFormDataCapture(
        thisVisualiser: Visualizer,
        waveform: ByteArray,
        samplingRate: Int
    ) {
        if (waveformView != null) {
            waveformView!!.setWaveform(waveform)
        }
    }

    override fun onFftDataCapture(thisVisualiser: Visualizer, fft: ByteArray, samplingRate: Int) {
        // NO-OP
    }

    // =================================== ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if (service != null) service!!.statusManager!!.unregister(this)
        service = null
    }

    // name.getClassName() contains the class of the service.
    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder = bndr as DispatchServiceBinder
        service = binder.service
        service!!.statusManager!!.register(this)
    }

    companion object {
        // ===================== IntentObserver =====================
        val name = "CoverFragment"
            get() = Companion.field
        private const val CAPTURE_SIZE = 256
    }
}