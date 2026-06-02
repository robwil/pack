package me.robwilliams.pack.fragment;

import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.robwilliams.pack.R;
import me.robwilliams.pack.adapter.PackingListExpandableAdapter;
import me.robwilliams.pack.data.Bag;
import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.data.TripItemContentProvider;

abstract public class AbstractPackingFragment extends Fragment implements PackingListExpandableAdapter.OnDataChangeListener {
    protected ExpandableListView expandableListView;
    protected PackingListExpandableAdapter expandableListAdapter;
    protected HorizontalScrollView bagLegendScroll;
    protected LinearLayout bagLegendContainer;
    protected int checkMarkDrawableResId;

    protected int tripId;
    protected ArrayList<TripItem> tripItems;
    protected ArrayList<TripItem> sortedTripItems;
    protected Map<Integer, TripItem> tripItemMap;
    protected HashMap<String, List<TripItem>> groupedTripItems;
    protected List<String> listNames;
    protected Set<String> expandedGroups; // Track which groups are expanded
    protected boolean hasUserCollapsed = false;
    protected ArrayList<Bag> tripBags;

    public final static int STATUS_SHOULD_PACK = 1;
    public final static int STATUS_PACKED = 2;
    public final static int STATUS_REPACKED = 3;

    abstract protected int getCurrentPageStatus();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_packing, container, false);
        expandableListView = (ExpandableListView) rootView.findViewById(R.id.expandable_list_view);
        bagLegendScroll = (HorizontalScrollView) rootView.findViewById(R.id.bag_legend_scroll);
        bagLegendContainer = (LinearLayout) rootView.findViewById(R.id.bag_legend);

        // Look up resource id for built-in Android checkmark icon
        TypedValue value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple, value, true);
        checkMarkDrawableResId = value.resourceId;

        // Fetch arguments for Fragment to get the Trip Items (already loaded from DB by parent activity)
        Bundle arguments = getArguments();
        tripId = arguments.getInt("tripId");
        tripItems = arguments.getParcelableArrayList("tripItems");
        tripBags = arguments.getParcelableArrayList("tripBags");
        if (tripBags == null) tripBags = new ArrayList<>();

        // Put trip items in Map for easy access
        tripItemMap = new HashMap<>();
        for (TripItem tripItem : tripItems) {
            tripItemMap.put(tripItem.getItemId(), tripItem);
        }

        // Initialize expanded groups tracking
        expandedGroups = new HashSet<>();

        populateView();
        populateBagLegend();

        return rootView;
    }

    protected void populateBagLegend() {
        if (bagLegendScroll == null || bagLegendContainer == null) return;

        if (tripBags == null || tripBags.isEmpty()) {
            bagLegendScroll.setVisibility(View.GONE);
            return;
        }

        bagLegendScroll.setVisibility(View.VISIBLE);
        bagLegendContainer.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int chipSize = (int) (10 * density);
        int dp4 = (int) (4 * density);
        int dp8 = (int) (8 * density);

        for (Bag bag : tripBags) {
            LinearLayout entry = new LinearLayout(getActivity());
            entry.setOrientation(LinearLayout.HORIZONTAL);
            entry.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams entryParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            entryParams.setMargins(0, 0, dp8 * 2, 0);
            entry.setLayoutParams(entryParams);

            View dot = new View(getActivity());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(chipSize, chipSize);
            dotParams.setMargins(0, 0, dp4, 0);
            dot.setLayoutParams(dotParams);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            try {
                circle.setColor(Color.parseColor(bag.getColor()));
            } catch (Exception e) {
                circle.setColor(Color.GRAY);
            }
            dot.setBackground(circle);

            TextView label = new TextView(getActivity());
            label.setText(bag.getName());
            label.setTextSize(12);
            label.setTextColor(0xFF666666);

            entry.addView(dot);
            entry.addView(label);
            bagLegendContainer.addView(entry);
        }
    }

    protected void sortTripItems() {
        sortedTripItems = new ArrayList<>(tripItems);
        Collections.sort(sortedTripItems, new Comparator<TripItem>() {
            @Override
            public int compare(TripItem lhs, TripItem rhs) {
                return lhs.getStatus() - rhs.getStatus();
            }
        });
    }

    public void populateView() {
        // Defensive check to prevent crashes if fragment is being destroyed
        if (getActivity() == null || expandableListView == null) {
            return;
        }

        groupItemsByList();

        expandableListAdapter = new PackingListExpandableAdapter(
            getActivity(),
            listNames,
            groupedTripItems,
            tripItemMap,
            tripId,
            getCurrentPageStatus(),
            tripBags
        );

        expandableListView.setAdapter(expandableListAdapter);
        expandableListAdapter.setOnDataChangeListener(this);

        // Track user-initiated collapse/expand
        expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {
            @Override
            public void onGroupCollapse(int groupPosition) {
                hasUserCollapsed = true;
                if (listNames != null && groupPosition < listNames.size()) {
                    expandedGroups.remove(listNames.get(groupPosition));
                }
            }
        });
        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                if (listNames != null && groupPosition < listNames.size()) {
                    expandedGroups.add(listNames.get(groupPosition));
                }
            }
        });

        restoreExpansionStates();
    }

    private void restoreExpansionStates() {
        if (listNames == null || expandableListView == null) {
            return;
        }

        if (!hasUserCollapsed && expandedGroups.isEmpty()) {
            // First time: expand all groups, deferred to after layout
            expandableListView.post(new Runnable() {
                @Override
                public void run() {
                    if (expandableListView != null && listNames != null) {
                        for (int i = 0; i < listNames.size(); i++) {
                            expandableListView.expandGroup(i);
                        }
                    }
                }
            });
        } else {
            for (int i = 0; i < listNames.size(); i++) {
                String groupName = listNames.get(i);
                if (expandedGroups.contains(groupName)) {
                    expandableListView.expandGroup(i);
                }
            }
        }
    }

    private void groupItemsByList() {
        // For non-Should Pack fragment, sort Trip Items by status ASC first
        List<TripItem> listToIterate = tripItems;
        if (getCurrentPageStatus() > STATUS_SHOULD_PACK) {
            sortTripItems();
            listToIterate = sortedTripItems;
        }

        groupedTripItems = new LinkedHashMap<>();
        listNames = new ArrayList<>();

        for (TripItem tripItem : listToIterate) {
            int status = tripItem.getStatus();
            if (status >= getCurrentPageStatus() - 1) {
                String listName = tripItem.getListName();

                if (!groupedTripItems.containsKey(listName)) {
                    groupedTripItems.put(listName, new ArrayList<TripItem>());
                    listNames.add(listName);
                }

                groupedTripItems.get(listName).add(tripItem);
            }
        }
    }

    @Override
    public void onDataChanged() {
        // Re-populate the view when data changes for non-Should Pack tabs
        // Use smarter update instead of full rebuild to preserve expansion states
        updateDataSmartly();
    }

    @Override
    public void onDestroyView() {
        // Clean up adapter references to prevent memory leaks
        if (expandableListAdapter != null) {
            expandableListAdapter.clearDataChangeListener();
            expandableListAdapter = null;
        }
        expandableListView = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        // Additional cleanup when fragment is detached
        tripItemMap = null;
        groupedTripItems = null;
        listNames = null;
        expandedGroups = null;
        super.onDetach();
    }

    private void updateDataSmartly() {
        // Defensive check to prevent crashes if fragment is being destroyed
        if (getActivity() == null || expandableListView == null) {
            return;
        }

        groupItemsByList();

        if (expandableListAdapter != null) {
            // Update existing adapter data instead of creating new adapter
            expandableListAdapter.updateData(listNames, groupedTripItems, tripItemMap);
        } else {
            // Fallback to full rebuild if adapter doesn't exist yet
            populateView();
        }
    }

}
