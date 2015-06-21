package me.robwilliams.pack;

import android.app.LoaderManager;
import android.content.ContentValues;
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

import me.robwilliams.pack.data.ListsContentProvider;


public class ListDetailActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private EditText mName;
    private EditText mWeight;
    private TextView mListItemsTitleView;
    private ListView mListItemsView;
    private TextView mEmptyItemsView;

    private Uri listUri;

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
                .getParcelable(ListsContentProvider.CONTENT_ITEM_TYPE);

        // Or passed from the other activity
        if (extras != null) {
            listUri = extras
                    .getParcelable(ListsContentProvider.CONTENT_ITEM_TYPE);

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
        outState.putParcelable(ListsContentProvider.CONTENT_ITEM_TYPE, listUri);
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
            listUri = getContentResolver().insert(ListsContentProvider.CONTENT_URI, values);
        } else {
            // Update list
            getContentResolver().update(listUri, values, null, null);
        }
    }

    private void fillData(Uri uri) {
        String[] projection = { "name", "weight" };
        Cursor cursor = getContentResolver().query(uri, projection, null, null,
                null);
        if (cursor != null) {
            cursor.moveToFirst();
            String listName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            mName.setText(listName);
            mWeight.setText(cursor.getString(cursor.getColumnIndexOrThrow("weight")));
            cursor.close();

            // Since it's Edit mode, modify Title and setup ListView of List Items
            setTitle("Editing List: " + listName);
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
//                    Intent i = new Intent(that, ListDetailActivity.class);
//                    Uri listUri = Uri.parse(ListsContentProvider.CONTENT_URI + "/" + id);
//                    i.putExtra(ListsContentProvider.CONTENT_ITEM_TYPE, listUri);
//                    startActivity(i);
                }
            });
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    //
    // Event handlers
    //

    public void saveList(View view) {
        if (TextUtils.isEmpty(mName.getText().toString())) {
            Toast.makeText(ListDetailActivity.this, "Name is required",
                    Toast.LENGTH_LONG).show();
        } else {
            // TODO: Save should not finish() but should reload Activity in Edit mode
            setResult(RESULT_OK);
            finish();
        }
    }

    public void deleteList(View view) {
        mName.setText("");
        mWeight.setText("");
        getContentResolver().delete(listUri, null, null);
        setResult(RESULT_OK);
        finish();
    }

    public void addListItem(View view) {
        Intent intent = new Intent(this, ListItemDetailActivity.class);
        startActivity(intent);
    }
}
