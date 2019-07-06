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
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import chuckcoughlin.bert.service.DispatchService;
import chuckcoughlin.bert.service.DispatchServiceBinder;
import chuckcoughlin.bert.speech.Annunciator;
import chuckcoughlin.bert.speech.SpeechAnalyzer;

/**
 * The main activity "owns" the page tab UI fragments. It also contains
 * the speech components, since they must execute on the main thread
 * (and not in the service).
 */
public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, ServiceConnection {
    private static final String CLSS = "MainActivity";
    private static final String DIALOG_TAG = "dialog";
    private SpeechAnalyzer analyzer = null;
    private Annunciator annunciator;
    private DispatchService service = null;

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
    }

    /**
     * It is possible to restart the activity in tbe same JVM leaving our singletons intact.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the comprehensive dispatch connection service
        Intent intent = new Intent(this, DispatchService.class);
        getApplicationContext().startForegroundService(intent);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

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
    }

    /**
     * Bind to the DispatchService, start speech analyzer and annunciator
     */
    @Override
    public void onStart() {
        super.onStart();

        if( service!=null && analyzer==null) {
            analyzer = new SpeechAnalyzer(service,getApplicationContext());
            analyzer.start();
        }
        annunciator = new Annunciator(getApplicationContext(),this);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService(this);
        annunciator.stop();
    }

    /**
     * Shutdown the DispatchService and text resources
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
    // =================================== ServiceConnection ===============================
    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
        analyzer.shutdown();
        analyzer = null;
    }

    // name.getClassName() contains the class of the service.
    @Override
    public void onServiceConnected(ComponentName name, IBinder bndr) {
        DispatchServiceBinder binder = (DispatchServiceBinder) bndr;
        service = binder.getService();
        if( analyzer==null ) {
            analyzer = new SpeechAnalyzer(service,getApplicationContext());
            analyzer.start();
        }
    }
}
