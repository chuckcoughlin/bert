/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak


import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import chuckcoughlin.bertspeak.tab.AssistantFragment
import chuckcoughlin.bertspeak.tab.CoverFragment
import chuckcoughlin.bertspeak.tab.RobotLogsFragment
import chuckcoughlin.bertspeak.tab.SettingsFragment
import chuckcoughlin.bertspeak.tab.TablesTabFragment
import chuckcoughlin.bertspeak.tab.TranscriptFragment

/**
 * This is a specialized page fragment for each tab position.
 * Return the appropriate fragment when requested.
 */
class MainActivityPagerAdapter(fragManager: FragmentManager, lifecycle: Lifecycle,titles: Array<String>) : FragmentStateAdapter(fragManager,lifecycle) {
    private val tabTitles = titles
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
            0 -> frag = CoverFragment(pos)
            1 -> frag = TranscriptFragment(pos)
            2 -> frag = RobotLogsFragment(pos)
            3 -> frag = TablesTabFragment(pos)
            4 -> frag = SettingsFragment(pos)
            else ->
                frag = CoverFragment(pos)
        }

        Log.i(CLSS, "createFragment: " + position + ": fragment=" + frag.javaClass.canonicalName)
        frag.title = getPageTitle(position)
        return frag as Fragment
    }

    /**
     * @return the number of pages in our repertoire.
     */
    override fun getItemCount(): Int {
        return tabTitles.size
    }
    fun getPageTitle(position: Int): String {
        return tabTitles[position]
    }


    companion object {
        private const val CLSS = "MainActivityPagerAdapter"
    }
}
