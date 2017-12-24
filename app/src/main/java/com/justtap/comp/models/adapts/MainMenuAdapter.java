package com.justtap.comp.models.adapts;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.justtap.comp.models.frags.LeaderboardsFragment;
import com.justtap.comp.models.frags.PlayFragment;
import com.justtap.comp.models.frags.SettingsFragment;

/**
 *
 */

public class MainMenuAdapter extends FragmentStatePagerAdapter {

    private int PAGE_COUNT = 0;

    public MainMenuAdapter(@NonNull FragmentManager fm, int pageCount) {
        super(fm);
        PAGE_COUNT = pageCount;
    }


    //This method is called by the system to get a page;
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new LeaderboardsFragment();
            case 1:
                return new PlayFragment();
            case 2:
                return new SettingsFragment();
            default:
                //You done GOOFED
                return null;
        }
    }


    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
