package me.robwilliams.pack.fragment;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.robwilliams.pack.R;
import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.data.TripItemContentProvider;

abstract public class AbstractPackingFragment extends Fragment {
    protected LinearLayout mainLayout;
    protected int checkMarkDrawableResId;

    protected int tripId;
    protected ArrayList<TripItem> tripItems;
    protected Map<Integer, TripItem> tripItemMap;

    protected final static int STATUS_SHOULD_PACK = 1;
    protected final static int STATUS_PACKED = 2;
    protected final static int STATUS_REPACKED = 3;

    abstract protected int getCurrentPageStatus();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_should_pack, container, false);
        mainLayout = (LinearLayout) rootView.findViewById(R.id.main_fragment_layout);

        // Look up resource id for built-in Android checkmark icon
        TypedValue value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple, value, true);
        checkMarkDrawableResId = value.resourceId;

        // Fetch arguments for Fragment to get the Trip Items (already loaded from DB by parent activity)
        Bundle arguments = getArguments();
        tripId = arguments.getInt("tripId");
        tripItems = arguments.getParcelableArrayList("tripItems");

        // Put trip items in Map for easy access
        tripItemMap = new HashMap<>();
        for (TripItem tripItem : tripItems) {
            tripItemMap.put(tripItem.getItemId(), tripItem);
        }

        populateView();

        return rootView;
    }

    public void populateView() {
        // Loop through cursor and dynamically create interface of checkable items with Category titles
        String currentListName = null;
        for (TripItem tripItem : tripItems) {
            int status = tripItem.getStatus();
            if (status >= getCurrentPageStatus() - 1) {
                if (!tripItem.getListName().equals(currentListName)) {
                    // Encountered new list name so add as title-like TextView
                    currentListName = tripItem.getListName();
                    createAndAddTextView(currentListName);
                }
                createAndAddCheckItem(tripItem.getItemId(), tripItem.getItemName(), status);
            }
        }
    }

    protected void createAndAddTextView(String text) {
        TextView textView = new TextView(getActivity());
        textView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Large);
        textView.setText(text);
        mainLayout.addView(textView);
    }

    protected void createAndAddCheckItem(final int itemId, final String itemText, int status) {
        final CheckedTextView checkedTextView = new CheckedTextView(getActivity());
        checkedTextView.setText(itemText);
        checkedTextView.setCheckMarkDrawable(checkMarkDrawableResId);
        checkedTextView.setChecked(status >= getCurrentPageStatus());
        checkedTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Since this click handler is operating when status may have changed, always look up values
                // from the tripItemMap

                int currentItemStatus = tripItemMap.get(itemId).getStatus();
                int newItemStatus = currentItemStatus >= getCurrentPageStatus() ? getCurrentPageStatus() - 1 : getCurrentPageStatus();

                ContentValues values = new ContentValues();
                values.put("item_id", itemId);
                values.put("trip_id", tripId);
                values.put("status", newItemStatus);
                if (currentItemStatus == 0) {
                    getActivity().getContentResolver().insert(TripItemContentProvider.CONTENT_URI, values);
                } else if (currentItemStatus == STATUS_SHOULD_PACK && getCurrentPageStatus() == STATUS_SHOULD_PACK) {
                    getActivity().getContentResolver().delete(TripItemContentProvider.CONTENT_URI,
                            "item_id=" + itemId + " AND trip_id=" + tripId, null);
                } else {
                    getActivity().getContentResolver().update(TripItemContentProvider.CONTENT_URI, values,
                            "item_id=" + itemId + " AND trip_id=" + tripId, null);
                }
                checkedTextView.setChecked(currentItemStatus < getCurrentPageStatus());
                tripItemMap.get(itemId).setStatus(newItemStatus);
            }
        });
        mainLayout.addView(checkedTextView);
    }
}
