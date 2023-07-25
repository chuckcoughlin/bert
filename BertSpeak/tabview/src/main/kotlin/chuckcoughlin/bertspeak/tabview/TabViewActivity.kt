package chuckcoughlin.bertspeak.tabview

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import chuckcoughlin.bertspeak.tabview.ui.main.SectionsPagerAdapter
import chuckcoughlin.bertspeak.tabview.databinding.ActivityTabViewBinding

class TabViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTabViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTabViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)
    }
}
