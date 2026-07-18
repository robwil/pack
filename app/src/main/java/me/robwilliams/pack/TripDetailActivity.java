package me.robwilliams.pack;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import me.robwilliams.pack.adapter.TripDetailPagerAdapter;
import me.robwilliams.pack.data.Bag;
import me.robwilliams.pack.data.BagContentProvider;
import me.robwilliams.pack.data.DatabaseHelper;
import me.robwilliams.pack.data.TripBagContentProvider;
import me.robwilliams.pack.data.TripContentProvider;
import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.data.TripItemContentProvider;
import me.robwilliams.pack.data.TripSetContentProvider;
import me.robwilliams.pack.fragment.AbstractPackingFragment;

public class TripDetailActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private TripDetailPagerAdapter mAdapter;
    private Uri tripUri;
    private int tripId;
    private ArrayList<TripItem> tripItems;
    private ArrayList<Bag> tripBags;
    private PagerSlidingTabStrip tabs;
    private MenuItem searchMenuItem;
    private View tripContent;
    private ListView searchResultsView;
    private View emptySearchView;
    private int pendingHighlightItemId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        tripItems = new ArrayList<>();
        tripContent = findViewById(R.id.trip_content);
        searchResultsView = (ListView) findViewById(R.id.trip_search_results);
        emptySearchView = findViewById(R.id.empty_trip_search_results);

        // Restore Trip details from saved instance or Intent
        Bundle extras = getIntent().getExtras();

        tripUri = (savedInstanceState == null) ? null : (Uri) savedInstanceState
                .getParcelable(TripContentProvider.CONTENT_ITEM_TYPE);

        if (extras != null) {
            tripUri = extras
                    .getParcelable(TripContentProvider.CONTENT_ITEM_TYPE);

            fillData(tripUri);
        }
    }

    public void setPendingHighlightItemId(int itemId) {
        this.pendingHighlightItemId = itemId;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
            searchMenuItem.collapseActionView();
        }
        if (tripUri != null && viewPager != null) {
            reloadTripData();
            dispatchPendingHighlight();
        }
    }

    private void dispatchPendingHighlight() {
        if (pendingHighlightItemId < 0 || viewPager == null || mAdapter == null) return;
        final int itemId = pendingHighlightItemId;
        pendingHighlightItemId = -1;
        viewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                Fragment f = mAdapter.getActiveFragment(viewPager.getCurrentItem());
                if (f instanceof AbstractPackingFragment) {
                    ((AbstractPackingFragment) f).scrollToAndHighlightItem(itemId);
                }
            }
        }, 400);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trip_detail, menu);

        searchMenuItem = menu.findItem(R.id.action_search_trip_items);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setQueryHint(getString(R.string.trip_search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performTripSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() == 0) {
                    showTripMode();
                } else {
                    performTripSearch(newText);
                }
                return true;
            }
        });

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                showTripMode();
                return true;
            }
        });

        return true;
    }

    private void performTripSearch(String query) {
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        final List<TripItem> matchedItems = new ArrayList<>();
        final Map<TripItem, Integer> originalIndex = new HashMap<>();

        for (int i = 0; i < tripItems.size(); i++) {
            TripItem item = tripItems.get(i);
            if (item.getItemName().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                matchedItems.add(item);
                originalIndex.put(item, i);
            }
        }

        Collections.sort(matchedItems, new Comparator<TripItem>() {
            @Override
            public int compare(TripItem a, TripItem b) {
                // Status descending (Repacked 3 > Packed 2 > To Pack 1 > Not Selected 0)
                int statusCmp = Integer.compare(b.getStatus(), a.getStatus());
                if (statusCmp != 0) return statusCmp;
                // Category alphabetically
                int catCmp = a.getListName().compareToIgnoreCase(b.getListName());
                if (catCmp != 0) return catCmp;
                // Original load order preserves weight within category
                return Integer.compare(originalIndex.get(a), originalIndex.get(b));
            }
        });

        final float density = getResources().getDisplayMetrics().density;

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() { return matchedItems.size(); }

            @Override
            public Object getItem(int position) { return matchedItems.get(position); }

            @Override
            public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TripItem item = matchedItems.get(position);

                LinearLayout layout;
                if (convertView instanceof LinearLayout) {
                    layout = (LinearLayout) convertView;
                    layout.removeAllViews();
                } else {
                    layout = new LinearLayout(TripDetailActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding((int)(12 * density), (int)(10 * density),
                            (int)(12 * density), (int)(10 * density));
                }

                // Item name
                TextView nameView = new TextView(TripDetailActivity.this);
                String displayName = item.getItemName();
                if (item.getQuantity() > 1) {
                    displayName += " x" + item.getQuantity();
                }
                nameView.setText(displayName);
                nameView.setTextSize(16);
                nameView.setTextColor(resolveThemeTextColor(android.R.attr.textColorPrimary));
                layout.addView(nameView);

                // Detail row: category + bag chip + bag name + status
                LinearLayout detailRow = new LinearLayout(TripDetailActivity.this);
                detailRow.setOrientation(LinearLayout.HORIZONTAL);
                detailRow.setGravity(Gravity.CENTER_VERTICAL);
                detailRow.setPadding(0, (int)(2 * density), 0, 0);

                TextView categoryView = new TextView(TripDetailActivity.this);
                categoryView.setText(item.getListName());
                categoryView.setTextSize(13);
                categoryView.setTextColor(resolveThemeTextColor(android.R.attr.textColorSecondary));
                detailRow.addView(categoryView);

                if (item.getBagName() != null && !item.getBagName().isEmpty()) {
                    TextView separator = new TextView(TripDetailActivity.this);
                    separator.setText(" · ");
                    separator.setTextSize(13);
                    separator.setTextColor(resolveThemeTextColor(android.R.attr.textColorSecondary));
                    detailRow.addView(separator);

                    if (item.getBagColor() != null) {
                        View dot = new View(TripDetailActivity.this);
                        int chipSize = (int)(10 * density);
                        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(chipSize, chipSize);
                        dotParams.gravity = Gravity.CENTER_VERTICAL;
                        dotParams.setMargins(0, 0, (int)(4 * density), 0);
                        dot.setLayoutParams(dotParams);
                        GradientDrawable circle = new GradientDrawable();
                        circle.setShape(GradientDrawable.OVAL);
                        try {
                            circle.setColor(Color.parseColor(item.getBagColor()));
                        } catch (Exception e) {
                            circle.setColor(Color.GRAY);
                        }
                        dot.setBackground(circle);
                        detailRow.addView(dot);
                    }

                    TextView bagView = new TextView(TripDetailActivity.this);
                    bagView.setText(item.getBagName());
                    bagView.setTextSize(13);
                    bagView.setTextColor(resolveThemeTextColor(android.R.attr.textColorSecondary));
                    detailRow.addView(bagView);
                }

                TextView statusView = new TextView(TripDetailActivity.this);
                statusView.setText(" · " + getStatusLabel(item.getStatus()));
                statusView.setTextSize(13);
                statusView.setTextColor(getStatusColor(item.getStatus()));
                detailRow.addView(statusView);

                layout.addView(detailRow);
                return layout;
            }
        };

        searchResultsView.setAdapter(adapter);
        searchResultsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < matchedItems.size()) {
                    TripItem item = matchedItems.get(position);
                    int targetPage = resolvePageForStatus(item.getStatus());
                    pendingHighlightItemId = item.getItemId();
                    searchMenuItem.collapseActionView();
                    viewPager.setCurrentItem(targetPage);
                    dispatchPendingHighlight();
                }
            }
        });

        showSearchMode(matchedItems.isEmpty());
    }

    private String getStatusLabel(int status) {
        if (status >= AbstractPackingFragment.STATUS_REPACKED) return "Repacked";
        if (status >= AbstractPackingFragment.STATUS_PACKED) return "Packed";
        if (status >= AbstractPackingFragment.STATUS_SHOULD_PACK) return "To pack";
        return "Not selected";
    }

    private int getStatusColor(int status) {
        if (status >= AbstractPackingFragment.STATUS_REPACKED) return 0xFF2E7D32;
        if (status >= AbstractPackingFragment.STATUS_PACKED) return 0xFF1565C0;
        if (status >= AbstractPackingFragment.STATUS_SHOULD_PACK) return 0xFFE65100;
        return Color.GRAY;
    }

    private int resolvePageForStatus(int status) {
        if (status >= AbstractPackingFragment.STATUS_REPACKED) return 2;
        if (status >= AbstractPackingFragment.STATUS_PACKED) return 1;
        return 0;
    }

    private void showSearchMode(boolean empty) {
        tripContent.setVisibility(View.GONE);
        if (empty) {
            searchResultsView.setVisibility(View.GONE);
            emptySearchView.setVisibility(View.VISIBLE);
        } else {
            searchResultsView.setVisibility(View.VISIBLE);
            emptySearchView.setVisibility(View.GONE);
        }
    }

    private void showTripMode() {
        searchResultsView.setVisibility(View.GONE);
        emptySearchView.setVisibility(View.GONE);
        tripContent.setVisibility(View.VISIBLE);
    }

    private void fillData(Uri uri) {
        String[] projection = { "_id", "name" };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String tripName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            tripId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            cursor.close();

            // Now that we've loaded the Trip, load Trip Items
            SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
            cursor = db.rawQuery("SELECT L.name as list_name, L.weight as list_weight, " +
                    "I._id as item_id, I.name as item_name, I.weight as item_weight, I.bag_hint_id, " +
                    "TI.status as status, COALESCE(TI.quantity, 1) as quantity, TI.bag_id as bag_id, " +
                    "B.name as bag_name, B.color as bag_color, " +
                    "LSL.listset_id as listset_id FROM trip T " +
                    "INNER JOIN trip_listset TLS ON T._id=TLS.trip_id AND TLS.trip_id=" + tripId + " " +
                    "INNER JOIN listset_list LSL ON LSL.listset_id=TLS.listset_id " +
                    "INNER JOIN list L ON L._id=LSL.list_id " +
                    "INNER JOIN item I ON I.list_id=LSL.list_id " +
                    "LEFT JOIN trip_item TI ON TI.trip_id=TLS.trip_id AND TI.item_id=I._id " +
                    "LEFT JOIN bag B ON B._id=TI.bag_id " +
                    "ORDER BY list_weight DESC, list_name ASC, item_weight DESC, item_name ASC", null);

            if (cursor != null) {
                // Loop through cursor and store items in a local repository
                int maximumStatus = 0;
                boolean allPacked = true;
                boolean hasActiveItems = false;
                while (cursor.moveToNext()) {
                    String listName = cursor.getString(cursor.getColumnIndexOrThrow("list_name"));
                    int itemId = cursor.getInt(cursor.getColumnIndexOrThrow("item_id"));
                    int listsetId = cursor.getInt(cursor.getColumnIndexOrThrow("listset_id"));
                    String itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                    int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                    int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                    int bagId = cursor.isNull(cursor.getColumnIndexOrThrow("bag_id")) ? 0 :
                            cursor.getInt(cursor.getColumnIndexOrThrow("bag_id"));
                    String bagName = cursor.getString(cursor.getColumnIndexOrThrow("bag_name"));
                    String bagColor = cursor.getString(cursor.getColumnIndexOrThrow("bag_color"));
                    int bagHintId = cursor.isNull(cursor.getColumnIndexOrThrow("bag_hint_id")) ? 0 :
                            cursor.getInt(cursor.getColumnIndexOrThrow("bag_hint_id"));
                    if (status > maximumStatus) {
                        maximumStatus = status;
                    }
                    if (status >= AbstractPackingFragment.STATUS_SHOULD_PACK) {
                        hasActiveItems = true;
                        if (status < AbstractPackingFragment.STATUS_PACKED) {
                            allPacked = false;
                        }
                    }
                    tripItems.add(new TripItem(itemId, listsetId, listName, itemName, status,
                            quantity, bagId, bagName, bagColor, bagHintId));
                }

                // Load trip bags
                tripBags = new ArrayList<>();
                Cursor bagCursor = db.rawQuery(
                        "SELECT B._id, B.name, B.color FROM trip_bag TB " +
                        "INNER JOIN bag B ON B._id = TB.bag_id " +
                        "WHERE TB.trip_id = " + tripId + " ORDER BY B.name ASC", null);
                if (bagCursor != null) {
                    while (bagCursor.moveToNext()) {
                        tripBags.add(new Bag(
                                bagCursor.getInt(0),
                                bagCursor.getString(1),
                                bagCursor.getString(2)));
                    }
                    bagCursor.close();
                }

                // And now with the Trip Items, finish setting up UI
                setTitle("Trip: " + tripName);
                viewPager = (ViewPager) findViewById(R.id.pager);
                mAdapter = new TripDetailPagerAdapter(getSupportFragmentManager(), tripId, tripItems, tripBags);
                viewPager.setAdapter(mAdapter);

                tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
                tabs.setViewPager(viewPager);
                tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

                    @Override
                    public void onPageSelected(int position) {
                        // This forced recreation of view fragment even if it was created previously.
                        // This ensures we always have fresh data.
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {}
                });

                viewPager.setCurrentItem(
                        BagSelectionLogic.resolveDefaultPage(maximumStatus, hasActiveItems, allPacked));
            }
        }
    }

    private void reloadTripData() {
        int currentPage = viewPager.getCurrentItem();
        tripItems.clear();

        SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT L.name as list_name, L.weight as list_weight, " +
                "I._id as item_id, I.name as item_name, I.weight as item_weight, I.bag_hint_id, " +
                "TI.status as status, COALESCE(TI.quantity, 1) as quantity, TI.bag_id as bag_id, " +
                "B.name as bag_name, B.color as bag_color, " +
                "LSL.listset_id as listset_id FROM trip T " +
                "INNER JOIN trip_listset TLS ON T._id=TLS.trip_id AND TLS.trip_id=" + tripId + " " +
                "INNER JOIN listset_list LSL ON LSL.listset_id=TLS.listset_id " +
                "INNER JOIN list L ON L._id=LSL.list_id " +
                "INNER JOIN item I ON I.list_id=LSL.list_id " +
                "LEFT JOIN trip_item TI ON TI.trip_id=TLS.trip_id AND TI.item_id=I._id " +
                "LEFT JOIN bag B ON B._id=TI.bag_id " +
                "ORDER BY list_weight DESC, list_name ASC, item_weight DESC, item_name ASC", null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String listName = cursor.getString(cursor.getColumnIndexOrThrow("list_name"));
                int itemId = cursor.getInt(cursor.getColumnIndexOrThrow("item_id"));
                int listsetId = cursor.getInt(cursor.getColumnIndexOrThrow("listset_id"));
                String itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                int bagId = cursor.isNull(cursor.getColumnIndexOrThrow("bag_id")) ? 0 :
                        cursor.getInt(cursor.getColumnIndexOrThrow("bag_id"));
                String bagName = cursor.getString(cursor.getColumnIndexOrThrow("bag_name"));
                String bagColor = cursor.getString(cursor.getColumnIndexOrThrow("bag_color"));
                int bagHintId = cursor.isNull(cursor.getColumnIndexOrThrow("bag_hint_id")) ? 0 :
                        cursor.getInt(cursor.getColumnIndexOrThrow("bag_hint_id"));
                tripItems.add(new TripItem(itemId, listsetId, listName, itemName, status,
                        quantity, bagId, bagName, bagColor, bagHintId));
            }
            cursor.close();

            tripBags = new ArrayList<>();
            Cursor bagCursor = db.rawQuery(
                    "SELECT B._id, B.name, B.color FROM trip_bag TB " +
                    "INNER JOIN bag B ON B._id = TB.bag_id " +
                    "WHERE TB.trip_id = " + tripId + " ORDER BY B.name ASC", null);
            if (bagCursor != null) {
                while (bagCursor.moveToNext()) {
                    tripBags.add(new Bag(
                            bagCursor.getInt(0),
                            bagCursor.getString(1),
                            bagCursor.getString(2)));
                }
                bagCursor.close();
            }

            mAdapter = new TripDetailPagerAdapter(getSupportFragmentManager(), tripId, tripItems, tripBags);
            viewPager.setAdapter(mAdapter);
            tabs.setViewPager(viewPager);
            viewPager.setCurrentItem(currentPage);
        }
    }

    public void showCopyTripDialog(MenuItem item) {
        // Construct dialog to Name the Trip
        final EditText txtTripName = new EditText(this);
        int dpPadding = 16;  // 16 dps
        final float scale = getResources().getDisplayMetrics().density;
        int pxPadding = (int) (dpPadding * scale + 0.5f);
        txtTripName.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);

        final TripDetailActivity that = this;
        new AlertDialog.Builder(this)
            .setTitle("Copy Trip")
            .setMessage("Select a name for the new trip.")
            .setView(txtTripName)
            .setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Toast.makeText(that, "Copying Trip...", Toast.LENGTH_SHORT).show();
                    // First, create the Trip with the given name.
                    String tripName = txtTripName.getText().toString();
                    ContentValues values = new ContentValues();
                    values.put("name", tripName);
                    Uri tripUri = getContentResolver().insert(TripContentProvider.CONTENT_URI, values);
                    long tripId = Long.parseLong(tripUri.getLastPathSegment());
                    // Iterate through existing Trip's items to figure out Sets and Items we want
                    Set<Integer> setIds = new HashSet<>();
                    for (TripItem tripItem : tripItems) {
                        setIds.add(tripItem.getListsetId());
                    }
                    // Next setup the join table relationship between new Trip and original Trip's sets
                    for (int listsetId : setIds) {
                        values = new ContentValues();
                        values.put("trip_id", tripId);
                        values.put("listset_id", listsetId);
                        getContentResolver().insert(TripSetContentProvider.CONTENT_URI, values);
                    }
                    // Finally setup the Trip Item join table with a status of SHOULD_PACK
                    for (TripItem ti : tripItems) {
                        if (ti.getStatus() >= AbstractPackingFragment.STATUS_SHOULD_PACK) {
                            values = new ContentValues();
                            values.put("trip_id", tripId);
                            values.put("item_id", ti.getItemId());
                            values.put("status", AbstractPackingFragment.STATUS_SHOULD_PACK);
                            values.put("quantity", ti.getQuantity());
                            getContentResolver().insert(TripItemContentProvider.CONTENT_URI, values);
                        }
                    }
                    // Copy trip bags
                    if (tripBags != null) {
                        for (Bag bag : tripBags) {
                            values = new ContentValues();
                            values.put("trip_id", tripId);
                            values.put("bag_id", bag.getId());
                            getContentResolver().insert(TripBagContentProvider.CONTENT_URI, values);
                        }
                    }
                    // Then transition to the new Trip
                    Intent i = new Intent(that, TripDetailActivity.class);
                    tripUri = Uri.parse(TripContentProvider.CONTENT_URI + "/" + tripId);
                    i.putExtra(TripContentProvider.CONTENT_ITEM_TYPE, tripUri);
                    startActivity(i);
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            })
            .show();
    }

    public void showTripBagSelection(MenuItem item) {
        TripBagSelectionDialogHelper.show(this, tripId,
                new TripBagSelectionDialogHelper.OnBagsChangedListener() {
                    @Override
                    public void onBagsChanged(List<Bag> activeBags) {
                        tripBags = new ArrayList<>(activeBags);
                        Set<Integer> activeBagIds = new HashSet<>();
                        for (Bag bag : activeBags) {
                            activeBagIds.add(bag.getId());
                        }
                        for (TripItem ti : tripItems) {
                            if (ti.getBagId() > 0 && !activeBagIds.contains(ti.getBagId())) {
                                ti.setBagId(0);
                                ti.setBagName(null);
                                ti.setBagColor(null);
                            }
                        }
                        mAdapter.updateTripBags(tripBags);
                    }
                });
    }

    public void clearRepackItems(MenuItem item) {
        int repackCount = 0;
        for (TripItem ti : tripItems) {
            if (ti.getStatus() >= AbstractPackingFragment.STATUS_REPACKED) {
                repackCount++;
            }
        }
        if (repackCount == 0) {
            Toast.makeText(this, "No repacked items to clear.", Toast.LENGTH_SHORT).show();
            return;
        }
        final int count = repackCount;
        new AlertDialog.Builder(this)
            .setTitle("Clear Repack")
            .setMessage("Move " + count + " repacked item(s) back to packed?")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    ContentValues values = new ContentValues();
                    values.put("status", AbstractPackingFragment.STATUS_PACKED);
                    getContentResolver().update(TripItemContentProvider.CONTENT_URI, values,
                            "trip_id=" + tripId + " AND status=" + AbstractPackingFragment.STATUS_REPACKED, null);
                    for (TripItem ti : tripItems) {
                        if (ti.getStatus() >= AbstractPackingFragment.STATUS_REPACKED) {
                            ti.setStatus(AbstractPackingFragment.STATUS_PACKED);
                        }
                    }
                    reloadTripData();
                    Toast.makeText(TripDetailActivity.this,
                            count + " item(s) moved back to packed.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(android.R.string.no, null).show();
    }

    public void deleteTrip(MenuItem item) {
        final TripDetailActivity that = this;
        new AlertDialog.Builder(this)
            .setTitle("Delete Trip")
            .setMessage("Do you really want to delete this Trip?")
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Delete from database, show Toast, then return to Trip Overview
                    Toast.makeText(that, "Removing Trip...", Toast.LENGTH_SHORT).show();
                    long tripId = Long.parseLong(tripUri.getLastPathSegment());
                    getContentResolver().delete(TripContentProvider.CONTENT_URI, "_id=" + tripId, null);
                    Intent i = new Intent(that, TripOverviewActivity.class);
                    startActivity(i);
                }
            })
            .setNegativeButton(android.R.string.no, null).show();
    }

    private int resolveThemeTextColor(int attr) {
        TypedArray a = obtainStyledAttributes(new int[]{attr});
        int color = a.getColor(0, Color.BLACK);
        a.recycle();
        return color;
    }
}
