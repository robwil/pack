package me.robwilliams.pack;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import me.robwilliams.pack.data.SetContentProvider;

public class SetOverviewActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_overview);
        fillData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_set_overview, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { "_id", "name" };
        return new CursorLoader(this,
                SetContentProvider.CONTENT_URI, projection, null, null, "weight DESC");
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

        ListView listView = (ListView) findViewById(R.id.sets);
        listView.setAdapter(adapter);
        listView.setEmptyView(findViewById(R.id.empty_sets));

        final SetOverviewActivity that = this;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(that, SetDetailActivity.class);
                Uri setUri = Uri.parse(SetContentProvider.CONTENT_URI + "/" + id);
                i.putExtra(SetContentProvider.CONTENT_ITEM_TYPE, setUri);
                startActivity(i);
            }
        });
    }

    //
    // User-defined event handlers
    //

    public void showSetDetailActivity(MenuItem item) {
        Intent intent = new Intent(this, SetDetailActivity.class);
        startActivity(intent);
    }


}
