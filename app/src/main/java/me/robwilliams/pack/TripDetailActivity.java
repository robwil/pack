package me.robwilliams.pack;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import com.astuetz.PagerSlidingTabStrip;

import me.robwilliams.pack.adapter.TripDetailPagerAdapter;
import me.robwilliams.pack.data.TripContentProvider;

public class TripDetailActivity extends ActionBarActivity {

    private ViewPager viewPager;
    private TripDetailPagerAdapter mAdapter;
    private Uri tripUri;
    private int tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        // Restore Trip details from saved instance or Intent
        Bundle extras = getIntent().getExtras();

        tripUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
                .getParcelable(TripContentProvider.CONTENT_ITEM_TYPE);

        if (extras != null) {
            tripUri = extras
                    .getParcelable(TripContentProvider.CONTENT_ITEM_TYPE);

            fillData(tripUri);
        }
    }

    private void fillData(Uri uri) {
        String[] projection = { "_id", "name" };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String tripName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            tripId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            cursor.close();

            // Now that we've loaded the Trip, finish setting up UI
            setTitle("Trip: " + tripName);
            viewPager = (ViewPager) findViewById(R.id.pager);
            mAdapter = new TripDetailPagerAdapter(getSupportFragmentManager(), tripId);
            viewPager.setAdapter(mAdapter);

            PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
            tabs.setViewPager(viewPager);
        }
    }

}
