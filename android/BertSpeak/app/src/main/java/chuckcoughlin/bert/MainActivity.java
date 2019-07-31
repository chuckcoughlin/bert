/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *            (MIT License)
 */

package chuckcoughlin.bert;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.common.IntentObserver;
import chuckcoughlin.bert.common.MessageType;
import chuckcoughlin.bert.service.DispatchService;
import chuckcoughlin.bert.service.DispatchServiceBinder;
import chuckcoughlin.bert.service.FacilityState;
import chuckcoughlin.bert.service.TextManager;
import chuckcoughlin.bert.service.TieredFacility;
import chuckcoughlin.bert.service.VoiceConstants;
import chuckcoughlin.bert.speech.Annunciator;
import chuckcoughlin.bert.speech.SpeechAnalyzer;
import chuckcoughlin.bert.speech.TextMessage;
import chuckcoughlin.bert.speech.TextMessageObserver;

/**
 * The main activity "owns" the page tab UI fragments. It also contains
 * the speech components, since they must execute on the main thread
 * (and not in the service).
 */
public class MainActivity extends AppCompatActivity
                          implements IntentObserver, TextMessageObserver,TextToSpeech.OnInitListener,
                                        ServiceConnection {
    private static final String CLSS = "MainActivity";
    private final static String UTTERANCE_ID = CLSS;
    private SpeechAnalyzer analyzer = null;
    private Annunciator annunciator = null;
    private DispatchService service = null;
    private UtteranceListener ul = new UtteranceListener();
    // Start phrases to choose from ...
    private static final String[] phrases = {
            "My speech module is ready",
            "The speech connection is enabled",
            "I am ready for voice commands",
            "The speech controller is ready"
    };

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager viewPager;

    public MainActivity() {
        Log.d(CLSS, "Main Activity startup ...");
    }

    /**
     * It is possible to restart the activity in tbe same JVM leaving our singletons intact.
     *
     * @param savedInstanceState the saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the comprehensive dispatch connection service
        Intent intent = new Intent(this, DispatchService.class);
        getApplicationContext().startForegroundService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        Log.i(CLSS, "onCreate ...");
        // If I absolutely have to start over again with the database ...
        //this.deleteDatabase(BertConstants.DB_NAME);

        setContentView(R.layout.activity_main);
        // Close the soft keyboard - it will still open on an EditText
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        ViewPager viewPager = findViewById(R.id.viewpager);
        MainActivityPagerAdapter pagerAdapter = new MainActivityPagerAdapter(getSupportFragmentManager(), getApplicationContext());
        viewPager.setAdapter(pagerAdapter);
    }

    /**
     * Bind to the DispatchService, start speech analyzer and annunciator
     */
    @Override
    public void onStart() {
        super.onStart();
        activateSpeechAnalyzer();
        annunciator = new Annunciator(getApplicationContext(), this);
        annunciator.setOnUtteranceProgressListener(ul);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (service != null) {
            unbindService(this);
            service = null;
        }
        annunciator.stop();
        deactivateSpeechAnalyzer();
    }

    /**
     * Shutdown the DispatchService and text resources
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        deactivateSpeechAnalyzer();
        Intent intent = new Intent(this, DispatchService.class);
        stopService(intent);
        annunciator.shutdown();
        annunciator = null;
    }

    /**
     * Select a random startup phrase from the list.
     *
     * @return the selected phrase.
     */
    private String selectRandomText() {
        double rand = Math.random();
        int index = (int) (rand * phrases.length);
        return phrases[index];
    }

    // =================================== OnInitListener ===============================
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Set<Voice> voices = annunciator.getVoices();
            if(voices!=null ) {
                for (Voice v : voices) {
                    if (v.getName().equalsIgnoreCase("en-GB-SMTm00")) {
                        Log.i(CLSS, String.format("onInit: voice = %s %d", v.getName(), v.describeContents()));
                        annunciator.setVoice(v);
                    }
                }
            }
            annunciator.setLanguage(Locale.UK);
            annunciator.setPitch(1.6f);       //Default＝1.0
            annunciator.setSpeechRate(1.0f);  // Default=1.0
            Log.i(CLSS, "onInit: TextToSpeech initialized ...");
            annunciator.speak(selectRandomText(), TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
        } else {
            Log.e(CLSS, String.format("onInit: TextToSpeech ERROR - %d", status));
            annunciator = null;  // don't use
        }


    }

    // ===================== IntentObserver =====================
    // Only turn on the speech recognizer if the action state is voice.
    @Override
    public void initialize(List<Intent> list) {
        for (Intent intent : list) {
            if (intent.hasCategory(VoiceConstants.CATEGORY_FACILITY_STATE)) {
                update(intent);
            }
        }
    }

    // For the speech analyzer to be active, the bluetooth socket should be live.
    // The speed analyzer must run on the "main thread"
    @Override
    public void update(Intent intent) {
        if (intent.hasCategory(VoiceConstants.CATEGORY_FACILITY_STATE)) {
            FacilityState actionState = FacilityState.valueOf(intent.getStringExtra(VoiceConstants.KEY_FACILITY_STATE));
            TieredFacility tf = TieredFacility.valueOf(intent.getStringExtra(VoiceConstants.KEY_TIERED_FACILITY));
            if (tf.equals(TieredFacility.SOCKET) && actionState.equals(FacilityState.ACTIVE)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activateSpeechAnalyzer();
                    }
                });
            }
            else if (tf.equals(TieredFacility.SOCKET) ) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deactivateSpeechAnalyzer();
                    }
                });
            }
        }
    }

    // =================================== ServiceConnection ===============================
    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (service != null) {
            service.getStatusManager().unregister(this);
            service.getTextManager().unregisterTranscriptViewer(this);
        }
        service = null;
        analyzer.shutdown();
        analyzer = null;
    }

    // name.getClassName() contains the class of the service.
    @Override
    public void onServiceConnected(ComponentName name, IBinder bndr) {
        DispatchServiceBinder binder = (DispatchServiceBinder) bndr;
        service = binder.getService();
        activateSpeechAnalyzer();
        service.getStatusManager().register(this);
        service.getTextManager().registerTranscriptViewer(this);
    }

    // Turn off the audio to mute the annoying beeping
    private void activateSpeechAnalyzer() {
        if (service != null && analyzer == null) {
            suppressAudio();
            analyzer = new SpeechAnalyzer(service, getApplicationContext());
            analyzer.start();
        }
    }

    private void deactivateSpeechAnalyzer() {
        if (analyzer != null) {
            restoreAudio();
            analyzer.shutdown();
        }
        analyzer = null;
    }

    private void restoreAudio() {
        //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
    }

    // Mute the beeps waiting for spoken input. At one point these methods were used to silence
    // annoying beeps with every onReadyForSpeech cycle. Currently they are not needed (??)
    private void suppressAudio() {
        //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
    }

    // =================================== TextMessageObserver ===============================
    @Override
    public String getName() { return CLSS; }
    @Override
    public void initialize() {}

    /**
     * If the message is a response from the robot, announce it.
     *
     * @param msg the new message
     */
    @Override
    public void update(TextMessage msg) {
        if (msg.getMessageType().equals(MessageType.ANS)) {
            restoreAudio();
            annunciator.speak(msg.getMessage());
            suppressAudio();
        }
    }

    // =================================== UtteranceProgressListener ===============================
    // Use this to suppress feedback with analyzer while we're speaking
    @SuppressWarnings("deprecation")
    public class UtteranceListener extends UtteranceProgressListener {

        @Override
        public synchronized void onDone(String utteranceId) {
            if( analyzer!=null ) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        analyzer.listen();
                    }
                });
            }
        }
        @Override
        public void onError(String utteranceId) {}
        @Override
        public void onError(String utteranceId,int code) {}
        @Override
        public void onStart(String utteranceId) {
            if( analyzer!=null ) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        analyzer.cancel();
                    }
                });
            }
        }
    }
}
