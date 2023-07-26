package chuckcoughlin.bertspeak.sandbox

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import chuckcoughlin.bertspeak.sandbox.ui.main.SectionsPagerAdapter
import chuckcoughlin.bertspeak.sandbox.databinding.ActivityTabViewBinding

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
