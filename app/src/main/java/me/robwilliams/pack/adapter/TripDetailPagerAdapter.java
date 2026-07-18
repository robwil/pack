package me.robwilliams.pack.adapter;

import android.os.Bundle;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

import me.robwilliams.pack.data.Bag;
import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.fragment.PackFragment;
import me.robwilliams.pack.fragment.RepackFragment;
import me.robwilliams.pack.fragment.ShouldPackFragment;
import me.robwilliams.pack.fragment.TripNotesFragment;

public class TripDetailPagerAdapter extends FragmentStatePagerAdapter {

    private String[] pageTitles = {"Should Pack", "Pack", "Repack", "Notes"};
    private int tripId;
    private ArrayList<TripItem> tripItems;
    private ArrayList<Bag> tripBags;
    private Fragment[] activeFragments = new Fragment[4];

    public TripDetailPagerAdapter(FragmentManager fm, int tripId, ArrayList<TripItem> tripItems, ArrayList<Bag> tripBags) {
        super(fm);
        this.tripId = tripId;
        this.tripItems = tripItems;
        this.tripBags = tripBags;
    }

    public void updateTripBags(ArrayList<Bag> tripBags) {
        this.tripBags = tripBags;
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {

        Bundle bundle = new Bundle();
        bundle.putInt("tripId", tripId);
        bundle.putParcelableArrayList("tripItems", tripItems);
        bundle.putParcelableArrayList("tripBags", tripBags);

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
            case 3:
                TripNotesFragment notesFragment = new TripNotesFragment();
                Bundle notesBundle = new Bundle();
                notesBundle.putInt("tripId", tripId);
                notesFragment.setArguments(notesBundle);
                return notesFragment;
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
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment f = (Fragment) super.instantiateItem(container, position);
        activeFragments[position] = f;
        return f;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        activeFragments[position] = null;
        super.destroyItem(container, position, object);
    }

    public Fragment getActiveFragment(int position) {
        if (position >= 0 && position < activeFragments.length) {
            return activeFragments[position];
        }
        return null;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
