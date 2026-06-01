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

    public static void show(Context context, String itemName, int currentQuantity,
                            OnQuantitySetListener listener) {
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
                btn.setOnClickListener(v -> input.setText(String.valueOf(qty)));
                buttonRow.addView(btn);
            }

            scrollView.addView(buttonRow);
            layout.addView(scrollView);
        }

        new AlertDialog.Builder(context)
                .setTitle("Quantity: " + itemName)
                .setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
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
                .show();
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
