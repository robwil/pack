package me.robwilliams.pack;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import me.robwilliams.pack.data.SetContentProvider;
import me.robwilliams.pack.data.TripContentProvider;
import me.robwilliams.pack.data.TripSetContentProvider;

public class TripOverviewActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor>{

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_overview);
        fillData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_trip_overview, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { "_id", "name" };
        return new CursorLoader(this,
                TripContentProvider.CONTENT_URI, projection, null, null, "timestamp DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private void fillData() {
        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null, // null cursor to start with, because it will get swapped in when load finished
                new String[] { "name" },
                new int[] { android.R.id.text1 },
                0);

        ListView listView = (ListView) findViewById(R.id.trips);
        listView.setAdapter(adapter);
        listView.setEmptyView(findViewById(R.id.empty_trips));

        final TripOverviewActivity that = this;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(that, TripDetailActivity.class);
                Uri tripUri = Uri.parse(TripContentProvider.CONTENT_URI + "/" + id);
                i.putExtra(TripContentProvider.CONTENT_ITEM_TYPE, tripUri);
                startActivity(i);
            }
        });
    }

    //
    // User-defined event handlers
    //

    public void showAddTripDialog(MenuItem item) {
        // Construct dynamic dialog to reference available Sets, as well as Name the Trip
        Cursor listSets = getContentResolver().query(SetContentProvider.CONTENT_URI, new String[]{"_id", "name"}, null, null, "weight DESC");
        CursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                listSets,
                new String[] { "name" },
                new int[] { android.R.id.text1 },
                0);
        final LinearLayout layout = new LinearLayout(this);
        int dpPadding = 16;  // 16 dps
        final float scale = getResources().getDisplayMetrics().density;
        int pxPadding = (int) (dpPadding * scale + 0.5f);
        layout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText txtTripName = new EditText(this);
        final Spinner spnSet = new Spinner(this);
        spnSet.setAdapter(adapter);
        layout.addView(txtTripName);
        layout.addView(spnSet);

        new AlertDialog.Builder(this)
            .setTitle("Add Trip")
            .setMessage("Select a name and List Set for your Trip.")
            .setView(layout)
            .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // First, create the Trip with the given name.
                    String tripName = txtTripName.getText().toString();
                    ContentValues values = new ContentValues();
                    values.put("name", tripName);
                    Uri tripUri = getContentResolver().insert(TripContentProvider.CONTENT_URI, values);
                    // Next setup the join table relationship between Trip and selected Set
                    long tripId = Long.parseLong(tripUri.getLastPathSegment());
                    long setId = spnSet.getSelectedItemId();
                    values = new ContentValues();
                    values.put("trip_id", tripId);
                    values.put("listset_id", setId);
                    getContentResolver().insert(TripSetContentProvider.CONTENT_URI, values);
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }
}
