package me.robwilliams.pack;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import me.robwilliams.pack.data.Bag;

public class BagPickerDialogHelper {

    public interface OnBagSelectedListener {
        void onBagSelected(Bag bag);
        void onCancelled();
    }

    public static void show(Context context, String itemName, List<Bag> tripBags,
                            int preselectedBagId, OnBagSelectedListener listener) {
        if (tripBags == null || tripBags.isEmpty()) {
            return;
        }

        int preselectedIndex = -1;
        for (int i = 0; i < tripBags.size(); i++) {
            if (tripBags.get(i).getId() == preselectedBagId) {
                preselectedIndex = i;
                break;
            }
        }

        final int[] selectedIndex = {preselectedIndex >= 0 ? preselectedIndex : 0};

        float density = context.getResources().getDisplayMetrics().density;
        int dp12 = (int) (12 * density);
        int dp8 = (int) (8 * density);

        ArrayAdapter<Bag> adapter = new ArrayAdapter<Bag>(context, android.R.layout.simple_list_item_single_choice, tripBags) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    Bag bag = tripBags.get(position);
                    GradientDrawable circle = new GradientDrawable();
                    circle.setShape(GradientDrawable.OVAL);
                    try {
                        circle.setColor(Color.parseColor(bag.getColor()));
                    } catch (Exception e) {
                        circle.setColor(Color.GRAY);
                    }
                    circle.setBounds(0, 0, dp12 * 2, dp12 * 2);
                    tv.setCompoundDrawables(circle, null, null, null);
                    tv.setCompoundDrawablePadding(dp8);
                }
                return view;
            }
        };

        new AlertDialog.Builder(context)
                .setTitle("Pack into: " + itemName)
                .setSingleChoiceItems(adapter, selectedIndex[0], (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    if (selectedIndex[0] >= 0 && selectedIndex[0] < tripBags.size()) {
                        listener.onBagSelected(tripBags.get(selectedIndex[0]));
                    } else {
                        listener.onCancelled();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> listener.onCancelled())
                .setOnCancelListener(dialog -> listener.onCancelled())
                .show();
    }
}
