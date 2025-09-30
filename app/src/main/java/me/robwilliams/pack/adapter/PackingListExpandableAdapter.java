package me.robwilliams.pack.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.robwilliams.pack.R;
import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.data.TripItemContentProvider;
import me.robwilliams.pack.fragment.AbstractPackingFragment;

public class PackingListExpandableAdapter extends BaseExpandableListAdapter {

    public interface OnDataChangeListener {
        void onDataChanged();
    }

    private Context context;
    private List<String> listNames; // Group headers (packing list names)
    private HashMap<String, List<TripItem>> listItems; // Items grouped by list name
    private Map<Integer, TripItem> tripItemMap;
    private int tripId;
    private int currentPageStatus;
    private int checkMarkDrawableResId;
    private WeakReference<OnDataChangeListener> dataChangeListenerRef;

    public PackingListExpandableAdapter(Context context, List<String> listNames,
                                        HashMap<String, List<TripItem>> listItems,
                                        Map<Integer, TripItem> tripItemMap,
                                        int tripId, int currentPageStatus) {
        this.context = context;
        this.listNames = listNames;
        this.listItems = listItems;
        this.tripItemMap = tripItemMap;
        this.tripId = tripId;
        this.currentPageStatus = currentPageStatus;

        // Get system checkmark drawable
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple, value, true);
        checkMarkDrawableResId = value.resourceId;
    }

    public void setOnDataChangeListener(OnDataChangeListener listener) {
        this.dataChangeListenerRef = listener != null ? new WeakReference<>(listener) : null;
    }

    public void clearDataChangeListener() {
        this.dataChangeListenerRef = null;
    }

    public void updateData(List<String> newListNames, HashMap<String, List<TripItem>> newListItems, Map<Integer, TripItem> newTripItemMap) {
        this.listNames = newListNames;
        this.listItems = newListItems;
        this.tripItemMap = newTripItemMap;
        notifyDataSetChanged();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.listItems.get(this.listNames.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        TripItem item = (TripItem) getChild(groupPosition, childPosition);
        return item.getItemId();
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final TripItem tripItem = (TripItem) getChild(groupPosition, childPosition);

        if (convertView == null) {
            // Create a custom layout with LinearLayout containing TextView and ImageView
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(60, 10, 10, 10);
            layout.setClickable(true);

            TextView textView = new TextView(context);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            textView.setTextSize(16);

            ImageView checkBox = new ImageView(context);
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            checkBox.setPadding(10, 0, 0, 0);

            layout.addView(textView);
            layout.addView(checkBox);

            convertView = layout;
        }

        final LinearLayout itemLayout = (LinearLayout) convertView;
        final TextView textView = (TextView) itemLayout.getChildAt(0);
        final ImageView checkBox = (ImageView) itemLayout.getChildAt(1);

        textView.setText(tripItem.getItemName());

        // Set checkbox state immediately without any animation
        boolean isChecked = tripItem.getStatus() >= currentPageStatus;
        checkBox.setImageResource(isChecked ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);

        itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int itemId = tripItem.getItemId();
                int currentItemStatus = tripItemMap.get(itemId).getStatus();
                int newItemStatus = currentItemStatus >= currentPageStatus ?
                    currentPageStatus - 1 : currentPageStatus;

                // Update checkbox state immediately
                boolean newCheckedState = newItemStatus >= currentPageStatus;
                checkBox.setImageResource(newCheckedState ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);

                ContentValues values = new ContentValues();
                values.put("item_id", itemId);
                values.put("trip_id", tripId);
                values.put("status", newItemStatus);

                if (currentItemStatus == 0) {
                    context.getContentResolver().insert(TripItemContentProvider.CONTENT_URI, values);
                } else if (currentItemStatus == AbstractPackingFragment.STATUS_SHOULD_PACK &&
                           currentPageStatus == AbstractPackingFragment.STATUS_SHOULD_PACK) {
                    context.getContentResolver().delete(TripItemContentProvider.CONTENT_URI,
                            "item_id=" + itemId + " AND trip_id=" + tripId, null);
                } else {
                    context.getContentResolver().update(TripItemContentProvider.CONTENT_URI, values,
                            "item_id=" + itemId + " AND trip_id=" + tripId, null);
                }

                tripItemMap.get(itemId).setStatus(newItemStatus);

                // For non-Should Pack views, we need to inform the fragment to refresh
                // since the data structure needs to be rebuilt
                if (currentPageStatus > AbstractPackingFragment.STATUS_SHOULD_PACK) {
                    OnDataChangeListener listener = dataChangeListenerRef != null ? dataChangeListenerRef.get() : null;
                    if (listener != null) {
                        listener.onDataChanged();
                    }
                }
            }
        });

        return itemLayout;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.listItems.get(this.listNames.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.listNames.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.listNames.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String listName = (String) getGroup(groupPosition);

        if (convertView == null) {
            convertView = new TextView(context);
        }

        TextView textView = (TextView) convertView;
        textView.setTextAppearance(context, android.R.style.TextAppearance_Large);

        // Add expand/collapse indicator
        String indicator = isExpanded ? "− " : "+ ";
        textView.setText(indicator + listName);

        textView.setPadding(20, 15, 20, 15);

        // Different background colors for expanded vs collapsed
        if (isExpanded) {
            // Light pastel green background for expanded groups
            textView.setBackgroundColor(0xFFE8F5E8); // Light pastel green
            textView.setTextColor(0xFF2E7D32); // Darker green text
        } else {
            // Light gray background with green text for collapsed groups
            textView.setBackgroundColor(0xFFF5F5F5); // Light gray
            textView.setTextColor(0xFF2E7D32); // Green text (same as expanded)
        }

        return textView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}