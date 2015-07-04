package me.robwilliams.pack.adapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import me.robwilliams.pack.fragment.PackFragment;
import me.robwilliams.pack.fragment.RepackFragment;
import me.robwilliams.pack.fragment.ShouldPackFragment;

public class TripDetailPagerAdapter extends FragmentPagerAdapter {

    private String[] pageTitles = {"Should Pack", "Pack", "Repack"};
    private int tripId;

    public TripDetailPagerAdapter(FragmentManager fm, int tripId) {
        super(fm);
        this.tripId = tripId;
    }

    @Override
    public Fragment getItem(int position) {

        Bundle bundle = new Bundle();
        bundle.putInt("tripId", tripId);

        switch (position) {
            case 0:
                ShouldPackFragment shouldPackFragment = new ShouldPackFragment();
                shouldPackFragment.setArguments(bundle);
                return shouldPackFragment;
            case 1:
                PackFragment packFragment = new PackFragment();
                packFragment.setArguments(bundle);
                return packFragment;
            case 2:
                RepackFragment repackFragment = new RepackFragment();
                repackFragment.setArguments(bundle);
                return repackFragment;
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
