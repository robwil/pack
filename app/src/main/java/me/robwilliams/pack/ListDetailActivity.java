package me.robwilliams.pack;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import me.robwilliams.pack.data.ItemContentProvider;
import me.robwilliams.pack.data.ListContentProvider;


public class ListDetailActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private EditText mName;
    private EditText mWeight;
    private TextView mListItemsTitleView;
    private ListView mListItemsView;
    private TextView mEmptyItemsView;

    private Uri listUri;
    private int listId;

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        mName = (EditText) findViewById(R.id.name);
        mWeight = (EditText) findViewById(R.id.weight);
        mListItemsTitleView = (TextView) findViewById(R.id.list_items_title);
        mListItemsView = (ListView) findViewById(R.id.list_items);
        mEmptyItemsView = (TextView) findViewById(R.id.empty_list_items);

        Bundle extras = getIntent().getExtras();

        // check from the saved Instance
        listUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
                .getParcelable(ListContentProvider.CONTENT_ITEM_TYPE);

        // Or passed from the other activity
        if (extras != null) {
            listUri = extras
                    .getParcelable(ListContentProvider.CONTENT_ITEM_TYPE);

            fillData(listUri);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putParcelable(ListContentProvider.CONTENT_ITEM_TYPE, listUri);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    private void saveState() {
        String name = mName.getText().toString();
        String weight = mWeight.getText().toString();
        if (name.length() == 0 && weight.length() == 0) {
            return;
        }

        int weightNumber = 1;
        try {
            weightNumber = Integer.parseInt(weight);
        } catch (NumberFormatException ex) {}

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("weight", weightNumber);

        if (listUri == null) {
            // New list
            listUri = getContentResolver().insert(ListContentProvider.CONTENT_URI, values);
            // @hack: convert content resolver's return value to the true content Uri.
            // So this will go from lists/X to content://.../lists/X
            // I have no idea why it doesn't return the value it then expects with query(...)
            listUri = Uri.parse(ListContentProvider.CONTENT_URI.toString() + "/" + listUri.getPathSegments().get(1));
        } else {
            // Update list
            getContentResolver().update(listUri, values, null, null);
        }
    }

    private void fillData(Uri uri) {
        if (listId != 0) {
            return; // Must already be in Edit Mode, so don't do all these things
        }
        String[] projection = { "_id", "name", "weight" };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String listName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            mName.setText(listName);
            mWeight.setText(cursor.getString(cursor.getColumnIndexOrThrow("weight")));
            listId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            cursor.close();

            // Since it's Edit mode, modify Title and setup ListView of List Items
            setTitle("Editing List: " + mName.getText());
            mListItemsTitleView.setVisibility(View.VISIBLE);
            mListItemsView.setVisibility(View.VISIBLE);
            mEmptyItemsView.setVisibility(View.VISIBLE);
            findViewById(R.id.delete).setVisibility(View.VISIBLE);
            findViewById(R.id.add_list_item).setVisibility(View.VISIBLE);

            getLoaderManager().initLoader(0, null, this);
            adapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_1,
                    null, // null cursor to start with, because it will get swapped in when load finished
                    new String[] { "name" },
                    new int[] { android.R.id.text1 },
                    0);

            mListItemsView.setAdapter(adapter);
            mListItemsView.setEmptyView(mEmptyItemsView);

            final ListDetailActivity that = this;
            mListItemsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i = new Intent(that, ListItemDetailActivity.class);
                    Uri listItemUri = Uri.parse(ItemContentProvider.CONTENT_URI + "/" + id);
                    i.putExtra(ItemContentProvider.CONTENT_ITEM_TYPE, listItemUri);
                    i.putExtra(ListContentProvider.CONTENT_ID_TYPE, listId);
                    startActivity(i);
                }
            });
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { "_id", "name" };
        return new CursorLoader(this,
                ItemContentProvider.CONTENT_URI, projection, "list_id=" + listId, null, "weight DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    //
    // Event handlers
    //

    public void saveList(View view) {
        if (TextUtils.isEmpty(mName.getText().toString())) {
            Toast.makeText(ListDetailActivity.this, "Name is required",
                    Toast.LENGTH_LONG).show();
        } else {
            // Save the list data then transition to Edit Mode via fillData(...)
            saveState();
            Toast.makeText(ListDetailActivity.this, (listId == 0) ? "Successfully created list" : "Successfully saved list",
                    Toast.LENGTH_LONG).show();
            fillData(listUri); // listUri is populated in saveSave()
        }
    }

    public void deleteList(View view) {
        mName.setText("");
        mWeight.setText("");
        getContentResolver().delete(listUri, null, null);
        Toast.makeText(ListDetailActivity.this, "Deleted list",
                Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }

    public void addListItem(View view) {
        Intent intent = new Intent(this, ListItemDetailActivity.class);
        intent.putExtra(ListContentProvider.CONTENT_ID_TYPE, listId);
        startActivity(intent);
    }
}
