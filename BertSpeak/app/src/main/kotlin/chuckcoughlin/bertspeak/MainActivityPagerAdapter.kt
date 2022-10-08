/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak


import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import chuckcoughlin.bertspeak.tab.*

/**
 * This is a specialized page fragment for each tab position.
 * Return the appropriate fragment when requested.
 */
class MainActivityPagerAdapter(fragManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragManager,lifecycle) {
    private val tabTitles: Array<String>
    /**
     * Each page is a different class. If position is out of range, use 0
     * @param position page number
     * @return a new instance of the page.
     */
    override fun createFragment(position: Int): Fragment {
        var pos = position
        var frag: AssistantFragment
        if (position<0) pos = 0
        if(position>tabTitles.size - 1) pos = tabTitles.size - 1
        when (pos) {
            0 -> frag = CoverFragment()
            1 -> frag = TranscriptFragment()
            2 -> frag = RobotLogsFragment()
            3 -> frag = TablesTabFragment()
            4 -> frag = SettingsFragment()
            else -> {}
        }

        Log.i(CLSS, "getItem: " + position + ": fragment=" + frag.javaClass.getCanonicalName())
        frag.pageNumber = position
        frag.title = tabTitles[position]
        return frag as Fragment
    }

    /**
     * @return the number of pages in our repertoire.
     */
    override fun getItemCount(): Int {
        return tabTitles.size
    }

    fun getPageTitle(position: Int): CharSequence {
        return tabTitles[position]
    }

    companion object {
        private const val CLSS = "MainActivityPagerAdapter"
    }

    init {
        tabTitles = arrayOf(
            ctx.getString(R.string.cover_tab_label),
            ctx.getString(R.string.transcript_tab_label),
            ctx.getString(R.string.robot_log_tab_label),
            ctx.getString(R.string.tables_tab_label),
            ctx.getString(R.string.settings_tab_label)
        )
        Log.i(CLSS, "Constructor ...")
    }
}
