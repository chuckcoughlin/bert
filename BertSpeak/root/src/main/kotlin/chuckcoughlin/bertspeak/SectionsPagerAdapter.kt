package chuckcoughlin.bertspeak

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import chuckcoughlin.bertspeak.tab.PlaceholderFragment


/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(activity:FragmentActivity) : FragmentStateAdapter(activity) {
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

    override fun createFragment(position: Int): Fragment {
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
    }
}
