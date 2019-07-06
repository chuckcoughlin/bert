/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *            (MIT License)
 */

package chuckcoughlin.bert;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import chuckcoughlin.bert.service.DispatchService;
import chuckcoughlin.bert.service.DispatchServiceConnection;
import chuckcoughlin.bert.speech.Annunciator;
import chuckcoughlin.bert.speech.SpeechAnalyzer;
import chuckcoughlin.bert.speech.SpokenTextManager;

/**
 * The main activity "owns" the page tab UI fragments. It also contains
 * the speech components, since they must execute on the main thread
 * (and not in the service).
 */
public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String CLSS = "MainActivity";
    private static final String DIALOG_TAG = "dialog";
    private DispatchService dispatchServoce = null;
    private DispatchServiceConnection serviceConnection = null;
    private SpeechAnalyzer analyzer = null;
    private Annunciator annunciator;

    /**
     * A specialized {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the application pages. We use a
     * {@link android.support.v4.app.FragmentStatePagerAdapter} so as to conserve
     * memory if the list of pages is great.
     */
    private MainActivityPagerAdapter pagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager viewPager;

    public MainActivity() {
        Log.d(CLSS,"Main Activity startup ...");
        this.serviceConnection = new DispatchServiceConnection();
    }

    /**
     * It is possible to restart the activity in tbe same JVM leaving our singletons intact.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(CLSS,"onCreate ...");
        // If I absolutely have to start over again with the database ...
        //this.deleteDatabase(BertConstants.DB_NAME);

        setContentView(R.layout.activity_main);
        // Close the soft keyboard - it will still open on an EditText
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        ViewPager viewPager = findViewById(R.id.viewpager);
        pagerAdapter = new MainActivityPagerAdapter(getSupportFragmentManager(),getApplicationContext());
        viewPager.setAdapter(pagerAdapter);

        // Create the comprehensive dispatch connection service
        Intent intent = new Intent(this, DispatchService.class);
        getApplicationContext().startForegroundService(intent);
    }

    /**
     * Bind to the DispatchService, start speech analyzer and enunciator
     */
    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, DispatchService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        if(serviceConnection!=null) {
            analyzer = new SpeechAnalyzer(serviceConnection.getService(),getApplicationContext());
            analyzer.start();
        }
        annunciator = new Annunciator(getApplicationContext(),this);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        annunciator.stop();
    }

    /**
     * Shutdown the DispatchService.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(analyzer!=null) analyzer.shutdown();
        Intent intent = new Intent(this, DispatchService.class);
        stopService(intent);
        annunciator.shutdown();
        annunciator = null;
    }

    // =================================== OnInitListener ===============================
    @Override
    public void onInit(int status) {
        Log.i(CLSS,String.format("onInit: SpeechToText status - %d",status));
        /*
            For when we need to select an appropriate speaker ... maybe one of these
            en-gb-x-rjs#male_2-local
            en-gb-x-fis#male_1-local
            en-gb-x-fis#male_3-local

        Set<Voice> voices = annunciator.getVoices();
        for( Voice v:voices) {
            Log.i(CLSS,String.format("oninit: voice = %s %d",v.getName(),v.describeContents()));
        }
          */
    }
}
