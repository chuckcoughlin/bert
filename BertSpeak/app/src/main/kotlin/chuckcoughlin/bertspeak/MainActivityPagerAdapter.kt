/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak

import android.content.Context
import android.support.v4.app.Fragment
import android.util.Log

/**
 * This is a specialized page fragment for each tab position.
 * Return the appropriate fragment when requested.
 */
class MainActivityPagerAdapter(fm: FragmentManager?, ctx: Context) : FragmentStatePagerAdapter(fm) {
    private val tabTitles: Array<String>

    /**
     * Each page is a different class.
     * @param position page number
     * @return a new instance of the page.
     */
    fun getItem(position: Int): Fragment? {
        var frag: AssistantFragment? = null
        when (position) {
            0 -> frag = CoverFragment()
            1 -> frag = TranscriptFragment()
            2 -> frag = RobotLogsFragment()
            3 -> frag = TablesTabFragment()
            4 -> frag = SettingsFragment()
            else -> {}
        }
        if (frag != null) {
            Log.i(CLSS, "getItem: " + position + ": fragment=" + frag.javaClass.getCanonicalName())
            frag.setPageNumber(position)
            frag.setTitle(tabTitles[position])
        }
        return frag as Fragment?
    }

    /**
     * @return the number of pages in our repertoire.
     */
    fun getCount(): Int {
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