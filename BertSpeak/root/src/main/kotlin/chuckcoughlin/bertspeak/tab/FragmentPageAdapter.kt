/**
 * Copyright 2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.tab

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import chuckcoughlin.bertspeak.R

/**
 * A [FragmentPageAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class FragmentPageAdapter(activity:FragmentActivity) : FragmentStateAdapter(activity) {
    val fragments: MutableList<Fragment>
    val titles: MutableList<String>

    fun addFragment(pos:Int,title: String) {
        //Log.i(CLSS, "addFragment: "+title+" at " + pos)
        fragments.add(createFragment(pos))
        titles.add(title)
    }
    public fun getTabTitle(position : Int): String {
        return titles[position]
    }

    /**
     * Each page is a different class. If position is out of range, use 0
     * @param position page number
     * @return a new instance of the page.
     */
    override fun createFragment(pos: Int): Fragment {
        var frag: Fragment
        when (pos) {
            0 -> frag = CoverFragment(pos)
            1 -> frag = AnimationFragment(pos)
            2 -> frag = FacesFragment(pos)
            3 -> frag = LogsFragment(pos)
            4 -> frag = PosesFragment(pos)
            5 -> frag = SettingsFragment(pos)
            6 -> frag = MotorPropertiesFragment(pos)
            7 -> frag = TranscriptFragment(pos)
            else ->
                frag = CoverFragment(pos)
        }

        Log.i(CLSS, "createFragment: " + pos + ": fragment=" + frag.javaClass.canonicalName)
        return frag
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    companion object {
        private const val CLSS = "FragmentPageAdapter"
    }

    init {
        fragments = ArrayList()
        titles = ArrayList()
        // Populate the array
        addFragment(0,activity.getString(R.string.fragmentCoverLabel))
        addFragment(1,activity.getString(R.string.fragmentAnimationLabel))
        addFragment(2,activity.getString(R.string.fragmentFacesLabel))
        addFragment(3,activity.getString(R.string.fragmentLogsLabel))
        addFragment(4,activity.getString(R.string.fragmentPosesLabel))
        addFragment(5,activity.getString(R.string.fragmentSettingsLabel))
        addFragment(6,activity.getString(R.string.fragmentStatusLabel))
        addFragment(7,activity.getString(R.string.fragmentTranscriptLabel))
    }
}
