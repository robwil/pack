package me.robwilliams.pack.fragment;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import me.robwilliams.pack.R;
import me.robwilliams.pack.data.DatabaseHelper;
import me.robwilliams.pack.data.TripItemContentProvider;

public class ShouldPackFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private LinearLayout mainLayout;
    private int checkMarkDrawableResId;

    private int tripId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_should_pack, container, false);

        mainLayout = (LinearLayout) rootView.findViewById(R.id.main_fragment_layout);

        Bundle arguments = getArguments();
        tripId = arguments.getInt("tripId");

        TypedValue value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple, value, true);
        checkMarkDrawableResId = value.resourceId;

        getActivity().getLoaderManager().initLoader(0, null, this).forceLoad();

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // TODO: This load should probably be in TripDetailActivity, and then passed into here via arguments ??

        // This query will join basically all the tables and return a list of all the items
        // which are found in lists referenced by this trip's Set. It joins those items with the trip_item
        // table to also fetch their "status" - whether they should be packed, are packed, or are repacked.
        final Context context = getActivity();
        return new AsyncTaskLoader<Cursor>(context) {
            @Override
            public Cursor loadInBackground() {
                SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
                return db.rawQuery("SELECT L.name as list_name, L.weight as list_weight, I._id as item_id, I.name as item_name, I.weight as item_weight, TI.status as status FROM trip T " +
                        "INNER JOIN trip_listset TLS ON T._id=TLS.trip_id AND TLS.trip_id=" + tripId + " " +
                        "INNER JOIN listset_list LSL ON LSL.listset_id=TLS.listset_id " +
                        "INNER JOIN list L ON L._id=LSL.list_id " +
                        "INNER JOIN item I ON I.list_id=LSL.list_id " +
                        "LEFT JOIN trip_item TI ON TI.trip_id=TLS.trip_id AND TI.item_id=I._id " +
                        "ORDER BY list_weight DESC, list_name ASC, item_weight DESC, item_name ASC", null);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Loop through cursor and dynamically create interface of checkable items with Category titles
        String currentListName = null;
        if (data != null) {
            while (data.moveToNext()) {
                String listName = data.getString(data.getColumnIndexOrThrow("list_name"));
                if (!listName.equals(currentListName)) {
                    // Encountered new list name so add as title-like TextView
                    currentListName = listName;
                    createAndAddTextView(currentListName);
                }
                int itemId = data.getInt(data.getColumnIndexOrThrow("item_id"));
                String itemName = data.getString(data.getColumnIndexOrThrow("item_name"));
                int status = data.getInt(data.getColumnIndexOrThrow("status"));
                createAndAddCheckItem(itemId, itemName, status);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mainLayout.removeAllViews();
    }

    private void createAndAddTextView(String text) {
        TextView textView = new TextView(getActivity());
        textView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Large);
        textView.setText(text);
        mainLayout.addView(textView);
    }

    private void createAndAddCheckItem(final int itemId, final String itemText, final int status) {
        final CheckedTextView checkedTextView = new CheckedTextView(getActivity());
        checkedTextView.setText(itemText);
        checkedTextView.setCheckMarkDrawable(checkMarkDrawableResId);
        checkedTextView.setChecked(status >= 1); // TODO: use constant
        checkedTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentValues values = new ContentValues();
                values.put("item_id", itemId);
                values.put("trip_id", tripId);
                values.put("status", status >= 1 ? 0 : 1);
                // TODO: handle update scenario - this is really an upsert
                getActivity().getContentResolver().insert(TripItemContentProvider.CONTENT_URI, values);
                checkedTextView.setChecked(status == 0);
            }
        });
        mainLayout.addView(checkedTextView);
    }
}
