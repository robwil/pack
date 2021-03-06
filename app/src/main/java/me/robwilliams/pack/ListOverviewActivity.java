package me.robwilliams.pack;

import android.app.LoaderManager;
import android.content.CursorLoader;
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
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import me.robwilliams.pack.data.ListContentProvider;

public class ListOverviewActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_overview);
        fillData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_overview, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { "_id", "name" };
        return new CursorLoader(this,
                ListContentProvider.CONTENT_URI, projection, null, null, "weight DESC");
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

        ListView listView = (ListView) findViewById(R.id.lists);
        listView.setAdapter(adapter);
        listView.setEmptyView(findViewById(R.id.empty_lists));

        final ListOverviewActivity that = this;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(that, ListDetailActivity.class);
                Uri listUri = Uri.parse(ListContentProvider.CONTENT_URI + "/" + id);
                i.putExtra(ListContentProvider.CONTENT_ITEM_TYPE, listUri);
                startActivity(i);
            }
        });
    }

    //
    // User-defined event handlers
    //

    public void showListDetailActivity(MenuItem item) {
        Intent intent = new Intent(this, ListDetailActivity.class);
        startActivity(intent);
    }
}
