package com.justtap.comp.models.adapts;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.justtap.comp.models.frags.GameOverStatsFragment;
import com.justtap.comp.models.frags.PlayFragment;

public class GameOverPagerAdapter extends FragmentStatePagerAdapter {

    private int PAGE_COUNT;
    private Bundle stats;


    public GameOverPagerAdapter(int pagecount, Bundle gameStats, FragmentManager fm) {
        super(fm);
        PAGE_COUNT = pagecount;
        stats = gameStats;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                Fragment statsFragment = new GameOverStatsFragment();
                statsFragment.setArguments(stats);
                return statsFragment;
            case 1:
                return new PlayFragment();
            default:
                return null;
        }
    }


    @Override
    public int getCount() {
        return PAGE_COUNT;
    }


}