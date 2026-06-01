package me.robwilliams.pack;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.robwilliams.pack.data.Bag;
import me.robwilliams.pack.data.BagContentProvider;
import me.robwilliams.pack.data.ItemContentProvider;
import me.robwilliams.pack.data.ListContentProvider;


public class ListItemDetailActivity extends AppCompatActivity {

    private EditText mName;
    private EditText mWeight;
    private Spinner mBagHintSpinner;
    private Uri listItemUri;
    private int listId;
    private List<Bag> bags;
    private int currentBagHintId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_item_detail);

        mName = (EditText) findViewById(R.id.name);
        mWeight = (EditText) findViewById(R.id.weight);
        mBagHintSpinner = (Spinner) findViewById(R.id.bag_hint_spinner);

        loadBags();

        Bundle extras = getIntent().getExtras();

        listItemUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
                .getParcelable(ItemContentProvider.CONTENT_ITEM_TYPE);

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

    private void loadBags() {
        bags = new ArrayList<>();
        Cursor cursor = getContentResolver().query(BagContentProvider.CONTENT_URI,
                new String[]{"_id", "name", "color"}, null, null, "name ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                bags.add(new Bag(
                        cursor.getInt(cursor.getColumnIndexOrThrow("_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("color"))
                ));
            }
            cursor.close();
        }

        List<String> spinnerItems = new ArrayList<>();
        spinnerItems.add("None");
        for (Bag bag : bags) {
            spinnerItems.add(bag.getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBagHintSpinner.setAdapter(spinnerAdapter);
    }

    private void selectBagInSpinner(int bagHintId) {
        if (bagHintId == 0) {
            mBagHintSpinner.setSelection(0);
            return;
        }
        for (int i = 0; i < bags.size(); i++) {
            if (bags.get(i).getId() == bagHintId) {
                mBagHintSpinner.setSelection(i + 1);
                return;
            }
        }
        mBagHintSpinner.setSelection(0);
    }

    private int getSelectedBagHintId() {
        int pos = mBagHintSpinner.getSelectedItemPosition();
        if (pos <= 0 || pos > bags.size()) return 0;
        return bags.get(pos - 1).getId();
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

        int bagHintId = getSelectedBagHintId();
        if (bagHintId > 0) {
            values.put("bag_hint_id", bagHintId);
        } else {
            values.putNull("bag_hint_id");
        }

        if (listItemUri == null) {
            listItemUri = getContentResolver().insert(ItemContentProvider.CONTENT_URI, values);
        } else {
            getContentResolver().update(listItemUri, values, null, null);
        }
    }

    private void fillData(Uri uri) {
        String[] projection = {"_id", "name", "weight", "bag_hint_id"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String listItemName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            mName.setText(listItemName);
            mWeight.setText(cursor.getString(cursor.getColumnIndexOrThrow("weight")));
            currentBagHintId = cursor.isNull(cursor.getColumnIndexOrThrow("bag_hint_id")) ? 0 :
                    cursor.getInt(cursor.getColumnIndexOrThrow("bag_hint_id"));
            cursor.close();

            selectBagInSpinner(currentBagHintId);

            setTitle("Editing Item: " + mName.getText());
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
