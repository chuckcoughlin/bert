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
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.common.NameValue
import chuckcoughlin.bertspeak.data.SettingsObserver
import chuckcoughlin.bertspeak.data.StatusData
import chuckcoughlin.bertspeak.data.StatusDataObserver
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.data.TextDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentCoverBinding
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.service.ManagerType
import chuckcoughlin.bertspeak.service.PermissionManager
import chuckcoughlin.bertspeak.ui.RendererFactory
import chuckcoughlin.bertspeak.ui.StatusImageButton
import chuckcoughlin.bertspeak.ui.VerticalSeekBar
import chuckcoughlin.bertspeak.ui.waveform.WaveformView
import kotlin.math.roundToInt


/**
 * This fragment presents a static "cover" with a waveform view of the voice signal
 * plus a volume bar. There are three status sbuttons: Connect, Listen, Speak
 */
class CoverFragment (pos:Int): BasicAssistantFragment(pos), SettingsObserver,StatusDataObserver,TextDataObserver,
                                OnClickListener,OnDataCaptureListener,OnSeekBarChangeListener {

    override val name : String

    private var visualizer: Visualizer
    // This property is only valid between onCreateView and onDestroyView
    private lateinit var seekBar: VerticalSeekBar
    private lateinit var waveformView: WaveformView

    private lateinit var voiceText: TextView
    private lateinit var networkStatusButton: StatusImageButton
    private lateinit var hearingStatusButton: StatusImageButton
    private lateinit var stopStatusButton: StatusImageButton
    private lateinit var speechStatusButton: StatusImageButton

    // Inflate the view. It holds a fixed image of the robot
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        val binding = FragmentCoverBinding.inflate(inflater, container, false)
        networkStatusButton = binding.networkStatus  // ToggleButton
        hearingStatusButton    = binding.hearingStatus
        stopStatusButton      = binding.stopButton
        speechStatusButton     = binding.speechStatus
        voiceText             = binding.voiceEditText
        networkStatusButton.isClickable = true // Not really buttons, just indicators
        networkStatusButton.setOnClickListener(this)
        stopStatusButton.setOnClickListener(this)
        hearingStatusButton.isClickable = true
        hearingStatusButton.setOnClickListener(this)
        speechStatusButton.isClickable = true
        speechStatusButton.setOnClickListener(this)
        val rendererFactory = RendererFactory()
        waveformView = binding.waveformView
        waveformView.setRenderer(
            rendererFactory.createSimpleWaveformRenderer(Color.GREEN, Color.DKGRAY)
        )
        // Seek Bar 0-100 - set to current database value
        seekBar = binding.verticalSeekbar
        seekBar.isClickable = true
        seekBar.setOnSeekBarChangeListener(this)
        try {
            val prog = DatabaseManager.getSetting(BertConstants.BERT_VOLUME).toDouble().roundToInt()
            seekBar.progress = prog
        }
        catch(nfe:NumberFormatException) {
            Log.w(name, String.format("onCreateView: non-numeric database setting for volume"))
            seekBar.progress = 50
        }
        Log.i(name, String.format("onCreateView: seek bar at %d.",seekBar.progress))
        DispatchService.restoreAudio()
        val pm = PermissionManager(requireActivity())
        pm.askForPermissions()
        return binding.root
    }
    /**
     * Start visualizer and registerSettingsObserver for status updates
     */
    override fun onStart() {
        super.onStart()
        Log.i(name, "onStart: registering as observer")
        DispatchService.registerForTranscripts(this)
        DispatchService.registerForStatus(this)
        DatabaseManager.registerSettingsObserver(this)
        startVisualizer()

    }

    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForTranscripts(this)
        DispatchService.unregisterForStatus(this)
        DatabaseManager.unregisterSettingsObserver(this)
        stopVisualizer()
    }

    @Synchronized
    private fun startVisualizer() {
        try {
            visualizer.setDataCaptureListener(
                this,Visualizer.getMaxCaptureRate(),
                true,false
            )
            visualizer.captureSize = CAPTURE_SIZE
            visualizer.enabled = true
        }
        catch (ex: Exception) {  // This will fail in the emulator
            Log.i(name, String.format("startVisualizer: %s FAILED to start (%s).",
                name, ex.localizedMessage) )
        }
    }

    @Synchronized
    private fun stopVisualizer() {
        //visualizer.enabled = false
        visualizer.release()
        visualizer.setDataCaptureListener(null, 0, false, false)
    }

    /**
     * Map current action state to StatusImageButton icon. Checked in this order ...
     * gray - active = false
     * green- checked = true
     * yellow - checked = false
     * red - enabled = false
     * @param state
     */
    private fun updateStatusButton(btn: StatusImageButton, type: ManagerType,state: ManagerState) {
        Log.i(name, String.format("updateStatusButton (%s):%s",type.name,state.name))
            requireActivity().runOnUiThread(Runnable {
                btn.visibility = View.INVISIBLE
                btn.setButtonState(state)
                btn.visibility = View.VISIBLE
        })
    }

    // ===================== SettingdObserver =====================
    override fun resetSettings(list: List<NameValue>) {
        for (ddata in list) {
            updateSetting(ddata)
        }
    }

    override fun updateSetting(data: NameValue) {
        if(data.name.equals(data.name.equals(BertConstants.BERT_VOLUME))) {
            seekBar.progress = data.value.toInt()
        }
    }
    // ===================== StatusDataObserver =====================
    /**
     * This is called when we first establish the observer.
     */
    override fun resetStatus(list: List<StatusData>) {
        for (ddata in list) {
            updateStatus(ddata)
        }
    }

    /**
     * We have received an update from one of the internal managers. Use the
     * category to determine which.
     */
    override fun updateStatus(data: StatusData) {
        Log.i(name, String.format("update (%s):%s = %s",data.action,data.type,data.state))
        if (data.action.equals(DispatchConstants.ACTION_MANAGER_STATE)) {
            val type = data.type
            val state= data.state
            when (type) {
                ManagerType.SOCKET-> {
                    updateStatusButton(networkStatusButton,type,state)
                }
                ManagerType.SPEECH   -> {
                    updateStatusButton(speechStatusButton, type,state)
                }
                else                 -> {
                    updateStatusButton(hearingStatusButton, type,state)
                }
            }
        }
    }
    // ===================== TextDataObserver =====================
    /**
     * This is called when we first establish the observer.
     * Simply update the text
     */
    override fun resetText(list: List<TextData>) {
        if(list.size>0) {
            updateText(list.last())
        }
    }

    /**
     * We only handle MSG, VOICE. All other text types are ignored.
     */
    override fun updateText(msg: TextData) {
        Log.i(name, String.format("updateText (%s):%s", msg.type, msg.message))
        if(msg.type.equals(MessageType.ANS)) {
            voiceText.text = msg.message
            voiceText.setTextColor(Color.BLUE)
        }
        else if(msg.type.equals(MessageType.MSG)) {
            voiceText.text = msg.message
            voiceText.setTextColor(Color.BLACK)
        }
    }
    // ================== OnClickListener ===============
    // One of the status buttons has been clicked.
    override fun onClick(v: View) {
        when(v) {
            networkStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.SOCKET.name))
            }
            hearingStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.HEARING.name))
            }
            // The stop button triggers an immediate shutdown
            stopStatusButton -> {
                Log.i(name, String.format("onClick: application shutdown",))
                DispatchService.instance.stop()
                requireActivity().finishAndRemoveTask()
                System.exit(0)
            }
            // This button has three states.
            speechStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.SPEECH.name))
                DispatchService.toggleSpeechState()
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
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
    override fun onStartTrackingTouch(seekBar: SeekBar) {}

    override fun onProgressChanged(seekBar: SeekBar, progress: Int,fromUser: Boolean) {
        Log.i(name, String.format("onProgressChanged: seekBar at %d",progress))
        val vol = NameValue(BertConstants.BERT_VOLUME,progress.toString())
        DatabaseManager.updateSetting(vol)
    }

    val CLSS = "CoverFragment"
    val CAPTURE_SIZE = 256


    init {
        name = CLSS
        visualizer = Visualizer(0)
    }
}
