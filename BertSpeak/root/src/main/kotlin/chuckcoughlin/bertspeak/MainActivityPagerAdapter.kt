/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak


import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import chuckcoughlin.bertspeak.tab.AnimationFragment
import chuckcoughlin.bertspeak.tab.AssistantFragment
import chuckcoughlin.bertspeak.tab.CoverFragment
import chuckcoughlin.bertspeak.tab.PlaceholderFragment
import chuckcoughlin.bertspeak.tab.RobotLogsFragment
import chuckcoughlin.bertspeak.tab.SettingsFragment
import chuckcoughlin.bertspeak.tab.TablesTabFragment
import chuckcoughlin.bertspeak.tab.TranscriptFragment

/**
 * This is a specialized page fragment for each tab position.
 * Return the appropriate fragment when requested.
 */
class MainActivityPagerAdapter(act: FragmentActivity) : FragmentStateAdapter(act) {
    val activity:FragmentActivity = act
    val fragments: MutableList<Fragment> = arrayListOf<Fragment>()
    val titles: MutableList<String> = arrayListOf<String>()


    fun addFragment(pos:Int,title: String) {
        Log.i(CLSS, "addFragment: "+title+" at " + pos)
        val frag = createFragment(pos)
        fragments.add(frag)
        titles.add(title)
    }
    override fun createFragment(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment.
        Log.i(CLSS, "createFragment: at " + position)
        return PlaceholderFragment.newInstance(position + 1)
    }
    /**
     * Satisfy the interface. We've created all the fragments on startup, so just return it

    override fun createFragment(pos: Int): Fragment {
        Log.i(CLSS, "createFragment: " + pos )
        val frag = createAssistantFragment(pos)
        return frag as Fragment
    }
     */
    /**
     * Each page is a different class. If position is out of range, use 0
     * @param position page number
     * @return a new instance of the page.
     */
    fun createAssistantFragment(pos: Int): AssistantFragment {
        var frag: AssistantFragment
        when (pos) {
            0 -> frag = CoverFragment(pos)
            1 -> frag = AnimationFragment(pos)
            2 -> frag = TranscriptFragment(pos)
            3 -> frag = RobotLogsFragment(pos)
            4 -> frag = TablesTabFragment(pos)
            5 -> frag = SettingsFragment(pos)
            else ->
                frag = CoverFragment(pos)
        }

        Log.i(CLSS, "createAssistantFragment: " + pos + ": fragment=" + frag.javaClass.canonicalName)
        return frag
    }

    override fun getItemId(position:Int) : Long {
        var id = 0L

        var pos = position
        if (position<0) pos = 0
        if(position>itemCount - 1) pos = itemCount - 1
        when (pos) {
            0 -> R.id.fragmentCover
            1 -> R.id.fragmentAnimation
            2 -> R.id.fragmentTranscript
            3 -> R.id.fragmentRobotLogs
            4 -> R.id.fragmentTable
            5 -> R.id.fragmentSettings
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
        return fragments.size
    }

    private fun getFragment(position : Int): Fragment {
        return fragments[position]
    }

    public fun getTabTitle(position : Int): String {
        return titles[position]
    }
    companion object {
        private const val CLSS = "MainActivityPagerAdapter"
    }

    init {
        // Populate the array
        addFragment(0,activity.getString(R.string.fragmentCoverLabel))
        addFragment(1,activity.getString(R.string.fragmentAnimationLabel))
        addFragment(2,activity.getString(R.string.fragmentTranscriptLabel))
        addFragment(3,activity.getString(R.string.fragmentLogsLabel))
        addFragment(4,activity.getString(R.string.fragmentTableLabel))
        addFragment(5,activity.getString(R.string.fragmentSettingsLabel))
    }
}
