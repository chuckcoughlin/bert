/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener
import android.os.Build.VERSION_CODES.R
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.Lifecycle
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.data.StatusData
import chuckcoughlin.bertspeak.data.StatusDataObserver
import chuckcoughlin.bertspeak.databinding.FragmentCoverBinding
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.service.ManagerType
import chuckcoughlin.bertspeak.service.ManagerType.BLUETOOTH
import chuckcoughlin.bertspeak.service.ManagerType.SOCKET
import chuckcoughlin.bertspeak.service.ManagerType.SPEECH
import chuckcoughlin.bertspeak.service.PermissionManager
import chuckcoughlin.bertspeak.ui.RendererFactory
import chuckcoughlin.bertspeak.ui.StatusImageButton
import chuckcoughlin.bertspeak.ui.VerticalSeekBar
import chuckcoughlin.bertspeak.ui.waveform.WaveformView
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt


/**
 * This fragment presents a static "cover" with a waveform view of the voice signal
 * plus a volume bar.
 */
class CoverFragment (pos:Int): BasicAssistantFragment(pos), StatusDataObserver, OnClickListener,OnDataCaptureListener,OnSeekBarChangeListener,RecognitionListener {

    override val name : String
    private var visualizer: Visualizer
    private val recognizerIntent : Intent
    private lateinit var sr : SpeechRecognizer
    // This property is only valid between onCreateView and onDestroyView
    private lateinit var seekBar: VerticalSeekBar
    private lateinit var waveformView: WaveformView

    private lateinit var bluetoothStatusButton: StatusImageButton
    private lateinit var socketStatusButton: StatusImageButton
    private lateinit var stopStatusButton: StatusImageButton
    private lateinit var voiceStatusButton: StatusImageButton

    // Inflate the view. It holds a fixed image of the robot
    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        val binding = FragmentCoverBinding.inflate(inflater, container, false)
        bluetoothStatusButton = binding.bluetoothStatus  // ToggleButton
        socketStatusButton    = binding.socketStatus
        stopStatusButton      = binding.stopButton
        voiceStatusButton     = binding.voiceStatus
        bluetoothStatusButton.isClickable = true // Not really buttons, just indicators
        bluetoothStatusButton.setOnClickListener(this)
        stopStatusButton.setOnClickListener(this)
        socketStatusButton.isClickable = true
        socketStatusButton.setOnClickListener(this)
        voiceStatusButton.isClickable = true
        voiceStatusButton.setOnClickListener(this)
        updateStatusButton(bluetoothStatusButton, BLUETOOTH, ManagerState.OFF)
        updateStatusButton(socketStatusButton, SOCKET,ManagerState.OFF)
        updateStatusButton(voiceStatusButton, SPEECH,ManagerState.OFF)
        val rendererFactory = RendererFactory()
        waveformView = binding.waveformView
        waveformView.setRenderer(
            rendererFactory.createSimpleWaveformRenderer(Color.GREEN, Color.DKGRAY)
        )
        // Seek Bar 0-100
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
        DispatchService.setVolume(seekBar.progress)
        Log.i(name, String.format("onCreateView: seek bar at %d.",seekBar.progress))
        DispatchService.restoreAudio()
        val pm = PermissionManager(requireActivity())
        pm.askForPermissions()
        sr = SpeechRecognizer.createSpeechRecognizer(requireContext())
        sr.setRecognitionListener(this)
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
        visualizer.enabled = false
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
        Log.i(name, String.format("update (%s):%s = %s",data.action,data.type,data.state))
        if (data.action.equals(DispatchConstants.ACTION_MANAGER_STATE)) {
            val type = data.type
            val state= data.state
            when (type) {
                ManagerType.BLUETOOTH-> {
                    updateStatusButton(bluetoothStatusButton,type,state)
                }
                ManagerType.SOCKET   -> {
                    updateStatusButton(socketStatusButton, type,state)
                }
                else                 -> {
                    updateStatusButton(voiceStatusButton, type,state)
                }
            }
        }
    }

    // ================== OnClickListener ===============
    // One of the status buttons has been clicked.
    override fun onClick(v: View) {
        when(v) {
            bluetoothStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.BLUETOOTH.name))
            }
            socketStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.SOCKET.name))
            }
            // The stop button triggers an immediate shutdown
            stopStatusButton -> {
                Log.i(name, String.format("onClick: application shutdown",))
                DispatchService.instance.stop()
                requireActivity().finishAndRemoveTask()
                System.exit(0)
            }
            voiceStatusButton -> {
                Log.i(name, String.format("onClick:%s",ManagerType.SPEECH.name))
                sr.startListening(recognizerIntent)
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
        DispatchService.setVolume(progress)
    }


    // ================================ RecognitionListener =============================
    override fun onReadyForSpeech(p0: Bundle?) {
        Log.i(name, "onReadyForSpeech: ....")
    }

    override fun onBeginningOfSpeech() {
        Log.i(name, "onBeginningOfSpeech: ....")
    }

    override fun onRmsChanged(p0: Float) {
        Log.i(name, "onRmsChanged: ....")
    }

    override fun onBufferReceived(p0: ByteArray?) {
        Log.i(name, "onBufferReceive: ....")
    }

    override fun onEndOfSpeech() {
        Log.i(name, "onEndOfSpeech: ....")
    }

    override fun onError(error: Int) {
        var reason:String =
            when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "Insufficient permission - Enable microphone in application"
                SpeechRecognizer.ERROR_NO_MATCH -> "no word match. Enunciate!"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "speech timeout"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_CLIENT -> "client error"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                    "recognition service is busy (started twice?)"
                SpeechRecognizer.ERROR_SERVER -> "server error"
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "too many requests"
                else -> String.format("ERROR (%d) ", error)
            }
        Log.e(CLSS, String.format("onError - %s", reason))
    }

    override fun onResults(results: Bundle?) {
        Log.i(CLSS, "onResults \n$results")
        if( results!=null && !results.isEmpty ) {
            // Fill the array with the strings the recognizer thought it could have heard, there should be 5
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)!!
            for (i in matches.indices) {
                Log.i(CLSS, "result " + matches[i])
            }
            // The zeroth result is usually the space-separated one
            if( !matches.isEmpty()) {
                var text = matches[0]
                Log.i(CLSS, "RESULT  = "+text)
            }
        }
    }

    override fun onPartialResults(p0: Bundle?) {
        Log.i(name, "onPartialResults: ....")
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        Log.i(name, "onEvent: ....")
    }
    private fun createRecognizerIntent(): Intent {
        //val locale = "us-UK"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        //intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, javaClass.getPackage()?.name)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) // Partials are always empty
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,SPEECH_MIN_TIME)
        //intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,END_OF_PHRASE_TIME)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak: ")
        //Give a hint to the recognizer about what the user is going to say
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        // Max number of results. This is two attempts at deciphering, not a 2-word limit.
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 2)
        return intent
    }
    val CLSS = "CoverFragment"
    val CAPTURE_SIZE = 256
    val REQUEST_CODE = 4242
    val BLUETOOTH_NAME = "Bluetooth"
    val SOCKET_NAME = "Socket"
    val VOICE_NAME = "Voice"

    init {
        name = CLSS
        recognizerIntent = createRecognizerIntent()
        visualizer = Visualizer(0)
    }


}
