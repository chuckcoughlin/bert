/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *            (MIT License)
 */

package chuckcoughlin.bert;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.db.SettingsManager;
import chuckcoughlin.bert.speech.SpokenTextManager;
import chuckcoughlin.bert.service.ServiceStatusManager;
import chuckcoughlin.bert.service.VoiceService;


public class MainActivity extends AppCompatActivity {
    private static final String CLSS = "MainActivity";
    private static final String DIALOG_TAG = "dialog";

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
        Log.d(CLSS,"Constructor ...");
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

        // Create the comprehensive voice connection service
        Intent intent = new Intent(this, VoiceService.class);
        getApplicationContext().startForegroundService(intent);
    }

    /**
     * Shutdown the VoiceService.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, VoiceService.class);
        stopService(intent);
    }
}
