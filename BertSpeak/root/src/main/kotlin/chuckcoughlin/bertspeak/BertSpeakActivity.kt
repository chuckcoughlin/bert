/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */

package chuckcoughlin.bertspeak

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Resources
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import chuckcoughlin.bertspeak.common.IntentObserver
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.databinding.BertspeakMainBinding
import chuckcoughlin.bertspeak.db.DatabaseHelper
import chuckcoughlin.bertspeak.service.ControllerState
import chuckcoughlin.bertspeak.service.ControllerType
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchServiceBinder
import chuckcoughlin.bertspeak.service.VoiceConstants
import chuckcoughlin.bertspeak.speech.Annunciator
import chuckcoughlin.bertspeak.speech.SpeechAnalyzer
import chuckcoughlin.bertspeak.speech.TextMessage
import chuckcoughlin.bertspeak.speech.TextMessageObserver
import chuckcoughlin.bertspeak.tab.FragmentPageAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Locale


/**
 * The main activity "owns" the page tab UI fragments. It also contains
 * the speech components, since they must execute on the main thread
 * (and not in the service). AppCompatActivity is a FragmentActivity.
 */
class BertSpeakActivity : AppCompatActivity() , IntentObserver, TextMessageObserver, TextToSpeech.OnInitListener, ServiceConnection {
    private var analyzer: SpeechAnalyzer? = null
    private var annunciator: Annunciator? = null
    private var service: DispatchService? = null

    override val name: String = CLSS
    private lateinit var binding: BertspeakMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Temporary code to throw errors when resource leaks encountered
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )
        // If we absolutely have to start over again with the database ...
        //deleteDatabase(BertConstants.DB_NAME);
        // Create the comprehensive dispatch connection service
        DispatchService.startForegroundService(this)
        // get device dimensions
        val width = getScreenWidth()
        val height = getScreenHeight()
        Log.i(CLSS, String.format("onCreate: ... inflating binding (%d x %d)",height,width ))
        binding = BertspeakMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = FragmentPageAdapter(this)
        val pager: ViewPager2 = binding.viewPager
        pager.currentItem = 0
        pager.adapter = adapter

        val tabs: TabLayout = binding.tabs
        TabLayoutMediator(tabs, pager) { tab, position->
            tab.text = adapter.getTabTitle(position)
        }.attach()
        // Close the soft keyboard - it will still open on an EditText
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        // To get swipe event of viewpager2
        pager.registerOnPageChangeCallback(object: OnPageChangeCallback() {
            // This method is triggered when there is any scrolling activity for the current page
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                //Log.i(CLSS, "onPageScrolled: ... page scrolled")
            }
            // triggered when you select a new page
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                Log.i(CLSS, "onPageSelected: ... page selected")
            }
            // triggered when there is
            // scroll state will be changed
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                //Log.i(CLSS, "onPageScrollStateChanged: ... page scroll state changed")
            }
        })

        // Initialize the database
        val helper = DatabaseHelper(this)
        helper.onCreate(helper.writableDatabase)
        helper.close()
    }


    /**
     * Bind to the DispatchService, start speech analyzer and annunciator
     */
    override fun onStart() {
        super.onStart()
        Log.i(CLSS, String.format("onStart: ..." ))
        activateSpeechAnalyzer()
        annunciator = Annunciator(applicationContext, this)
        annunciator!!.setOnUtteranceProgressListener(UtteranceListener())
        //val audioManager = getSystemService(AUDIO_SERVICE)
        suppressAudio() // TEMPORARY
    }

    override fun onStop() {
        super.onStop()
        if(service != null) {
            unbindService(this)
            service = null
        }
        annunciator?.stop()
        deactivateSpeechAnalyzer()
    }

    /**
     * Shutdown the DispatchService and text resources
     */
    override fun onDestroy() {
        super.onDestroy()
        deactivateSpeechAnalyzer()
        DispatchService.stopForegroundService(this)
        annunciator!!.shutdown()
        annunciator = null
    }

    /**
     * @return a random startup phrase from the list.
     */
    private fun selectRandomText(): String {
        val rand = Math.random()
        val index = (rand * phrases.size).toInt()
        return phrases[index]
    }

    // =================================== OnInitListener ===============================
    override fun onInit(status: Int) {
        if(status == TextToSpeech.SUCCESS) {
            val voices: Set<Voice> = annunciator!!.voices
            for(v in voices) {
                if(v.name.equals("en-GB-SMTm00", ignoreCase = true)) {
                    Log.i(CLSS, String.format("onInit: voice = %s %d",
                        v.name, v.describeContents()))
                    annunciator!!.voice = v
                }
            }
            annunciator!!.language = Locale.UK
            annunciator!!.setPitch(1.6f) //Default＝1.0
            annunciator!!.setSpeechRate(1.0f) // Default=1.0
            Log.i(CLSS, "onInit: TextToSpeech initialized ...")
            annunciator!!.speak(selectRandomText(), TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
        else {
            Log.e(CLSS, String.format("onInit: TextToSpeech ERROR - %d", status))
            annunciator = null  // Don't use
        }
    }

    // ===================== IntentObserver =====================
    // Only turn on the speech recognizer if the action state is voice.
    override fun initialize(list: List<Intent>) {
        for(intent in list) {
            if(intent.hasCategory(VoiceConstants.CATEGORY_CONTROLLER_STATE)) {
                update(intent)
            }
        }
    }

    // For the speech analyzer to be active, the bluetooth socket should be live.
    // The speed analyzer must run on the "main thread"
    override fun update(intent: Intent) {
        if(intent.hasCategory(VoiceConstants.CATEGORY_CONTROLLER_STATE)) {
            var value = intent.getStringExtra(VoiceConstants.KEY_CONTROLLER_STATE)
            if(value != null) {
                val actionState: ControllerState = ControllerState.valueOf(value)
                value = intent.getStringExtra(VoiceConstants.KEY_CONTROLLER)
                if(value != null) {
                    val tf: ControllerType =
                        ControllerType.valueOf(value)
                    if(tf == ControllerType.SOCKET && actionState == ControllerState.ACTIVE) {
                        runOnUiThread { activateSpeechAnalyzer() }
                    }
                    else if(tf == ControllerType.SOCKET) {
                        runOnUiThread { deactivateSpeechAnalyzer() }
                    }
                }
            }
        }
    }

    // =================================== ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if(service != null) {
            service!!.statusManager.unregister(this)
            service!!.getTextManager().unregisterTranscriptViewer(this)
        }
        service = null
        analyzer!!.shutdown()
        analyzer = null
    }

    // name.getClassName() contains the class of the service.
    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder: DispatchServiceBinder = bndr as DispatchServiceBinder
        service = binder.getService()
        activateSpeechAnalyzer()
        service!!.statusManager.register(this)
        service!!.getTextManager().registerTranscriptViewer(this)
    }

    // Turn off the audio to mute the annoying beeping
    private fun activateSpeechAnalyzer() {
        if(service != null && analyzer == null) {
            suppressAudio()
            analyzer = SpeechAnalyzer(service!!, applicationContext)
            analyzer!!.start()
        }
    }

    private fun deactivateSpeechAnalyzer() {
        if(analyzer != null) {
            restoreAudio()
            analyzer!!.shutdown()
        }
        analyzer = null
    }

    private fun restoreAudio() {
        //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
    }

    // Mute the beeps waiting for spoken input. At one point these methods were used to silence
    // annoying beeps with every onReadyForSpeech cycle. Currently they are not needed (??)
    private fun suppressAudio() {
        //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    // ================= TextMessageObserver ==================
    override fun initialize() {}

    /**
     * If the message is a response from the robot, announce it.
     *
     * @param msg the new message
     */
    override fun update(msg: TextMessage) {
        if(msg.messageType == MessageType.ANS) {
            restoreAudio()
            annunciator!!.speak(msg.message)
            suppressAudio()
        }
    }

    // ================= UtteranceProgressListener ========================
    // Use this to suppress feedback with analyzer while we're speaking
    inner class UtteranceListener: UtteranceProgressListener() {
        @Synchronized
        override fun onDone(utteranceId: String) {
            if(analyzer != null) {
                runOnUiThread { analyzer!!.listen() }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
        }

        override fun onError(utteranceId: String, code: Int) {}
        override fun onStart(utteranceId: String) {
            if(analyzer != null) {
                runOnUiThread { analyzer!!.cancel() }
            }
        }
    }

    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    companion object {
        private const val CLSS = "BertSpeakActivity"
        private const val UTTERANCE_ID = CLSS

        // Start phrases to choose from ...
        private val phrases = arrayOf(
            "My speech module is ready",
            "The speech connection is enabled",
            "I am ready for voice commands",
            "The speech controller is ready",
            "Marj I am ready",
            "Marj speak to me"
        )
    }

    init {
        Log.d(CLSS, "Main Activity startup ...")
    }
}
