package me.robwilliams.pack;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.robwilliams.pack.data.Bag;
import me.robwilliams.pack.data.BagContentProvider;
import me.robwilliams.pack.data.TripBagContentProvider;
import me.robwilliams.pack.data.TripItemContentProvider;

public class TripBagSelectionDialogHelper {

    public interface OnBagsChangedListener {
        void onBagsChanged(List<Bag> activeBags);
    }

    public static void show(Context context, int tripId, OnBagsChangedListener listener) {
        List<Bag> allBags = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(BagContentProvider.CONTENT_URI,
                new String[]{"_id", "name", "color"}, null, null, "name ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                allBags.add(new Bag(
                        cursor.getInt(cursor.getColumnIndexOrThrow("_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("color"))
                ));
            }
            cursor.close();
        }

        if (allBags.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("No Bags")
                    .setMessage("No bags have been created yet. Create bags from the main screen first.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Set<Integer> activeBagIds = new HashSet<>();
        cursor = context.getContentResolver().query(TripBagContentProvider.CONTENT_URI,
                new String[]{"_id", "bag_id"}, "trip_id=" + tripId, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                activeBagIds.add(cursor.getInt(cursor.getColumnIndexOrThrow("bag_id")));
            }
            cursor.close();
        }

        final boolean[] checked = new boolean[allBags.size()];
        for (int i = 0; i < allBags.size(); i++) {
            checked[i] = activeBagIds.contains(allBags.get(i).getId());
        }

        final boolean[] originalChecked = checked.clone();

        float density = context.getResources().getDisplayMetrics().density;
        int dp12 = (int) (12 * density);
        int dp8 = (int) (8 * density);
        int dp16 = (int) (16 * density);
        int dp48 = (int) (48 * density);

        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(dp16, dp8, dp16, dp8);

        for (int i = 0; i < allBags.size(); i++) {
            final int index = i;
            Bag bag = allBags.get(i);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp48);
            row.setPadding(0, dp8, 0, dp8);

            CheckBox checkBox = new CheckBox(context);
            checkBox.setChecked(checked[i]);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> checked[index] = isChecked);

            View dot = new View(context);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp12 * 2, dp12 * 2);
            dotParams.setMargins(dp8, 0, dp12, 0);
            dot.setLayoutParams(dotParams);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            try {
                circle.setColor(Color.parseColor(bag.getColor()));
            } catch (Exception e) {
                circle.setColor(Color.GRAY);
            }
            dot.setBackground(circle);

            TextView nameView = new TextView(context);
            nameView.setText(bag.getName());
            nameView.setTextSize(16);

            row.addView(checkBox);
            row.addView(dot);
            row.addView(nameView);

            row.setOnClickListener(v -> checkBox.toggle());

            listLayout.addView(row);
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(listLayout);

        new AlertDialog.Builder(context)
                .setTitle("Bags for this Trip")
                .setView(scrollView)
                .setPositiveButton("OK", (dialog, which) -> {
                    applyBagChanges(context, tripId, allBags, originalChecked, checked);
                    List<Bag> newActiveBags = new ArrayList<>();
                    for (int i = 0; i < allBags.size(); i++) {
                        if (checked[i]) {
                            newActiveBags.add(allBags.get(i));
                        }
                    }
                    listener.onBagsChanged(newActiveBags);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void applyBagChanges(Context context, int tripId, List<Bag> allBags,
                                          boolean[] originalChecked, boolean[] newChecked) {
        for (int i = 0; i < allBags.size(); i++) {
            int bagId = allBags.get(i).getId();
            if (!originalChecked[i] && newChecked[i]) {
                ContentValues values = new ContentValues();
                values.put("trip_id", tripId);
                values.put("bag_id", bagId);
                context.getContentResolver().insert(TripBagContentProvider.CONTENT_URI, values);
            } else if (originalChecked[i] && !newChecked[i]) {
                ContentValues nullBag = new ContentValues();
                nullBag.putNull("bag_id");
                context.getContentResolver().update(TripItemContentProvider.CONTENT_URI, nullBag,
                        "trip_id=" + tripId + " AND bag_id=" + bagId, null);
                context.getContentResolver().delete(TripBagContentProvider.CONTENT_URI,
                        "trip_id=" + tripId + " AND bag_id=" + bagId, null);
            }
        }
    }
}
