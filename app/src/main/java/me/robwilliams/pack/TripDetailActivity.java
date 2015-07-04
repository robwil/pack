package me.robwilliams.pack;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;

import me.robwilliams.pack.adapter.TripDetailPagerAdapter;
import me.robwilliams.pack.data.DatabaseHelper;
import me.robwilliams.pack.data.TripContentProvider;
import me.robwilliams.pack.data.TripItem;

public class TripDetailActivity extends ActionBarActivity {

    private ViewPager viewPager;
    private TripDetailPagerAdapter mAdapter;
    private Uri tripUri;
    private int tripId;
    private ArrayList<TripItem> tripItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        tripItems = new ArrayList<>();

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

            // Now that we've loaded the Trip, load Trip Items
            SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
            cursor = db.rawQuery("SELECT L.name as list_name, L.weight as list_weight, I._id as item_id, I.name as item_name, I.weight as item_weight, TI.status as status FROM trip T " +
                    "INNER JOIN trip_listset TLS ON T._id=TLS.trip_id AND TLS.trip_id=" + tripId + " " +
                    "INNER JOIN listset_list LSL ON LSL.listset_id=TLS.listset_id " +
                    "INNER JOIN list L ON L._id=LSL.list_id " +
                    "INNER JOIN item I ON I.list_id=LSL.list_id " +
                    "LEFT JOIN trip_item TI ON TI.trip_id=TLS.trip_id AND TI.item_id=I._id " +
                    "ORDER BY list_weight DESC, list_name ASC, item_weight DESC, item_name ASC", null);

            if (cursor != null) {
                // Loop through cursor and store items in a local repository
                while (cursor.moveToNext()) {
                    String listName = cursor.getString(cursor.getColumnIndexOrThrow("list_name"));
                    int itemId = cursor.getInt(cursor.getColumnIndexOrThrow("item_id"));
                    String itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                    tripItems.add(new TripItem(itemId, listName, itemName, status));
                }

                // And now with the Trip Items, finish setting up UI
                setTitle("Trip: " + tripName);
                viewPager = (ViewPager) findViewById(R.id.pager);
                mAdapter = new TripDetailPagerAdapter(getSupportFragmentManager(), tripId, tripItems);
                viewPager.setAdapter(mAdapter);

                PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
                tabs.setViewPager(viewPager);
                tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

                    @Override
                    public void onPageSelected(int position) {
                        // This forced recreation of view fragment even if it was created previously.
                        // This ensures we always have fresh data.
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {}
                });
            }
        }
    }

}
