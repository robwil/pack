package me.robwilliams.pack;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import me.robwilliams.pack.data.BagContentProvider;
import me.robwilliams.pack.data.ItemContentProvider;
import me.robwilliams.pack.data.ListContentProvider;
import me.robwilliams.pack.data.ListsetListContentProvider;
import me.robwilliams.pack.data.SetContentProvider;
import me.robwilliams.pack.data.TripBagContentProvider;
import me.robwilliams.pack.data.TripContentProvider;
import me.robwilliams.pack.data.TripItemContentProvider;
import me.robwilliams.pack.data.TripSetContentProvider;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.widget.EditText;

@RunWith(AndroidJUnit4.class)
public class TripPackingFlowTest {

    private static final String P = "__packtest_";
    private Context context;

    private long listA, listB;
    private long itemA1, itemA2, itemA3;
    private long itemB1, itemB2, itemB3;
    private long bag1, bag2;
    private long setId, tripId;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        ContentValues cv = new ContentValues();

        // Category A: 3 items
        cv.put("name", P + "Clothes");
        cv.put("weight", 10);
        listA = insertId(ListContentProvider.CONTENT_URI, cv);

        cv.clear(); cv.put("list_id", listA); cv.put("name", P + "Shirt"); cv.put("weight", 3);
        itemA1 = insertId(ItemContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("list_id", listA); cv.put("name", P + "Pants"); cv.put("weight", 2);
        itemA2 = insertId(ItemContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("list_id", listA); cv.put("name", P + "Socks"); cv.put("weight", 1);
        itemA3 = insertId(ItemContentProvider.CONTENT_URI, cv);

        // Category B: 3 items
        cv.clear();
        cv.put("name", P + "Toiletries");
        cv.put("weight", 5);
        listB = insertId(ListContentProvider.CONTENT_URI, cv);

        cv.clear(); cv.put("list_id", listB); cv.put("name", P + "Toothbrush"); cv.put("weight", 3);
        itemB1 = insertId(ItemContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("list_id", listB); cv.put("name", P + "Shampoo"); cv.put("weight", 2);
        itemB2 = insertId(ItemContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("list_id", listB); cv.put("name", P + "Sunscreen"); cv.put("weight", 1);
        itemB3 = insertId(ItemContentProvider.CONTENT_URI, cv);

        // 2 bags
        cv.clear(); cv.put("name", P + "Backpack"); cv.put("color", "#4CAF50");
        bag1 = insertId(BagContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("name", P + "Suitcase"); cv.put("color", "#2196F3");
        bag2 = insertId(BagContentProvider.CONTENT_URI, cv);

        // Set containing both lists
        cv.clear(); cv.put("name", P + "set"); cv.put("weight", 0);
        setId = insertId(SetContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("listset_id", setId); cv.put("list_id", listA);
        context.getContentResolver().insert(ListsetListContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("listset_id", setId); cv.put("list_id", listB);
        context.getContentResolver().insert(ListsetListContentProvider.CONTENT_URI, cv);

        // Trip linked to the set, with both bags
        cv.clear(); cv.put("name", P + "trip");
        tripId = insertId(TripContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("trip_id", tripId); cv.put("listset_id", setId);
        context.getContentResolver().insert(TripSetContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("trip_id", tripId); cv.put("bag_id", bag1);
        context.getContentResolver().insert(TripBagContentProvider.CONTENT_URI, cv);
        cv.clear(); cv.put("trip_id", tripId); cv.put("bag_id", bag2);
        context.getContentResolver().insert(TripBagContentProvider.CONTENT_URI, cv);
    }

    @After
    public void tearDown() {
        context.getContentResolver().delete(TripItemContentProvider.CONTENT_URI,
                "trip_id=" + tripId, null);
        context.getContentResolver().delete(TripBagContentProvider.CONTENT_URI,
                "trip_id=" + tripId, null);
        context.getContentResolver().delete(TripSetContentProvider.CONTENT_URI,
                "trip_id=" + tripId, null);
        context.getContentResolver().delete(TripContentProvider.CONTENT_URI,
                "_id=" + tripId, null);
        context.getContentResolver().delete(ListsetListContentProvider.CONTENT_URI,
                "listset_id=" + setId, null);
        context.getContentResolver().delete(SetContentProvider.CONTENT_URI,
                "_id=" + setId, null);
        for (long id : new long[]{itemA1, itemA2, itemA3, itemB1, itemB2, itemB3}) {
            context.getContentResolver().delete(ItemContentProvider.CONTENT_URI, "_id=" + id, null);
        }
        context.getContentResolver().delete(ListContentProvider.CONTENT_URI, "_id=" + listA, null);
        context.getContentResolver().delete(ListContentProvider.CONTENT_URI, "_id=" + listB, null);
        context.getContentResolver().delete(BagContentProvider.CONTENT_URI, "_id=" + bag1, null);
        context.getContentResolver().delete(BagContentProvider.CONTENT_URI, "_id=" + bag2, null);
    }

    @Test
    public void fullPackingFlow() {
        // Navigate to the trip (launches on Should Pack tab since nothing is selected)
        Uri tripUri = Uri.parse(TripContentProvider.CONTENT_URI + "/" + tripId);
        android.content.Intent intent = new android.content.Intent(context, TripDetailActivity.class);
        intent.putExtra(TripContentProvider.CONTENT_ITEM_TYPE, tripUri);
        ActivityScenario<TripDetailActivity> scenario = ActivityScenario.launch(intent);

        // --- SHOULD PACK: select 2 of 3 items per category ---
        // Clothes: select Shirt and Pants (skip Socks)
        onView(allOf(withText(P + "Shirt"), isDisplayed())).perform(click());
        onView(allOf(withText(P + "Pants"), isDisplayed())).perform(click());
        // Toiletries: select Toothbrush and Shampoo (skip Sunscreen)
        onView(allOf(withText(P + "Toothbrush"), isDisplayed())).perform(click());
        onView(allOf(withText(P + "Shampoo"), isDisplayed())).perform(click());

        // --- Switch to Pack tab ---
        onView(withText("Pack")).perform(click());

        // Set quantity of Shirt to 3 via long-press -> quantity dialog
        onView(allOf(withText(P + "Shirt"), isDisplayed())).perform(longClick());
        // Quantity dialog appears; type 3 and press OK
        onView(isAssignableFrom(EditText.class)).perform(replaceText("3"));
        onView(withText("OK")).perform(click());
        // Verify quantity is shown
        onView(allOf(withText(P + "Shirt x3"), isDisplayed())).check(matches(isDisplayed()));

        // Pack Shirt -> bag picker appears, select Backpack
        onView(allOf(withText(P + "Shirt x3"), isDisplayed())).perform(click());
        onView(withText(containsString(P + "Backpack"))).perform(click());
        onView(withText("OK")).perform(click());

        // Pack Pants -> bag picker appears, select Backpack
        onView(allOf(withText(P + "Pants"), isDisplayed())).perform(click());
        onView(withText(containsString(P + "Backpack"))).perform(click());
        onView(withText("OK")).perform(click());

        // Pack Toothbrush -> bag picker appears, select Suitcase
        onView(allOf(withText(P + "Toothbrush"), isDisplayed())).perform(click());
        onView(withText(containsString(P + "Suitcase"))).perform(click());
        onView(withText("OK")).perform(click());

        // Pack Shampoo -> bag picker appears, select Suitcase
        onView(allOf(withText(P + "Shampoo"), isDisplayed())).perform(click());
        onView(withText(containsString(P + "Suitcase"))).perform(click());
        onView(withText("OK")).perform(click());

        // --- QUICK EDIT: rename Shampoo -> Shampoo/Conditioner ---
        onView(allOf(withText(P + "Shampoo"), isDisplayed())).perform(longClick());
        // Quantity dialog appears; click "Quick Edit Item…"
        onView(withText("Quick Edit Item…")).perform(click());
        // Item editor opens; change the name
        onView(withId(R.id.name)).perform(clearText(), replaceText(P + "Shampoo/Conditioner"));
        // Press back to save (onPause triggers saveState) and return to trip
        Espresso.pressBack();

        // Verify renamed item on Pack tab
        onView(allOf(withText(P + "Shampoo/Conditioner"), isDisplayed()))
                .check(matches(isDisplayed()));
        onView(withText(P + "Shampoo")).check(doesNotExist());

        // --- Switch to Repack tab and verify all 4 items ---
        onView(withText("Repack")).perform(click());

        onView(allOf(withText(P + "Shirt x3"), isDisplayed())).check(matches(isDisplayed()));
        onView(allOf(withText(P + "Pants"), isDisplayed())).check(matches(isDisplayed()));
        onView(allOf(withText(P + "Toothbrush"), isDisplayed())).check(matches(isDisplayed()));
        onView(allOf(withText(P + "Shampoo/Conditioner"), isDisplayed())).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void reassignBagViLongPress() {
        // Pre-populate: mark Shirt as should-pack, then packed into Backpack
        ContentValues cv = new ContentValues();
        cv.put("trip_id", tripId); cv.put("item_id", itemA1);
        cv.put("status", 2); cv.put("bag_id", bag1);
        context.getContentResolver().insert(TripItemContentProvider.CONTENT_URI, cv);

        Uri tripUri = Uri.parse(TripContentProvider.CONTENT_URI + "/" + tripId);
        android.content.Intent intent = new android.content.Intent(context, TripDetailActivity.class);
        intent.putExtra(TripContentProvider.CONTENT_ITEM_TYPE, tripUri);
        ActivityScenario<TripDetailActivity> scenario = ActivityScenario.launch(intent);

        // Navigate to Pack tab where the packed item lives
        onView(withText("Pack")).perform(click());

        // Long press Shirt to open quantity dialog
        onView(allOf(withText(P + "Shirt"), isDisplayed())).perform(longClick());

        // Tap "Reassign Bag…"
        onView(withText("Reassign Bag…")).perform(click());

        // Bag picker appears — select Suitcase
        onView(withText(containsString(P + "Suitcase"))).perform(click());
        onView(withText("OK")).perform(click());

        // Verify the item is still displayed (bag updated in DB, UI refreshed)
        onView(allOf(withText(P + "Shirt"), isDisplayed())).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void clearRepackMovesItemsBackToPacked() {
        // Pre-populate: mark Shirt and Pants as repacked, Toothbrush as packed
        ContentValues cv = new ContentValues();
        cv.put("trip_id", tripId); cv.put("item_id", itemA1);
        cv.put("status", 3); cv.put("bag_id", bag1);
        context.getContentResolver().insert(TripItemContentProvider.CONTENT_URI, cv);

        cv.clear();
        cv.put("trip_id", tripId); cv.put("item_id", itemA2);
        cv.put("status", 3); cv.put("bag_id", bag1);
        context.getContentResolver().insert(TripItemContentProvider.CONTENT_URI, cv);

        cv.clear();
        cv.put("trip_id", tripId); cv.put("item_id", itemB1);
        cv.put("status", 2); cv.put("bag_id", bag2);
        context.getContentResolver().insert(TripItemContentProvider.CONTENT_URI, cv);

        Uri tripUri = Uri.parse(TripContentProvider.CONTENT_URI + "/" + tripId);
        android.content.Intent intent = new android.content.Intent(context, TripDetailActivity.class);
        intent.putExtra(TripContentProvider.CONTENT_ITEM_TYPE, tripUri);
        ActivityScenario<TripDetailActivity> scenario = ActivityScenario.launch(intent);

        // Open overflow menu and tap Clear Repack
        Espresso.openActionBarOverflowOrOptionsMenu(context);
        onView(withText("Clear Repack")).perform(click());

        // Confirm dialog — 2 repacked items
        onView(withText(containsString("2 repacked"))).check(matches(isDisplayed()));
        onView(withText(android.R.string.yes)).perform(click());

        // Switch to Pack tab — all 3 items should be there (Shirt, Pants moved back + Toothbrush)
        onView(withText("Pack")).perform(click());
        onView(allOf(withText(P + "Shirt"), isDisplayed())).check(matches(isDisplayed()));
        onView(allOf(withText(P + "Pants"), isDisplayed())).check(matches(isDisplayed()));
        onView(allOf(withText(P + "Toothbrush"), isDisplayed())).check(matches(isDisplayed()));

        // Switch to Repack tab — should have no repacked-only items
        // (Shirt/Pants now appear as packed, so they show on Repack tab too,
        // but verify via DB that status was actually changed)
        scenario.onActivity(activity -> {
            android.database.Cursor cursor = context.getContentResolver().query(
                    TripItemContentProvider.CONTENT_URI, new String[]{"status"},
                    "trip_id=" + tripId + " AND status=3", null, null);
            int repackedCount = cursor != null ? cursor.getCount() : -1;
            if (cursor != null) cursor.close();
            assert repackedCount == 0 : "Expected 0 repacked items, got " + repackedCount;
        });

        scenario.close();
    }

    private long insertId(Uri contentUri, ContentValues cv) {
        Uri uri = context.getContentResolver().insert(contentUri, cv);
        return Long.parseLong(uri.getLastPathSegment());
    }
}
