package chuckcoughlin.bertspeak

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import chuckcoughlin.bertspeak.databinding.BertspeakMainBinding
import chuckcoughlin.bertspeak.tab.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * This application is simply a sandbox for experimentation with the UI.
 */
class BertSpeakActivity : AppCompatActivity() {
    private lateinit var binding: BertspeakMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = BertspeakMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = FragmentPagerAdapter(this)

        val pager: ViewPager2 = binding.viewPager
        pager.currentItem = 0
        pager.adapter = adapter

        val tabs: TabLayout = binding.tabs
        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = adapter.getTabTitle(position)
        }.attach()
    }
}
