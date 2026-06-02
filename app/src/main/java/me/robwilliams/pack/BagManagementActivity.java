package me.robwilliams.pack;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import me.robwilliams.pack.data.BagContentProvider;

public class BagManagementActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String[] PRESET_COLORS = {
            "#EF9A9A", "#F48FB1", "#CE93D8", "#90CAF9",
            "#80DEEA", "#80CBC4", "#A5D6A7", "#FFCC80",
            "#BCAAA4", "#B0BEC5", "#78909C", "#FFD54F"
    };

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bag_management);
        fillData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bag_management, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {"_id", "name", "color"};
        return new CursorLoader(this, BagContentProvider.CONTENT_URI, projection, null, null, "name ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    private void fillData() {
        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1,
                null,
                new String[]{"name"},
                new int[]{android.R.id.text1},
                0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == android.R.id.text1) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String color = cursor.getString(cursor.getColumnIndexOrThrow("color"));
                    TextView textView = (TextView) view;
                    textView.setText(name);
                    textView.setCompoundDrawablePadding(16);
                    GradientDrawable circle = new GradientDrawable();
                    circle.setShape(GradientDrawable.OVAL);
                    try {
                        circle.setColor(Color.parseColor(color));
                    } catch (Exception e) {
                        circle.setColor(Color.GRAY);
                    }
                    int size = (int) (24 * view.getResources().getDisplayMetrics().density);
                    circle.setBounds(0, 0, size, size);
                    textView.setCompoundDrawables(circle, null, null, null);
                    return true;
                }
                return false;
            }
        });

        ListView listView = (ListView) findViewById(R.id.bags_list);
        listView.setAdapter(adapter);
        listView.setEmptyView(findViewById(R.id.empty_bags));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) adapter.getItem(position);
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String color = cursor.getString(cursor.getColumnIndexOrThrow("color"));
                showEditBagDialog(id, name, color);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) adapter.getItem(position);
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                showDeleteBagDialog(id, name);
                return true;
            }
        });
    }

    public void showAddBagDialog(MenuItem item) {
        showBagDialog(-1, "", PRESET_COLORS[6]);
    }

    private void showEditBagDialog(long bagId, String currentName, String currentColor) {
        showBagDialog(bagId, currentName, currentColor);
    }

    private void showBagDialog(long bagId, String currentName, String currentColor) {
        float density = getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        int dp8 = (int) (8 * density);
        int dp40 = (int) (40 * density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp16, dp16, dp8);

        TextView nameLabel = new TextView(this);
        nameLabel.setText("Name");
        nameLabel.setTextSize(12);
        nameLabel.setTextColor(Color.GRAY);
        layout.addView(nameLabel);

        EditText nameInput = new EditText(this);
        nameInput.setText(currentName);
        nameInput.setTextSize(16);
        layout.addView(nameInput);

        TextView colorLabel = new TextView(this);
        colorLabel.setText("Color");
        colorLabel.setTextSize(12);
        colorLabel.setTextColor(Color.GRAY);
        colorLabel.setPadding(0, dp8, 0, dp8);
        layout.addView(colorLabel);

        final String[] selectedColor = {currentColor};

        GridLayout colorGrid = new GridLayout(this);
        colorGrid.setColumnCount(4);

        for (String color : PRESET_COLORS) {
            View swatch = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp40;
            params.height = dp40;
            params.setMargins(dp8, dp8, dp8, dp8);
            swatch.setLayoutParams(params);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp8);
            bg.setColor(Color.parseColor(color));
            if (color.equals(currentColor)) {
                bg.setStroke((int) (3 * density), Color.BLACK);
            }
            swatch.setBackground(bg);

            swatch.setOnClickListener(v -> {
                selectedColor[0] = color;
                for (int i = 0; i < colorGrid.getChildCount(); i++) {
                    View child = colorGrid.getChildAt(i);
                    GradientDrawable childBg = new GradientDrawable();
                    childBg.setShape(GradientDrawable.RECTANGLE);
                    childBg.setCornerRadius(dp8);
                    childBg.setColor(Color.parseColor(PRESET_COLORS[i]));
                    if (PRESET_COLORS[i].equals(color)) {
                        childBg.setStroke((int) (3 * density), Color.BLACK);
                    }
                    child.setBackground(childBg);
                }
            });

            colorGrid.addView(swatch);
        }

        layout.addView(colorGrid);

        String title = bagId == -1 ? "Add Bag" : "Edit Bag";
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ContentValues values = new ContentValues();
                    values.put("name", name);
                    values.put("color", selectedColor[0]);

                    if (bagId == -1) {
                        getContentResolver().insert(BagContentProvider.CONTENT_URI, values);
                    } else {
                        Uri uri = Uri.parse(BagContentProvider.CONTENT_URI + "/" + bagId);
                        getContentResolver().update(uri, values, null, null);
                    }
                    getLoaderManager().restartLoader(0, null, this);
                })
                .setNegativeButton("Cancel", null);

        builder.show();
    }

    private void showDeleteBagDialog(long bagId, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Bag")
                .setMessage("Delete bag \"" + name + "\"? Items hinted to this bag will lose their hint.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Uri uri = Uri.parse(BagContentProvider.CONTENT_URI + "/" + bagId);
                    getContentResolver().delete(uri, null, null);
                    getLoaderManager().restartLoader(0, null, this);
                    Toast.makeText(this, "Bag deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
