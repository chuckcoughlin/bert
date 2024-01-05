/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.graphics.Color
import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.lifecycle.Lifecycle
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.data.StatusData
import chuckcoughlin.bertspeak.data.StatusDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentCoverBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.service.ManagerType
import chuckcoughlin.bertspeak.service.PermissionManager
import chuckcoughlin.bertspeak.ui.RendererFactory
import chuckcoughlin.bertspeak.ui.StatusImageButton
import chuckcoughlin.bertspeak.ui.VerticalSeekBar
import chuckcoughlin.bertspeak.ui.waveform.WaveformView


/**
 * This fragment presents a static "cover" with a waveform view of the voice signal
 * plus a volume bar.
 */
class CoverFragment (pos:Int): BasicAssistantFragment(pos), StatusDataObserver, OnClickListener,OnDataCaptureListener,OnSeekBarChangeListener {

    override val name : String
    private var visualizer: Visualizer?

    // This property is only valid between onCreateView and onDestroyView
    private lateinit var seekBar: VerticalSeekBar
    private lateinit var waveformView: WaveformView

    private lateinit var bluetoothStatusButton: StatusImageButton
    private lateinit var socketStatusButton: StatusImageButton
    private lateinit var voiceStatusButton: StatusImageButton

    // Inflate the view. It holds a fixed image of the robot
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        val binding = FragmentCoverBinding.inflate(inflater, container, false)
        bluetoothStatusButton = binding.bluetoothStatus  // ToggleButton
        socketStatusButton    = binding.socketStatus
        voiceStatusButton     = binding.voiceStatus
        bluetoothStatusButton.isClickable = true // Not really buttons, just indicators
        bluetoothStatusButton.setOnClickListener(this)
        socketStatusButton.isClickable = true
        voiceStatusButton.isClickable = true
        updateStatusButton(bluetoothStatusButton, ManagerState.OFF)
        updateStatusButton(socketStatusButton, ManagerState.OFF)
        updateStatusButton(voiceStatusButton, ManagerState.OFF)
        val rendererFactory = RendererFactory()
        waveformView = binding.root.findViewById(R.id.waveformView)
        waveformView.setRenderer(
            rendererFactory.createSimpleWaveformRenderer(Color.GREEN, Color.DKGRAY)
        )
        seekBar = binding.verticalSeekbar
        seekBar.setOnSeekBarChangeListener(this)
        val pm = PermissionManager(requireActivity())
        pm.askForPermissions()
        return binding.root
    }
    /**
     * Start visualizer and register for status updates
     */
    override fun onStart() {
        super.onStart()
        Log.i(name, "onStart: registering as observer")
        DispatchService.registerForStatus(this)
        startVisualizer()
    }

    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForStatus(this)
        stopVisualizer()
    }

    @Synchronized
    private fun startVisualizer() {
        if (visualizer != null) {
            try {
                visualizer!!.setDataCaptureListener(
                    this,
                    Visualizer.getMaxCaptureRate(),
                    true,
                    false
                )
                visualizer!!.captureSize = CAPTURE_SIZE
                visualizer!!.enabled = true
            } catch (ex: Exception) {  // This will fail in the emulator
                Log.i(
                    name, String.format(
                        "startVisualizer: %s FAILED to start (%s).",
                        name, ex.localizedMessage
                    )
                )
            }
        }
    }
    @Synchronized
    private fun stopVisualizer() {
        if( visualizer!=null ) {
            visualizer!!.enabled = false
            visualizer!!.release()
            visualizer!!.setDataCaptureListener(null, 0, false, false)
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
    private fun updateStatusButton(btn: StatusImageButton, state: ManagerState) {
        Log.i(name, String.format("updateStatusButton:%s", state.name))
        requireActivity().runOnUiThread(Runnable {
            btn.visibility = View.INVISIBLE
            btn.setButtonState(state)
            btn.visibility = View.VISIBLE
        })
    }

    /**
     * This is called when we first establish the observer.
     */
    override fun reset(list: List<StatusData>) {
        for (ddata in list) {
            update(ddata)
        }
    }

    /**
     * We have received an update from one of the internal managers. Use the
     * category to determine which.
     */
    override fun update(data: StatusData) {
        if (data.action.equals(DispatchConstants.ACTION_MANAGER_STATE)) {
            val type = data.type
            val state= data.state
            when (type) {
                ManagerType.BLUETOOTH-> {
                    updateStatusButton(bluetoothStatusButton,state)
                }
                ManagerType.SOCKET   -> {
                    updateStatusButton(socketStatusButton, state)
                }
                else                 -> {
                    updateStatusButton(voiceStatusButton, state)
                }
            }
        }
    }

    // ================== OnClickListener ===============
    // One of the status buttons has been clicked
    override fun onClick(v: View) {
        when(v) {
            bluetoothStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.BLUETOOTH.name))
            }
            socketStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.SOCKET.name))
            }
            voiceStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.STATUS.name))
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

    // ====================OnSeekBarChangeListener =====================
    override fun onStopTrackingTouch(seekBar: SeekBar?) {}


    override fun onStartTrackingTouch(seekBar: SeekBar?) {}


    override fun onProgressChanged(seekBar: SeekBar?, progress: Int,fromUser: Boolean) {
        //sliderText.setText("" + progress)
    }


    val CLSS = "CoverFragment"
    val CAPTURE_SIZE = 256

    init {
        name = CLSS
        visualizer = null
        try {
            visualizer = Visualizer(0)
        }
        catch(ex:Exception) {
            Log.e(name, String.format("init: Failed to initialize visualizer (%s)",ex.localizedMessage))
        }
    }
}
