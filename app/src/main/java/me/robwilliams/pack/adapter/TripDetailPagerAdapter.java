package me.robwilliams.pack.adapter;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.fragment.PackFragment;
import me.robwilliams.pack.fragment.RepackFragment;
import me.robwilliams.pack.fragment.ShouldPackFragment;

public class TripDetailPagerAdapter extends FragmentPagerAdapter {

    private String[] pageTitles = {"Should Pack", "Pack", "Repack"};
    private int tripId;
    private ArrayList<TripItem> tripItems;

    public TripDetailPagerAdapter(FragmentManager fm, int tripId, ArrayList<TripItem> tripItems) {
        super(fm);
        this.tripId = tripId;
        this.tripItems = tripItems;
    }

    @Override
    public Fragment getItem(int position) {

        Bundle bundle = new Bundle();
        bundle.putInt("tripId", tripId);
        bundle.putParcelableArrayList("tripItems", tripItems);

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

    @Override
    public int getItemPosition(Object object) {
        // This forces views to be recreated whenever the data set change notification comes.
        return POSITION_NONE;
    }
}
