package me.robwilliams.pack.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import me.robwilliams.pack.fragment.PackFragment;
import me.robwilliams.pack.fragment.RepackFragment;
import me.robwilliams.pack.fragment.ShouldPackFragment;

public class TripDetailPagerAdapter extends FragmentPagerAdapter {

    private String[] pageTitles = {"Should Pack", "Pack", "Repack"};

    public TripDetailPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                return new ShouldPackFragment();
            case 1:
                return new PackFragment();
            case 2:
                return new RepackFragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        return pageTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position >= pageTitles.length) {
            return "";
        }
        return pageTitles[position];
    }
}
