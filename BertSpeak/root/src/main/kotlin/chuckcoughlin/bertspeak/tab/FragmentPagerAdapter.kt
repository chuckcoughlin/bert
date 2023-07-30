package chuckcoughlin.bertspeak.tab

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import chuckcoughlin.bertspeak.R


/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class FragmentPagerAdapter(activity:FragmentActivity) : FragmentStateAdapter(activity) {
    val fragments: MutableList<Fragment>
    val titles: MutableList<String>

    fun addFragment(pos:Int,title: String) {
        Log.i(CLSS, "addFragment: "+title+" at " + pos)
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
            2 -> frag = TranscriptFragment(pos)
            3 -> frag = RobotLogsFragment(pos)
            4 -> frag = TablesTabFragment(pos)
            5 -> frag = SettingsFragment(pos)
            else ->
                frag = CoverFragment(pos)
        }

        Log.i(CLSS, "createFragment: " + pos + ": fragment=" + frag.javaClass.canonicalName)
        return frag
    }

    fun createFragmentOLD(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment.
        Log.i(CLSS, "createFragment: at " + position)
        return PlaceholderFragment.newInstance(position + 1)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    companion object {
        private const val CLSS = "SectionsPagerAdapter"
    }

    init {
        fragments = ArrayList()
        titles = ArrayList()
        // Populate the array
        addFragment(0,activity.getString(R.string.fragmentCoverLabel))
        addFragment(1,activity.getString(R.string.fragmentAnimationLabel))
        addFragment(2,activity.getString(R.string.fragmentTranscriptLabel))
        addFragment(3,activity.getString(R.string.fragmentLogsLabel))
        addFragment(4,activity.getString(R.string.fragmentTableLabel))
        addFragment(5,activity.getString(R.string.fragmentSettingsLabel))
    }
}
