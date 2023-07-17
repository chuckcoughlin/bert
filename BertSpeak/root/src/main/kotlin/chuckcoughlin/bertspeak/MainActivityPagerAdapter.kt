/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak


import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
class MainActivityPagerAdapter(activity: FragmentActivity, titles: Array<String>) : FragmentStateAdapter(activity) {
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

    override fun getItemId(position:Int) : Long {
        var id = 0L
        var pos = position
        if (position<0) pos = 0
        if(position>tabTitles.size - 1) pos = tabTitles.size - 1
        when (pos) {
            0 -> R.id.fragmentCover
            1 -> R.id.fragmentTranscript
            2 -> R.id.fragmentRobotLogs
            3 -> R.id.fragmentTable
            4 -> R.id.fragmentSettings
            else ->
                R.id.fragmentCover
        }

        Log.i(CLSS, "getItemId: " + position + ": id" + id)
        return id
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
