package me.robwilliams.pack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class QuantityDialogHelper {

    private static final String PREFS_NAME = "quantity_prefs";
    private static final String KEY_RECENT = "recent_quantities";
    private static final int MAX_RECENT = 5;

    public interface OnQuantitySetListener {
        void onQuantitySet(int quantity);
    }

    public interface OnQuickEditListener {
        void onQuickEdit();
    }

    public interface OnReassignBagListener {
        void onReassignBag();
    }

    public static void show(Context context, String itemName, int currentQuantity,
                            OnQuantitySetListener listener) {
        show(context, itemName, currentQuantity, listener, null, null);
    }

    public static void show(Context context, String itemName, int currentQuantity,
                            OnQuantitySetListener listener,
                            OnQuickEditListener quickEditListener) {
        show(context, itemName, currentQuantity, listener, quickEditListener, null);
    }

    public static void show(Context context, String itemName, int currentQuantity,
                            OnQuantitySetListener listener,
                            OnQuickEditListener quickEditListener,
                            OnReassignBagListener reassignBagListener) {
        float density = context.getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        int dp8 = (int) (8 * density);
        int dp48 = (int) (48 * density);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp16, dp16, dp16, dp8);

        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentQuantity));
        input.selectAll();
        input.setTextSize(18);
        layout.addView(input);

        List<Integer> recentQuantities = getRecentQuantities(context);
        List<Button> recentButtons = new ArrayList<>();
        if (!recentQuantities.isEmpty()) {
            TextView recentLabel = new TextView(context);
            recentLabel.setText("Recent:");
            recentLabel.setTextSize(12);
            recentLabel.setTextColor(Color.GRAY);
            recentLabel.setPadding(0, dp8, 0, dp8);
            layout.addView(recentLabel);

            HorizontalScrollView scrollView = new HorizontalScrollView(context);
            LinearLayout buttonRow = new LinearLayout(context);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);

            for (int qty : recentQuantities) {
                Button btn = new Button(context);
                btn.setText(String.valueOf(qty));
                btn.setMinimumWidth(dp48);
                btn.setMinHeight(dp48);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, dp8, 0);
                btn.setLayoutParams(params);
                recentButtons.add(btn);
                buttonRow.addView(btn);
            }

            scrollView.addView(buttonRow);
            layout.addView(scrollView);
        }

        if (quickEditListener != null) {
            Button editButton = new Button(context);
            editButton.setText("Quick Edit Item…");
            editButton.setTextSize(14);
            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            editParams.setMargins(0, dp8, 0, 0);
            editButton.setLayoutParams(editParams);
            layout.addView(editButton);
        }

        if (reassignBagListener != null) {
            Button reassignButton = new Button(context);
            reassignButton.setText("Reassign Bag…");
            reassignButton.setTextSize(14);
            LinearLayout.LayoutParams reassignParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            reassignParams.setMargins(0, dp8, 0, 0);
            reassignButton.setLayoutParams(reassignParams);
            layout.addView(reassignButton);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Quantity: " + itemName)
                .setView(layout)
                .setPositiveButton("OK", (d, which) -> {
                    String text = input.getText().toString().trim();
                    int quantity = 1;
                    try {
                        quantity = Integer.parseInt(text);
                        if (quantity < 1) quantity = 1;
                    } catch (NumberFormatException e) {
                        // default to 1
                    }
                    saveRecentQuantity(context, quantity);
                    listener.onQuantitySet(quantity);
                })
                .setNegativeButton("Cancel", null)
                .create();
        for (Button btn : recentButtons) {
            int qty = Integer.parseInt(btn.getText().toString());
            btn.setOnClickListener(v -> {
                saveRecentQuantity(context, qty);
                listener.onQuantitySet(qty);
                dialog.dismiss();
            });
        }

        if (reassignBagListener != null) {
            Button reassignButton = (Button) layout.getChildAt(layout.getChildCount() - 1);
            reassignButton.setOnClickListener(v -> {
                dialog.dismiss();
                reassignBagListener.onReassignBag();
            });
        }

        if (quickEditListener != null) {
            int editIndex = layout.getChildCount() - 1;
            if (reassignBagListener != null) editIndex--;
            Button editButton = (Button) layout.getChildAt(editIndex);
            editButton.setOnClickListener(v -> {
                dialog.dismiss();
                quickEditListener.onQuickEdit();
            });
        }

        dialog.show();
    }

    private static List<Integer> getRecentQuantities(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_RECENT, "");
        List<Integer> result = new ArrayList<>();
        if (!stored.isEmpty()) {
            for (String s : stored.split(",")) {
                try {
                    int val = Integer.parseInt(s.trim());
                    if (val > 1) result.add(val);
                } catch (NumberFormatException e) {
                    // skip
                }
            }
        }
        return result;
    }

    private static void saveRecentQuantity(Context context, int quantity) {
        if (quantity <= 1) return;
        List<Integer> existing = getRecentQuantities(context);
        LinkedHashSet<Integer> deduped = new LinkedHashSet<>();
        deduped.add(quantity);
        deduped.addAll(existing);

        List<Integer> result = new ArrayList<>(deduped);
        if (result.size() > MAX_RECENT) {
            result = result.subList(0, MAX_RECENT);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(result.get(i));
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_RECENT, sb.toString()).apply();
    }
}
