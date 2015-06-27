package me.robwilliams.pack;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import me.robwilliams.pack.data.ItemContentProvider;
import me.robwilliams.pack.data.ListContentProvider;


public class ListItemDetailActivity extends ActionBarActivity {

    private EditText mName;
    private EditText mWeight;
    private Uri listItemUri;
    private int listId; // the ID for the List where this List Item should be added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_item_detail);

        mName = (EditText) findViewById(R.id.name);
        mWeight = (EditText) findViewById(R.id.weight);

        Bundle extras = getIntent().getExtras();

        // check from the saved Instance
        listItemUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
                .getParcelable(ItemContentProvider.CONTENT_ITEM_TYPE);

        // Or passed from the other activity
        if (extras != null) {
            listItemUri = extras.getParcelable(ItemContentProvider.CONTENT_ITEM_TYPE);
            listId = extras.getInt(ListContentProvider.CONTENT_ID_TYPE);

            if (listItemUri != null) {
                fillData(listItemUri);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putParcelable(ItemContentProvider.CONTENT_ITEM_TYPE, listItemUri);
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
        values.put("list_id", listId);

        if (listItemUri == null) {
            // New list item
            listItemUri = getContentResolver().insert(ItemContentProvider.CONTENT_URI, values);
        } else {
            // Update list item
            getContentResolver().update(listItemUri, values, null, null);
        }
    }

    private void fillData(Uri uri) {
        String[] projection = {"_id", "name", "weight"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String listItemName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            mName.setText(listItemName);
            mWeight.setText(cursor.getString(cursor.getColumnIndexOrThrow("weight")));
            cursor.close();

            // Edit mode, so show delete button
            findViewById(R.id.delete).setVisibility(View.VISIBLE);
        }
    }

    public void saveListItem(View view) {
        if (TextUtils.isEmpty(mName.getText().toString())) {
            Toast.makeText(ListItemDetailActivity.this, "Name is required",
                    Toast.LENGTH_LONG).show();
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    public void deleteListItem(View view) {
        mName.setText("");
        mWeight.setText("");
        getContentResolver().delete(listItemUri, null, null);
        Toast.makeText(ListItemDetailActivity.this, "Deleted list item",
                Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
        finish();
    }
}
