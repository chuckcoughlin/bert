package chuckcoughlin.bertspeak.sandbox

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import chuckcoughlin.bertspeak.sandbox.ui.main.SectionsPagerAdapter
import chuckcoughlin.bertspeak.sandbox.databinding.ActivityTabViewBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * This application is simply a sandbox for experimentation with the UI.
 */
class TabViewActivity : AppCompatActivity() {
    private val TAB_TITLES = arrayOf<String>(
        "Tab 1",
        "Tab 2"
    )
    private lateinit var binding: ActivityTabViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTabViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = SectionsPagerAdapter(this)
        adapter.addFragment(0,TAB_TITLES[0])
        adapter.addFragment(1,TAB_TITLES[1])

        val pager: ViewPager2 = binding.viewPager
        pager.currentItem = 0
        pager.adapter = adapter

        val tabs: TabLayout = binding.tabs
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()


        //tabs.setupWithViewPager(pager)
    }
}
