/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */

package chuckcoughlin.bertspeak

import android.content.pm.PackageManager
import android.content.res.Resources
import android.Manifest
import android.media.MediaRecorder
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import chuckcoughlin.bertspeak.databinding.BertspeakMainBinding
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.tab.FragmentPageAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


/**
 * The main activity "owns" the page tab UI fragments.
 * AppCompatActivity is a FragmentActivity.
 */
class BertSpeakActivity : AppCompatActivity() {
    val name: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Temporary code to throw errors when resource leaks encountered
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects().build()
        )
        checkPermissions()

        // Apparently the AudioManager on the emulator is configured
        // at too low fidelity for speech
        val mr = MediaRecorder(baseContext)
        mr.setAudioSamplingRate(11)
        mr.setAudioEncodingBitRate(20)

        // If we absolutely have to start over again with the database ...
        //deleteDatabase(BertConstants.DB_NAME);

        // get device dimensions
        val width = getScreenWidth()
        val height = getScreenHeight()
        Log.i(CLSS, String.format("onCreate: ... inflating binding (%d x %d)",height,width ))
        val binding = BertspeakMainBinding.inflate(layoutInflater)
        Log.i(CLSS, "onCreate: ... binding inflated")
        setContentView(binding.root)

        val adapter = FragmentPageAdapter(this)
        val pager: ViewPager2 = binding.mainViewPager
        pager.currentItem = 0
        pager.adapter = adapter

        val tabs: TabLayout = binding.mainTabs
        TabLayoutMediator(tabs, pager) { tab, position->
            tab.text = adapter.getTabTitle(position)
        }.attach()
        // Close the soft keyboard - it will still open on an EditText
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        /* To get swipe event of viewpager2
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

         */

        // Initialize the database
        DatabaseManager.initialize()
        DispatchService.instance.context = this.baseContext
    }

    /**
     * The visualizer needs run-time permissions.
     * If permissions have not been granted in the tablet settings, OKs are requested on application startup.
     */
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),RCODE)
        }
    }

    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }


    private val CLSS = "BertSpeakActivity"
    private val RCODE= 23

    init {
        Log.d(CLSS, "Main activity init ...")
        name = CLSS
    }
}
