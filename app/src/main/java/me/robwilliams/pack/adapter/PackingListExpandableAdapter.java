package me.robwilliams.pack.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.robwilliams.pack.BagPickerDialogHelper;
import me.robwilliams.pack.QuantityDialogHelper;
import me.robwilliams.pack.R;
import me.robwilliams.pack.data.Bag;
import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.data.TripItemContentProvider;
import me.robwilliams.pack.fragment.AbstractPackingFragment;

public class PackingListExpandableAdapter extends BaseExpandableListAdapter {

    public interface OnDataChangeListener {
        void onDataChanged();
    }

    private Context context;
    private List<String> listNames;
    private HashMap<String, List<TripItem>> listItems;
    private Map<Integer, TripItem> tripItemMap;
    private int tripId;
    private int currentPageStatus;
    private int checkMarkDrawableResId;
    private WeakReference<OnDataChangeListener> dataChangeListenerRef;
    private List<Bag> tripBags;

    public PackingListExpandableAdapter(Context context, List<String> listNames,
                                        HashMap<String, List<TripItem>> listItems,
                                        Map<Integer, TripItem> tripItemMap,
                                        int tripId, int currentPageStatus,
                                        List<Bag> tripBags) {
        this.context = context;
        this.listNames = listNames;
        this.listItems = listItems;
        this.tripItemMap = tripItemMap;
        this.tripId = tripId;
        this.currentPageStatus = currentPageStatus;
        this.tripBags = tripBags != null ? tripBags : new ArrayList<>();

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
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(60, 10, 10, 10);
            layout.setClickable(true);
            layout.setGravity(Gravity.CENTER_VERTICAL);

            TextView textView = new TextView(context);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            textView.setTextSize(16);

            View bagChip = new View(context);
            float density = context.getResources().getDisplayMetrics().density;
            int chipSize = (int) (12 * density);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(chipSize, chipSize);
            chipParams.setMargins((int)(8 * density), 0, (int)(8 * density), 0);
            chipParams.gravity = Gravity.CENTER_VERTICAL;
            bagChip.setLayoutParams(chipParams);
            bagChip.setVisibility(View.GONE);

            ImageView checkBox = new ImageView(context);
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            checkBox.setPadding(10, 0, 0, 0);

            layout.addView(textView);
            layout.addView(bagChip);
            layout.addView(checkBox);

            convertView = layout;
        }

        final LinearLayout itemLayout = (LinearLayout) convertView;
        final TextView textView = (TextView) itemLayout.getChildAt(0);
        final View bagChip = itemLayout.getChildAt(1);
        final ImageView checkBox = (ImageView) itemLayout.getChildAt(2);

        String displayText = tripItem.getItemName();
        if (tripItem.getQuantity() > 1) {
            displayText += " x" + tripItem.getQuantity();
        }
        textView.setText(displayText);

        // Bag chip
        if (tripItem.getBagId() > 0 && tripItem.getBagColor() != null) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            try {
                circle.setColor(Color.parseColor(tripItem.getBagColor()));
            } catch (Exception e) {
                circle.setColor(Color.GRAY);
            }
            bagChip.setBackground(circle);
            bagChip.setVisibility(View.VISIBLE);
        } else {
            bagChip.setVisibility(View.GONE);
        }

        boolean isChecked = tripItem.getStatus() >= currentPageStatus;
        checkBox.setImageResource(isChecked ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);

        itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int itemId = tripItem.getItemId();
                int currentItemStatus = tripItemMap.get(itemId).getStatus();
                int newItemStatus = currentItemStatus >= currentPageStatus ?
                    currentPageStatus - 1 : currentPageStatus;

                boolean isPacking = newItemStatus >= currentPageStatus;
                boolean needsBagSelection = isPacking && !tripBags.isEmpty()
                        && currentPageStatus >= AbstractPackingFragment.STATUS_PACKED;

                if (needsBagSelection) {
                    int preselectedBagId = tripItem.getBagId() > 0 ? tripItem.getBagId() : tripItem.getBagHintId();
                    BagPickerDialogHelper.show(context, tripItem.getItemName(), tripBags, preselectedBagId,
                            new BagPickerDialogHelper.OnBagSelectedListener() {
                                @Override
                                public void onBagSelected(Bag bag) {
                                    performStatusUpdate(tripItem, itemId, newItemStatus, currentItemStatus,
                                            bag.getId(), bag.getName(), bag.getColor(), checkBox);
                                }

                                @Override
                                public void onCancelled() {
                                    // Do nothing — item stays at current status
                                }
                            });
                } else {
                    int bagId = isPacking ? 0 : 0;
                    performStatusUpdate(tripItem, itemId, newItemStatus, currentItemStatus,
                            0, null, null, checkBox);
                }
            }
        });

        itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final int itemId = tripItem.getItemId();
                QuantityDialogHelper.show(context, tripItem.getItemName(), tripItem.getQuantity(),
                        new QuantityDialogHelper.OnQuantitySetListener() {
                            @Override
                            public void onQuantitySet(int quantity) {
                                tripItemMap.get(itemId).setQuantity(quantity);
                                ContentValues values = new ContentValues();
                                values.put("quantity", quantity);
                                if (tripItemMap.get(itemId).getStatus() == 0) {
                                    values.put("item_id", itemId);
                                    values.put("trip_id", tripId);
                                    values.put("status", AbstractPackingFragment.STATUS_SHOULD_PACK);
                                    tripItemMap.get(itemId).setStatus(AbstractPackingFragment.STATUS_SHOULD_PACK);
                                    context.getContentResolver().insert(TripItemContentProvider.CONTENT_URI, values);
                                } else {
                                    context.getContentResolver().update(TripItemContentProvider.CONTENT_URI, values,
                                            "item_id=" + itemId + " AND trip_id=" + tripId, null);
                                }
                                notifyDataSetChanged();
                            }
                        });
                return true;
            }
        });

        return itemLayout;
    }

    private void performStatusUpdate(TripItem tripItem, int itemId, int newItemStatus,
                                     int currentItemStatus, int bagId, String bagName,
                                     String bagColor, ImageView checkBox) {
        boolean newCheckedState = newItemStatus >= currentPageStatus;
        checkBox.setImageResource(newCheckedState ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_unchecked);

        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        values.put("trip_id", tripId);
        values.put("status", newItemStatus);
        if (bagId > 0) {
            values.put("bag_id", bagId);
        } else if (!newCheckedState) {
            values.putNull("bag_id");
        }

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
        if (bagId > 0) {
            tripItemMap.get(itemId).setBagId(bagId);
            tripItemMap.get(itemId).setBagName(bagName);
            tripItemMap.get(itemId).setBagColor(bagColor);
        } else if (!newCheckedState) {
            tripItemMap.get(itemId).setBagId(0);
            tripItemMap.get(itemId).setBagName(null);
            tripItemMap.get(itemId).setBagColor(null);
        }

        if (currentPageStatus > AbstractPackingFragment.STATUS_SHOULD_PACK) {
            OnDataChangeListener listener = dataChangeListenerRef != null ? dataChangeListenerRef.get() : null;
            if (listener != null) {
                listener.onDataChanged();
            }
        }
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

        String indicator = isExpanded ? "− " : "+ ";
        textView.setText(indicator + listName);

        textView.setPadding(20, 15, 20, 15);

        if (isExpanded) {
            textView.setBackgroundColor(0xFFE8F5E8);
            textView.setTextColor(0xFF2E7D32);
        } else {
            textView.setBackgroundColor(0xFFF5F5F5);
            textView.setTextColor(0xFF2E7D32);
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
