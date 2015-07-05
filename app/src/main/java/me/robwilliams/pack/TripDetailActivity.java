package me.robwilliams.pack;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import me.robwilliams.pack.adapter.TripDetailPagerAdapter;
import me.robwilliams.pack.data.DatabaseHelper;
import me.robwilliams.pack.data.TripContentProvider;
import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.data.TripItemContentProvider;
import me.robwilliams.pack.data.TripSetContentProvider;
import me.robwilliams.pack.fragment.AbstractPackingFragment;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_trip_detail, menu);
        return true;
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
            cursor = db.rawQuery("SELECT L.name as list_name, L.weight as list_weight, I._id as item_id, I.name as item_name, I.weight as item_weight, TI.status as status, LSL.listset_id as listset_id FROM trip T " +
                    "INNER JOIN trip_listset TLS ON T._id=TLS.trip_id AND TLS.trip_id=" + tripId + " " +
                    "INNER JOIN listset_list LSL ON LSL.listset_id=TLS.listset_id " +
                    "INNER JOIN list L ON L._id=LSL.list_id " +
                    "INNER JOIN item I ON I.list_id=LSL.list_id " +
                    "LEFT JOIN trip_item TI ON TI.trip_id=TLS.trip_id AND TI.item_id=I._id " +
                    "ORDER BY list_weight DESC, list_name ASC, item_weight DESC, item_name ASC", null);

            if (cursor != null) {
                // Loop through cursor and store items in a local repository
                int maximumStatus = 0;
                while (cursor.moveToNext()) {
                    String listName = cursor.getString(cursor.getColumnIndexOrThrow("list_name"));
                    int itemId = cursor.getInt(cursor.getColumnIndexOrThrow("item_id"));
                    int listsetId = cursor.getInt(cursor.getColumnIndexOrThrow("listset_id"));
                    String itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                    if (status > maximumStatus) {
                        maximumStatus = status;
                    }
                    tripItems.add(new TripItem(itemId, listsetId, listName, itemName, status));
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

                // Set current tab based on maximum status in tripItems
                // can just pass status value directly since first page is Should Pack, second page is Pack, etc.
                viewPager.setCurrentItem(maximumStatus);
            }
        }
    }

    public void showCopyTripDialog(MenuItem item) {
        // Construct dialog to Name the Trip
        final EditText txtTripName = new EditText(this);
        int dpPadding = 16;  // 16 dps
        final float scale = getResources().getDisplayMetrics().density;
        int pxPadding = (int) (dpPadding * scale + 0.5f);
        txtTripName.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);

        final TripDetailActivity that = this;
        new AlertDialog.Builder(this)
            .setTitle("Copy Trip")
            .setMessage("Select a name for the new trip.")
            .setView(txtTripName)
            .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Toast.makeText(that, "Copying Trip...", Toast.LENGTH_SHORT).show();
                    // First, create the Trip with the given name.
                    String tripName = txtTripName.getText().toString();
                    ContentValues values = new ContentValues();
                    values.put("name", tripName);
                    Uri tripUri = getContentResolver().insert(TripContentProvider.CONTENT_URI, values);
                    long tripId = Long.parseLong(tripUri.getLastPathSegment());
                    // Iterate through existing Trip's items to figure out Sets and Items we want
                    Set<Integer> setIds = new HashSet<>();
                    Set<Integer> itemIds = new HashSet<>();
                    for (TripItem tripItem : tripItems) {
                        setIds.add(tripItem.getListsetId());
                        if (tripItem.getStatus() >= AbstractPackingFragment.STATUS_SHOULD_PACK) {
                            itemIds.add(tripItem.getItemId());
                        }
                    }
                    // Next setup the join table relationship between new Trip and original Trip's sets
                    for (int listsetId : setIds) {
                        values = new ContentValues();
                        values.put("trip_id", tripId);
                        values.put("listset_id", listsetId);
                        getContentResolver().insert(TripSetContentProvider.CONTENT_URI, values);
                    }
                    // Finally setup the Trip Item join table with a status of SHOULD_PACK
                    for (int itemId : itemIds) {
                        values = new ContentValues();
                        values.put("trip_id", tripId);
                        values.put("item_id", itemId);
                        values.put("status", AbstractPackingFragment.STATUS_SHOULD_PACK);
                        getContentResolver().insert(TripItemContentProvider.CONTENT_URI, values);
                    }
                    // Then transition to the new Trip
                    Intent i = new Intent(that, TripDetailActivity.class);
                    tripUri = Uri.parse(TripContentProvider.CONTENT_URI + "/" + tripId);
                    i.putExtra(TripContentProvider.CONTENT_ITEM_TYPE, tripUri);
                    startActivity(i);
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    public void deleteTrip(MenuItem item) {
        final TripDetailActivity that = this;
        new AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Do you really want to delete this Trip?")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Delete from database, show Toast, then return to Trip Overview
                    Toast.makeText(that, "Removing Trip...", Toast.LENGTH_SHORT).show();
                    long tripId = Long.parseLong(tripUri.getLastPathSegment());
                    getContentResolver().delete(TripContentProvider.CONTENT_URI, "_id=" + tripId, null);
                    Intent i = new Intent(that, TripOverviewActivity.class);
                    startActivity(i);
                }
            })
            .setNegativeButton(android.R.string.no, null).show();
    }

}
