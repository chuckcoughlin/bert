/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import chuckcoughlin.sb.assistant.tab.AssistantFragment;
import chuckcoughlin.sb.assistant.tab.AudioFragment;
import chuckcoughlin.sb.assistant.tab.CoverFragment;
import chuckcoughlin.sb.assistant.tab.DiscoveryFragment;
import chuckcoughlin.sb.assistant.tab.LogsFragment;
import chuckcoughlin.sb.assistant.tab.LidarFragment;
import chuckcoughlin.sb.assistant.tab.SettingsFragment;
import chuckcoughlin.sb.assistant.tab.SystemFragment;
import chuckcoughlin.sb.assistant.tab.TeleopFragment;


/**
 * There is a specialized page fragment for each tab position.
 * Return the appropriate fragment when requested.
 */

public class MainActivityPagerAdapter extends FragmentStatePagerAdapter {
    private final static String CLSS = "MainActivityPagerAdapter";
    private String[] tabTitles;

    public MainActivityPagerAdapter(FragmentManager fm,Context ctx) {
        super(fm);

        tabTitles = new String[] {
                ctx.getString(R.string.cover_tab_label), ctx.getString(R.string.discovery_tab_label), ctx.getString(R.string.settings_tab_label),ctx.getString(R.string.system_tab_label),
                ctx.getString(R.string.lidar_tab_label),ctx.getString(R.string.audio_tab_label),
                ctx.getString(R.string.teleop_tab_label),ctx.getString(R.string.log_tab_label)};
        Log.i(CLSS,"Constructor ...");
    }

    /**
     * Each page is a different class.
     * @param position page number
     * @return a new instance of the page.
     */
    @Override
    public Fragment getItem(int position) {
        AssistantFragment frag = null;

        switch (position) {
            case 0:
                frag = new CoverFragment();
                break;
            case 1:
                frag = new DiscoveryFragment();
                break;
            case 2:
                frag =  new SettingsFragment();
                break;
            case 3:
                frag =  new SystemFragment();
                break;
            case 4:
                frag = new LidarFragment();
                break;
            case 5:
                frag = new AudioFragment();
                break;
            case 6:
                frag = new TeleopFragment();
                break;
            case 7:
                frag = new LogsFragment();
                break;
            default:
        }
        if( frag!=null ) {
            Log.i(CLSS,"getItem: "+position+": fragment="+frag.getClass().getCanonicalName());
            frag.setPageNumber(position);
            frag.setTitle(tabTitles[position]);
        }
        return (Fragment)frag;
    }



    /**
     * @return the number of pages in our repertoire.
     */
    @Override
    public int getCount() {
        return tabTitles.length;
    }


    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
