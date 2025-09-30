package me.robwilliams.pack.fragment;

import android.content.ContentValues;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
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
import me.robwilliams.pack.data.TripItem;
import me.robwilliams.pack.data.TripItemContentProvider;

abstract public class AbstractPackingFragment extends Fragment implements PackingListExpandableAdapter.OnDataChangeListener {
    protected ExpandableListView expandableListView;
    protected PackingListExpandableAdapter expandableListAdapter;
    protected int checkMarkDrawableResId;

    protected int tripId;
    protected ArrayList<TripItem> tripItems;
    protected ArrayList<TripItem> sortedTripItems;
    protected Map<Integer, TripItem> tripItemMap;
    protected HashMap<String, List<TripItem>> groupedTripItems;
    protected List<String> listNames;
    protected Set<String> expandedGroups; // Track which groups are expanded

    public final static int STATUS_SHOULD_PACK = 1;
    public final static int STATUS_PACKED = 2;
    public final static int STATUS_REPACKED = 3;

    abstract protected int getCurrentPageStatus();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_packing, container, false);
        expandableListView = (ExpandableListView) rootView.findViewById(R.id.expandable_list_view);

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

        // Initialize expanded groups tracking
        expandedGroups = new HashSet<>();

        populateView();

        return rootView;
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

        // Save current expansion states before rebuilding
        saveExpansionStates();

        groupItemsByList();

        expandableListAdapter = new PackingListExpandableAdapter(
            getActivity(),
            listNames,
            groupedTripItems,
            tripItemMap,
            tripId,
            getCurrentPageStatus()
        );

        expandableListView.setAdapter(expandableListAdapter);
        expandableListAdapter.setOnDataChangeListener(this);

        // Restore expansion states instead of expanding all groups
        restoreExpansionStates();
    }

    private void saveExpansionStates() {
        if (expandableListView != null && expandableListAdapter != null && listNames != null) {
            expandedGroups.clear();
            for (int i = 0; i < listNames.size(); i++) {
                if (expandableListView.isGroupExpanded(i)) {
                    expandedGroups.add(listNames.get(i));
                }
            }
        }
    }

    private void restoreExpansionStates() {
        if (listNames == null) {
            return;
        }

        // Handle first-time setup separately from restoration
        if (expandedGroups.isEmpty()) {
            performInitialExpansionSetup();
        } else {
            // Restore previously saved expansion states
            for (int i = 0; i < listNames.size(); i++) {
                String groupName = listNames.get(i);
                if (expandedGroups.contains(groupName)) {
                    expandableListView.expandGroup(i);
                }
            }
        }
    }

    private void performInitialExpansionSetup() {
        // Define initial expansion behavior for first-time setup
        // Current behavior: expand all groups for better discoverability
        for (int i = 0; i < listNames.size(); i++) {
            expandableListView.expandGroup(i);
            expandedGroups.add(listNames.get(i));
        }

        // Alternative behaviors (commented out):
        //
        // Expand only first group:
        // if (!listNames.isEmpty()) {
        //     expandableListView.expandGroup(0);
        //     expandedGroups.add(listNames.get(0));
        // }
        //
        // Expand no groups (all collapsed):
        // (don't expand anything, leave expandedGroups empty)
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
