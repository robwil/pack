package me.robwilliams.pack;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import me.robwilliams.pack.data.DatabaseHelper;
import me.robwilliams.pack.data.ListContentProvider;
import me.robwilliams.pack.data.ListsetListContentProvider;
import me.robwilliams.pack.data.SetContentProvider;


public class SetDetailActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private EditText mName;
    private EditText mWeight;
    private TextView mListSetListsTitleView;
    private ListView mListSetListsView;
    private TextView mEmptyListSetListsView;

    private Uri setUri;
    private int setId;

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_detail);

        mName = (EditText) findViewById(R.id.name);
        mWeight = (EditText) findViewById(R.id.weight);
        mListSetListsTitleView = (TextView) findViewById(R.id.listset_lists_title);
        mListSetListsView = (ListView) findViewById(R.id.listset_lists);
        mEmptyListSetListsView = (TextView) findViewById(R.id.empty_listset_list);

        Bundle extras = getIntent().getExtras();

        // check from the saved Instance
        setUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
                .getParcelable(SetContentProvider.CONTENT_ITEM_TYPE);

        // Or passed from the other activity
        if (extras != null) {
            setUri = extras.getParcelable(SetContentProvider.CONTENT_ITEM_TYPE);
            fillData(setUri);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putParcelable(SetContentProvider.CONTENT_ITEM_TYPE, setUri);
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

        if (setUri == null) {
            // New set
            setUri = getContentResolver().insert(SetContentProvider.CONTENT_URI, values);
            // @hack: convert content resolver's return value to the true content Uri.
            // So this will go from listsets/X to content://.../listsets/X
            // I have no idea why it doesn't return the value it then expects with query(...)
            setUri = Uri.parse(SetContentProvider.CONTENT_URI.toString() + "/" + setUri.getPathSegments().get(1));
        } else {
            // Update list item
            getContentResolver().update(setUri, values, null, null);
        }
    }

    private void fillData(Uri uri) {
        if (setId != 0) {
            return; // Must already be in Edit Mode, so don't do all these things
        }
        String[] projection = {"_id", "name", "weight"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String setName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            mName.setText(setName);
            mWeight.setText(cursor.getString(cursor.getColumnIndexOrThrow("weight")));
            setId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            cursor.close();

            // Edit mode, so show delete button
            // Since it's Edit mode, modify Title and setup ListView of Lists/Set mapping
            setTitle("Editing Set: " + mName.getText());
            mListSetListsTitleView.setVisibility(View.VISIBLE);
            mListSetListsView.setVisibility(View.VISIBLE);
            mEmptyListSetListsView.setVisibility(View.VISIBLE);
            findViewById(R.id.delete).setVisibility(View.VISIBLE);

            getLoaderManager().initLoader(0, null, this).forceLoad();
            adapter = new SimpleCursorAdapter(this,
                    android.R.layout.simple_list_item_multiple_choice,
                    null, // null cursor to start with, because it will get swapped in when load finished
                    new String[] { "name", "checked" },
                    new int[] { android.R.id.text1, android.R.id.text1 },
                    0);

            mListSetListsView.setAdapter(adapter);
            mListSetListsView.setEmptyView(mEmptyListSetListsView);

            // Must override ViewBinder to map the "name" and "checked" fields
            // appropriately to the CheckedTextView
            adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                    if (view.getId() != android.R.id.text1) {
                        return false;
                    }
                    if (columnIndex == cursor.getColumnIndex("name")) {
                        CheckedTextView textView = (CheckedTextView) view.findViewById(android.R.id.text1);
                        textView.setText(cursor.getString(columnIndex));
                    } else if (columnIndex == cursor.getColumnIndex("checked")) {
                        mListSetListsView.setItemChecked(cursor.getPosition(), !cursor.isNull(columnIndex));
                    }
                    return true;
                }
            });

            mListSetListsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ContentValues values = new ContentValues();
                    values.put("listset_id", setId);
                    values.put("list_id", id);

                    // If item is unchecked, delete entry from join table
                    if (!mListSetListsView.isItemChecked(position)) {
                        getContentResolver().delete(ListsetListContentProvider.CONTENT_URI,
                                "listset_id=" + setId + " AND list_id=" + id, null);
                    } else { // Otherwise, add to join table
                        getContentResolver().insert(ListsetListContentProvider.CONTENT_URI, values);
                    }
                }
            });
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This query will return all List names, and a "checked" property which will be either 1 or 0
        // if the join table contains an entry for the current Set with the given List
        final SetDetailActivity that = this;
        return new AsyncTaskLoader<Cursor>(this) {
            @Override
            public Cursor loadInBackground() {
                SQLiteDatabase db = new DatabaseHelper(that).getReadableDatabase();
                return db.rawQuery("SELECT L._id AS _id, L.name AS name, LSL.listset_id AS checked " +
                                   "FROM list L LEFT JOIN listset_list LSL ON L._id=LSL.list_id AND LSL.listset_id=" + setId, null);
            }
        };
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

    public void saveListSet(View view) {
        if (TextUtils.isEmpty(mName.getText().toString())) {
            Toast.makeText(SetDetailActivity.this, "Name is required",
                    Toast.LENGTH_LONG).show();
        } else {
            // Save the set data then transition to Edit Mode via fillData(...)
            saveState();
            Toast.makeText(SetDetailActivity.this, (setId == 0) ? "Successfully created List Set" : "Successfully saved List Set",
                    Toast.LENGTH_LONG).show();
            fillData(setUri); // setUri is populated in saveSave()
        }
    }

    public void deleteListSet(View view) {
        mName.setText("");
        mWeight.setText("");
        getContentResolver().delete(setUri, null, null);
        Toast.makeText(SetDetailActivity.this, "Deleted list set",
                Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }
}
