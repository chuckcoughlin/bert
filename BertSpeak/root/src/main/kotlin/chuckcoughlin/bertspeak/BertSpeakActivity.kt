/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */

package chuckcoughlin.bertspeak

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import chuckcoughlin.bertspeak.databinding.BertspeakMainBinding
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.common.DispatchConstants
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
    private lateinit var binding: BertspeakMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Temporary code to throw errors when resource leaks encountered
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects().build()
        )
        // If we absolutely have to start over again with the database ...
        //deleteDatabase(BertConstants.DB_NAME);

        // Start the comprehensive dispatch connection service
        // This must be in place before the fragments
        val intent = Intent(this, DispatchService::class.java)
        intent.action = DispatchConstants.ACTION_START_SERVICE
        startService(intent)

        // get device dimensions
        val width = getScreenWidth()
        val height = getScreenHeight()
        Log.i(CLSS, String.format("onCreate: ... inflating binding (%d x %d)",height,width ))
        binding = BertspeakMainBinding.inflate(layoutInflater)
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
    }

    /**
     * Send a startup intent to the DispatchService
     */
    override fun onStart() {
        super.onStart()
        Log.i(CLSS, String.format("onStart: ..." ))



    }

    override fun onStop() {
        super.onStop()
        val intent = Intent(this,DispatchService::class.java)
        intent.action = DispatchConstants.ACTION_STOP_SERVICE
        stopService(intent)
    }

    /**
     * Shutdown the DispatchService and text resources
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.i(CLSS, String.format("onDestroy: ..." ))

    }

    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    private val CLSS = "BertSpeakActivity"

    init {
        Log.d(CLSS, "Main activity init ...")
        name = CLSS
    }
}
