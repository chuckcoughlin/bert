/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
import chuckcoughlin.bertspeak.bert.common.IntentObserver
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.service.*
import chuckcoughlin.bertspeak.speech.Annunciator
import chuckcoughlin.bertspeak.speech.SpeechAnalyzer
import chuckcoughlin.bertspeak.speech.TextMessage
import chuckcoughlin.bertspeak.speech.TextMessageObserver
import java.util.*

/**
 * The main activity "owns" the page tab UI fragments. It also contains
 * the speech components, since they must execute on the main thread
 * (and not in the service).
 *
 * @see Android ViewPager2 sample code CardFragmentActivity
 */
class MainActivity : FragmentActivity(), IntentObserver, TextMessageObserver,
    TextToSpeech.OnInitListener,
    ServiceConnection {
    private var analyzer: SpeechAnalyzer? = null
    private var annunciator: Annunciator? = null
    private var service: DispatchService? = null
    private val ul = UtteranceListener()

    /**
     * The [ViewPager] that will host the section contents.
     */
    private val viewPager2: ViewPager? = null

    /**
     * It is possible to restart the activity in tbe same JVM leaving our singletons intact.
     *
     * @param savedInstanceState the saved instance state
     */
    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create the comprehensive dispatch connection service
        val intent = Intent(this, DispatchService::class.java)
        getApplicationContext().startForegroundService(intent)
        bindService(intent, this, Context.BIND_AUTO_CREATE)
        Log.i(CLSS, "onCreate ...")
        // If we absolutely have to start over again with the database ...
        //this.deleteDatabase(BertConstants.DB_NAME);
        setContentView(R.layout.activity_main)
        // Close the soft keyboard - it will still open on an EditText
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        val viewPager: ViewPager = findViewById(R.id.viewpager)
        val pagerAdapter =
            MainActivityPagerAdapter(getSupportFragmentManager(), getApplicationContext())
        viewPager.setAdapter(pagerAdapter)
    }

    /**
     * Bind to the DispatchService, start speech analyzer and annunciator
     */
    override fun onStart() {
        super.onStart()
        activateSpeechAnalyzer()
        annunciator = Annunciator(getApplicationContext(), this)
        annunciator.setOnUtteranceProgressListener(ul)
    }

    override fun onStop() {
        super.onStop()
        if (service != null) {
            unbindService(this)
            service = null
        }
        annunciator.stop()
        deactivateSpeechAnalyzer()
    }

    /**
     * Shutdown the DispatchService and text resources
     */
    override protected fun onDestroy() {
        super.onDestroy()
        deactivateSpeechAnalyzer()
        val intent = Intent(this, DispatchService::class.java)
        stopService(intent)
        annunciator.shutdown()
        annunciator = null
    }

    /**
     * Select a random startup phrase from the list.
     *
     * @return the selected phrase.
     */
    private fun selectRandomText(): String {
        val rand = Math.random()
        val index = (rand * phrases.size).toInt()
        return phrases[index]
    }

    // =================================== OnInitListener ===============================
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val voices: Set<Voice> = annunciator.getVoices()
            if (voices != null) {
                for (v in voices) {
                    if (v.getName().equals("en-GB-SMTm00", ignoreCase = true)) {
                        Log.i(
                            CLSS,
                            String.format(
                                "onInit: voice = %s %d",
                                v.getName(),
                                v.describeContents()
                            )
                        )
                        annunciator.setVoice(v)
                    }
                }
            }
            annunciator.setLanguage(Locale.UK)
            annunciator.setPitch(1.6f) //Default＝1.0
            annunciator.setSpeechRate(1.0f) // Default=1.0
            Log.i(CLSS, "onInit: TextToSpeech initialized ...")
            annunciator.speak(selectRandomText(), TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        } else {
            Log.e(CLSS, String.format("onInit: TextToSpeech ERROR - %d", status))
            annunciator = null // don't use
        }
    }

    override val name: String
        get() = TODO("Not yet implemented")

    // ===================== IntentObserver =====================
    // Only turn on the speech recognizer if the action state is voice.
    override fun initialize(list: List<Intent>) {
        for (intent in list) {
            if (intent.hasCategory(VoiceConstants.CATEGORY_FACILITY_STATE)) {
                update(intent)
            }
        }
    }

    // For the speech analyzer to be active, the bluetooth socket should be live.
    // The speed analyzer must run on the "main thread"
    override fun update(intent: Intent) {
        if (intent.hasCategory(VoiceConstants.CATEGORY_FACILITY_STATE)) {
            val actionState: FacilityState =
                FacilityState.valueOf(intent.getStringExtra(VoiceConstants.KEY_FACILITY_STATE))
            val tf: TieredFacility =
                TieredFacility.valueOf(intent.getStringExtra(VoiceConstants.KEY_TIERED_FACILITY))
            if (tf == TieredFacility.SOCKET && actionState == FacilityState.ACTIVE) {
                runOnUiThread(Runnable { activateSpeechAnalyzer() })
            } else if (tf == TieredFacility.SOCKET) {
                runOnUiThread(Runnable { deactivateSpeechAnalyzer() })
            }
        }
    }

    // =================================== ServiceConnection ===============================
    override fun onServiceDisconnected(name: ComponentName) {
        if (service != null) {
            service.getStatusManager().unregister(this)
            service.getTextManager().unregisterTranscriptViewer(this)
        }
        service = null
        analyzer.shutdown()
        analyzer = null
    }

    // name.getClassName() contains the class of the service.
    override fun onServiceConnected(name: ComponentName, bndr: IBinder) {
        val binder: DispatchServiceBinder = bndr as DispatchServiceBinder
        service = binder.getService()
        activateSpeechAnalyzer()
        service.getStatusManager().register(this)
        service.getTextManager().registerTranscriptViewer(this)
    }

    // Turn off the audio to mute the annoying beeping
    private fun activateSpeechAnalyzer() {
        if (service != null && analyzer == null) {
            suppressAudio()
            analyzer = SpeechAnalyzer(service, getApplicationContext())
            analyzer.start()
        }
    }

    private fun deactivateSpeechAnalyzer() {
        if (analyzer != null) {
            restoreAudio()
            analyzer.shutdown()
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

    // =================================== TextMessageObserver ===============================
    override fun getName(): String {
        return CLSS
    }

    override fun initialize() {}

    /**
     * If the message is a response from the robot, announce it.
     *
     * @param msg the new message
     */
    override fun update(msg: TextMessage) {
        if (msg.getMessageType() == MessageType.ANS) {
            restoreAudio()
            annunciator.speak(msg.getMessage())
            suppressAudio()
        }
    }

    // =================================== UtteranceProgressListener ===============================
    // Use this to suppress feedback with analyzer while we're speaking
    inner class UtteranceListener : UtteranceProgressListener() {
        @Synchronized
        override fun onDone(utteranceId: String) {
            if (analyzer != null) {
                runOnUiThread(Runnable { analyzer.listen() })
            }
        }

        override fun onError(utteranceId: String) {}
        override fun onError(utteranceId: String, code: Int) {}
        override fun onStart(utteranceId: String) {
            if (analyzer != null) {
                runOnUiThread(Runnable { analyzer.cancel() })
            }
        }
    }

    companion object {
        private const val CLSS = "MainActivity"
        private const val UTTERANCE_ID = CLSS

        // Start phrases to choose from ...
        private val phrases = arrayOf(
            "My speech module is ready",
            "The speech connection is enabled",
            "I am ready for voice commands",
            "The speech controller is ready"
        )
    }

    init {
        Log.d(CLSS, "Main Activity startup ...")
    }
}
