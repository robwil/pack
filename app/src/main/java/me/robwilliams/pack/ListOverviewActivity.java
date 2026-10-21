package me.robwilliams.pack;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import me.robwilliams.pack.data.DatabaseHelper;
import me.robwilliams.pack.data.ListContentProvider;

public class ListOverviewActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private SimpleCursorAdapter adapter;
    private ListView listView;
    private ListView searchResultsView;
    private TextView emptyListsView;
    private View emptySearchView;
    private SimpleCursorAdapter searchAdapter;
    private DatabaseHelper dbHelper;
    private Cursor searchCursor;
    private MenuItem searchMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_overview);
        dbHelper = new DatabaseHelper(this);
        listView = (ListView) findViewById(R.id.lists);
        emptyListsView = (TextView) findViewById(R.id.empty_lists);
        searchResultsView = (ListView) findViewById(R.id.search_results);
        emptySearchView = findViewById(R.id.empty_search_results);
        fillData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list_overview, menu);

        searchMenuItem = menu.findItem(R.id.action_search_items);
        MenuItem searchItem = searchMenuItem;
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() == 0) {
                    showListMode();
                } else {
                    performSearch(newText);
                }
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                showListMode();
                return true;
            }
        });

        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { "_id", "name" };
        return new CursorLoader(this,
                ListContentProvider.CONTENT_URI, projection, null, null, "weight DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
            searchMenuItem.collapseActionView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchCursor != null) {
            searchCursor.close();
        }
    }

    private void fillData() {
        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                new String[] { "name" },
                new int[] { android.R.id.text1 },
                0);

        listView.setAdapter(adapter);
        listView.setEmptyView(emptyListsView);

        final ListOverviewActivity that = this;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(that, ListDetailActivity.class);
                Uri listUri = Uri.parse(ListContentProvider.CONTENT_URI + "/" + id);
                i.putExtra(ListContentProvider.CONTENT_ITEM_TYPE, listUri);
                startActivity(i);
            }
        });
    }

    private void performSearch(String query) {
        if (searchCursor != null) {
            searchCursor.close();
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor allItems = db.rawQuery(
                "SELECT I._id AS _id, I.name AS item_name, L.name AS list_name, L._id AS list_id" +
                " FROM item I INNER JOIN list L ON I.list_id = L._id" +
                " ORDER BY L.weight DESC, L.name ASC, I.weight DESC, I.name ASC",
                null);
        // Filter in Java so matching can ignore punctuation/spacing (see SearchLogic)
        MatrixCursor filtered = new MatrixCursor(allItems.getColumnNames());
        try {
            while (allItems.moveToNext()) {
                String itemName = allItems.getString(1);
                if (SearchLogic.matches(query, itemName)) {
                    filtered.addRow(new Object[] { allItems.getLong(0), itemName,
                            allItems.getString(2), allItems.getLong(3) });
                }
            }
        } finally {
            allItems.close();
        }
        searchCursor = filtered;

        if (searchAdapter == null) {
            searchAdapter = new SimpleCursorAdapter(this,
                    R.layout.item_search_result,
                    searchCursor,
                    new String[] { "item_name", "list_name" },
                    new int[] { R.id.item_name, R.id.list_name },
                    0);
            searchResultsView.setAdapter(searchAdapter);
            searchResultsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Cursor cursor = (Cursor) searchAdapter.getItem(position);
                    long listId = cursor.getLong(cursor.getColumnIndexOrThrow("list_id"));
                    Intent i = new Intent(ListOverviewActivity.this, ListDetailActivity.class);
                    Uri listUri = Uri.parse(ListContentProvider.CONTENT_URI + "/" + listId);
                    i.putExtra(ListContentProvider.CONTENT_ITEM_TYPE, listUri);
                    i.putExtra("highlight_item_id", id);
                    startActivity(i);
                }
            });
        } else {
            searchAdapter.swapCursor(searchCursor);
        }

        showSearchMode();
    }

    private void showSearchMode() {
        listView.setVisibility(View.GONE);
        emptyListsView.setVisibility(View.GONE);
        searchResultsView.setVisibility(View.VISIBLE);
        searchResultsView.setEmptyView(emptySearchView);
        emptySearchView.setVisibility(searchAdapter != null && searchAdapter.getCount() == 0
                ? View.VISIBLE : View.GONE);
    }

    private void showListMode() {
        searchResultsView.setVisibility(View.GONE);
        emptySearchView.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        listView.setEmptyView(emptyListsView);
        if (searchCursor != null) {
            searchCursor.close();
            searchCursor = null;
        }
    }

    //
    // User-defined event handlers
    //

    public void showListDetailActivity(MenuItem item) {
        Intent intent = new Intent(this, ListDetailActivity.class);
        startActivity(intent);
    }
}
