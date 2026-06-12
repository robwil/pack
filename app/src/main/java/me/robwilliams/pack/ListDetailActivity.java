package me.robwilliams.pack;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.robwilliams.pack.data.Bag;
import me.robwilliams.pack.data.BagContentProvider;
import me.robwilliams.pack.data.DatabaseHelper;
import me.robwilliams.pack.data.ItemContentProvider;
import me.robwilliams.pack.data.ListContentProvider;


public class ListDetailActivity extends AppCompatActivity
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            findViewById(R.id.set_default_bag).setVisibility(View.VISIBLE);
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

    public void showSetDefaultBagDialog(View view) {
        List<Bag> bags = new ArrayList<>();
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

        if (bags.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No Bags")
                    .setMessage("No bags have been created yet. Create bags from the main screen first.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        int dp8 = (int) (8 * density);
        int dp12 = (int) (12 * density);
        int dp16 = (int) (16 * density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp16, dp16, dp8);

        TextView description = new TextView(this);
        description.setText("Select a default bag for this list. All items without an existing bag assignment will be updated. Items that already have a different bag will not be changed.\n\nNew items added to this list will also use this bag.");
        description.setTextSize(14);
        description.setPadding(0, 0, 0, dp16);
        layout.addView(description);

        Button suggestButton = new Button(this);
        suggestButton.setText("Suggest from Recent Trips");
        LinearLayout.LayoutParams suggestParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        suggestParams.setMargins(0, 0, 0, dp16);
        suggestButton.setLayoutParams(suggestParams);
        layout.addView(suggestButton);

        final int[] selectedIndex = {-1};

        LinearLayout bagList = new LinearLayout(this);
        bagList.setOrientation(LinearLayout.VERTICAL);

        final List<View> dotViews = new ArrayList<>();
        for (int i = 0; i < bags.size(); i++) {
            final int index = i;
            Bag bag = bags.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp8, dp12, dp8, dp12);

            View dot = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp12 * 2, dp12 * 2);
            dotParams.setMargins(0, 0, dp12, 0);
            dot.setLayoutParams(dotParams);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            try {
                circle.setColor(Color.parseColor(bag.getColor()));
            } catch (Exception e) {
                circle.setColor(Color.GRAY);
            }
            dot.setBackground(circle);
            dotViews.add(dot);

            TextView nameView = new TextView(this);
            nameView.setText(bag.getName());
            nameView.setTextSize(16);

            row.addView(dot);
            row.addView(nameView);

            row.setOnClickListener(v -> {
                selectedIndex[0] = index;
                for (int j = 0; j < dotViews.size(); j++) {
                    View d = dotViews.get(j);
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.OVAL);
                    try {
                        bg.setColor(Color.parseColor(bags.get(j).getColor()));
                    } catch (Exception e) {
                        bg.setColor(Color.GRAY);
                    }
                    if (j == index) {
                        float dens = getResources().getDisplayMetrics().density;
                        bg.setStroke((int)(3 * dens), Color.BLACK);
                    }
                    d.setBackground(bg);
                }
            });

            bagList.addView(row);
        }

        layout.addView(bagList);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);

        final List<Bag> finalBags = bags;
        final AlertDialog parentDialog = new AlertDialog.Builder(this)
                .setTitle("Set Default Bag")
                .setView(scrollView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    if (selectedIndex[0] < 0 || selectedIndex[0] >= finalBags.size()) return;
                    Bag selectedBag = finalBags.get(selectedIndex[0]);
                    applyDefaultBag(selectedBag);
                })
                .setNegativeButton("Cancel", null)
                .create();
        suggestButton.setOnClickListener(v -> showSuggestFromRecentTripsDialog(parentDialog));
        parentDialog.show();
    }

    private void applyDefaultBag(Bag bag) {
        ContentValues listValues = new ContentValues();
        listValues.put("default_bag_id", bag.getId());
        getContentResolver().update(listUri, listValues, null, null);

        ContentValues itemValues = new ContentValues();
        itemValues.put("bag_hint_id", bag.getId());
        int updated = getContentResolver().update(ItemContentProvider.CONTENT_URI, itemValues,
                "list_id=" + listId + " AND bag_hint_id IS NULL", null);

        Toast.makeText(this, "Default bag set to \"" + bag.getName() + "\". " +
                updated + " item(s) updated.", Toast.LENGTH_LONG).show();
    }

    private void showSuggestFromRecentTripsDialog(final AlertDialog parentDialog) {
        android.database.sqlite.SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();

        // Find the most recent trip that contains items from this list
        Cursor tripCursor = db.rawQuery(
                "SELECT T._id, T.name FROM trip T " +
                "INNER JOIN trip_listset TLS ON T._id = TLS.trip_id " +
                "INNER JOIN listset_list LSL ON LSL.listset_id = TLS.listset_id " +
                "WHERE LSL.list_id = " + listId + " " +
                "ORDER BY T.timestamp DESC LIMIT 1", null);
        if (tripCursor == null || !tripCursor.moveToFirst()) {
            if (tripCursor != null) tripCursor.close();
            Toast.makeText(this, "No trips found using this list", Toast.LENGTH_SHORT).show();
            return;
        }
        int lastTripId = tripCursor.getInt(0);
        String lastTripName = tripCursor.getString(1);
        tripCursor.close();

        // Get bag assignments for items in this list from that trip
        Cursor itemCursor = db.rawQuery(
                "SELECT I._id as item_id, I.name as item_name, I.bag_hint_id, " +
                "TI.bag_id, B.name as bag_name, B.color as bag_color " +
                "FROM item I " +
                "INNER JOIN trip_item TI ON TI.item_id = I._id AND TI.trip_id = " + lastTripId + " " +
                "INNER JOIN bag B ON B._id = TI.bag_id " +
                "WHERE I.list_id = " + listId + " AND TI.bag_id IS NOT NULL " +
                "ORDER BY I.name ASC", null);

        if (itemCursor == null || !itemCursor.moveToFirst()) {
            if (itemCursor != null) itemCursor.close();
            Toast.makeText(this, "No bag assignments found in the last trip", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect suggestions: only items where the suggestion differs from current bag_hint_id
        List<int[]> suggestions = new ArrayList<>(); // [itemId, bagId, bagHintId]
        List<String[]> suggestionLabels = new ArrayList<>(); // [itemName, bagName, bagColor]
        do {
            int itemId = itemCursor.getInt(itemCursor.getColumnIndexOrThrow("item_id"));
            String itemName = itemCursor.getString(itemCursor.getColumnIndexOrThrow("item_name"));
            int bagHintId = itemCursor.isNull(itemCursor.getColumnIndexOrThrow("bag_hint_id")) ? 0 :
                    itemCursor.getInt(itemCursor.getColumnIndexOrThrow("bag_hint_id"));
            int bagId = itemCursor.getInt(itemCursor.getColumnIndexOrThrow("bag_id"));
            String bagName = itemCursor.getString(itemCursor.getColumnIndexOrThrow("bag_name"));
            String bagColor = itemCursor.getString(itemCursor.getColumnIndexOrThrow("bag_color"));

            if (bagId != bagHintId) {
                suggestions.add(new int[]{itemId, bagId, bagHintId});
                suggestionLabels.add(new String[]{itemName, bagName, bagColor});
            }
        } while (itemCursor.moveToNext());
        itemCursor.close();

        if (suggestions.isEmpty()) {
            Toast.makeText(this, "All items already match their last trip bags", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the checklist dialog
        float density = getResources().getDisplayMetrics().density;
        int dp8 = (int) (8 * density);
        int dp12 = (int) (12 * density);
        int dp16 = (int) (16 * density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp16, dp16, dp8);

        TextView header = new TextView(this);
        header.setText("Based on trip \"" + lastTripName + "\":");
        header.setTextSize(14);
        header.setPadding(0, 0, 0, dp12);
        layout.addView(header);

        final List<CheckBox> checkBoxes = new ArrayList<>();
        for (int i = 0; i < suggestions.size(); i++) {
            String[] labels = suggestionLabels.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp8, 0, dp8);

            CheckBox checkBox = new CheckBox(this);
            checkBox.setChecked(true);
            checkBoxes.add(checkBox);

            View dot = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    (int) (12 * density), (int) (12 * density));
            dotParams.setMargins(0, 0, dp8, 0);
            dot.setLayoutParams(dotParams);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            try {
                circle.setColor(Color.parseColor(labels[2]));
            } catch (Exception e) {
                circle.setColor(Color.GRAY);
            }
            dot.setBackground(circle);

            TextView label = new TextView(this);
            label.setText(labels[0] + "  →  " + labels[1]);
            label.setTextSize(15);

            row.addView(checkBox);
            row.addView(dot);
            row.addView(label);
            layout.addView(row);
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);

        new AlertDialog.Builder(this)
                .setTitle("Suggest Default Bags")
                .setView(scrollView)
                .setPositiveButton("Apply Selected", (dialog, which) -> {
                    int applied = 0;
                    for (int i = 0; i < suggestions.size(); i++) {
                        if (!checkBoxes.get(i).isChecked()) continue;
                        int[] s = suggestions.get(i);
                        ContentValues values = new ContentValues();
                        values.put("bag_hint_id", s[1]);
                        getContentResolver().update(ItemContentProvider.CONTENT_URI, values,
                                "_id=" + s[0], null);
                        applied++;
                    }
                    Toast.makeText(this, applied + " item(s) updated.",
                            Toast.LENGTH_SHORT).show();
                    parentDialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
