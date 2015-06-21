package me.robwilliams.pack;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import me.robwilliams.pack.data.ListsContentProvider;


public class ListDetailActivity extends ActionBarActivity {

    private EditText mName;
    private EditText mWeight;

    private Uri listUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_detail);

        mName = (EditText) findViewById(R.id.name);
        mWeight = (EditText) findViewById(R.id.weight);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            setTitle("Editing List: " + listName);
            cursor.close();
        }
    }

    //
    // Event handlers
    //

    public void saveList(View view) {
        if (TextUtils.isEmpty(mName.getText().toString())) {
            Toast.makeText(ListDetailActivity.this, "Name is required",
                    Toast.LENGTH_LONG).show();
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }
}
