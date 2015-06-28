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
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.Set;

import me.robwilliams.pack.data.ItemContentProvider;
import me.robwilliams.pack.data.ListContentProvider;
import me.robwilliams.pack.data.SetContentProvider;


public class SetDetailActivity extends ActionBarActivity {

    private EditText mName;
    private EditText mWeight;
    private Uri setUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_detail);

        mName = (EditText) findViewById(R.id.name);
        mWeight = (EditText) findViewById(R.id.weight);

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
            // New list item
            setUri = getContentResolver().insert(SetContentProvider.CONTENT_URI, values);
        } else {
            // Update list item
            getContentResolver().update(setUri, values, null, null);
        }
    }

    private void fillData(Uri uri) {
        String[] projection = {"_id", "name", "weight"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String setName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            mName.setText(setName);
            mWeight.setText(cursor.getString(cursor.getColumnIndexOrThrow("weight")));
            cursor.close();

            // Edit mode, so show delete button
            findViewById(R.id.delete).setVisibility(View.VISIBLE);
        }
    }

    public void saveListSet(View view) {
        if (TextUtils.isEmpty(mName.getText().toString())) {
            Toast.makeText(SetDetailActivity.this, "Name is required",
                    Toast.LENGTH_LONG).show();
        } else {
            setResult(RESULT_OK);
            finish();
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

    // TODO: Need to find some way to execute below arbitrary query and then add to ScrollView
    // SELECT L.name AS name, CASE LSL._id WHEN LSL._id IS NOT NULL THEN 1 ELSE 0 END
    // FROM list L LEFT JOIN listset_list LSL ON L._id=LSL.list_id AND LSL.listset_id=1
}
