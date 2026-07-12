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

import me.robwilliams.pack.data.ItemContentProvider;
import me.robwilliams.pack.data.ListContentProvider;
import me.robwilliams.pack.data.ListsetListContentProvider;
import me.robwilliams.pack.data.SetContentProvider;
import me.robwilliams.pack.data.TripContentProvider;
import me.robwilliams.pack.data.TripSetContentProvider;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;

@RunWith(AndroidJUnit4.class)
public class NavigationTest {

    private static final String TEST_PREFIX = "__navtest_";
    private Context context;
    private long tripId;
    private long listId;
    private long itemId;
    private long setId;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();

        // Seed: list -> item, set -> set_list, trip -> trip_set
        ContentValues cv = new ContentValues();

        cv.put("name", TEST_PREFIX + "list");
        cv.put("weight", 0);
        Uri listUri = context.getContentResolver().insert(ListContentProvider.CONTENT_URI, cv);
        listId = Long.parseLong(listUri.getLastPathSegment());

        cv.clear();
        cv.put("list_id", listId);
        cv.put("name", TEST_PREFIX + "item");
        cv.put("weight", 0);
        Uri itemUri = context.getContentResolver().insert(ItemContentProvider.CONTENT_URI, cv);
        itemId = Long.parseLong(itemUri.getLastPathSegment());

        cv.clear();
        cv.put("name", TEST_PREFIX + "set");
        cv.put("weight", 0);
        Uri setUri = context.getContentResolver().insert(SetContentProvider.CONTENT_URI, cv);
        setId = Long.parseLong(setUri.getLastPathSegment());

        cv.clear();
        cv.put("listset_id", setId);
        cv.put("list_id", listId);
        context.getContentResolver().insert(ListsetListContentProvider.CONTENT_URI, cv);

        cv.clear();
        cv.put("name", TEST_PREFIX + "trip");
        Uri tripUri = context.getContentResolver().insert(TripContentProvider.CONTENT_URI, cv);
        tripId = Long.parseLong(tripUri.getLastPathSegment());

        cv.clear();
        cv.put("trip_id", tripId);
        cv.put("listset_id", setId);
        context.getContentResolver().insert(TripSetContentProvider.CONTENT_URI, cv);
    }

    @After
    public void tearDown() {
        // Delete in reverse dependency order
        context.getContentResolver().delete(TripSetContentProvider.CONTENT_URI,
                "trip_id=" + tripId, null);
        context.getContentResolver().delete(TripContentProvider.CONTENT_URI,
                "_id=" + tripId, null);
        context.getContentResolver().delete(ListsetListContentProvider.CONTENT_URI,
                "listset_id=" + setId, null);
        context.getContentResolver().delete(SetContentProvider.CONTENT_URI,
                "_id=" + setId, null);
        context.getContentResolver().delete(ItemContentProvider.CONTENT_URI,
                "_id=" + itemId, null);
        context.getContentResolver().delete(ListContentProvider.CONTENT_URI,
                "_id=" + listId, null);
    }

    @Test
    public void homeToTripAndBack_doesNotCrash() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Home -> Trips
        onView(withId(R.id.tile_trips)).perform(click());

        // Trips list -> click first trip
        onData(anything())
                .inAdapterView(withId(R.id.trips))
                .atPosition(0)
                .perform(click());

        // Trip detail should be showing the pager
        onView(withId(R.id.pager)).check(matches(isDisplayed()));

        // Back to Trips list
        Espresso.pressBack();
        onView(withId(R.id.trips)).check(matches(isDisplayed()));

        // Back to Home
        Espresso.pressBack();
        onView(withId(R.id.tile_trips)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void homeToSetAndBack_doesNotCrash() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Home -> Sets
        onView(withId(R.id.tile_sets)).perform(click());

        // Sets list -> click the test set
        onData(anything())
                .inAdapterView(withId(R.id.sets))
                .atPosition(0)
                .perform(click());

        // Set detail should show the lists checklist
        onView(withId(R.id.listset_lists)).check(matches(isDisplayed()));

        // Back to Sets list
        Espresso.pressBack();
        onView(withId(R.id.sets)).check(matches(isDisplayed()));

        // Back to Home
        Espresso.pressBack();
        onView(withId(R.id.tile_sets)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void homeToListItemAndBack_doesNotCrash() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Home -> Lists
        onView(withId(R.id.tile_lists)).perform(click());

        // Lists -> click the test list
        onData(anything())
                .inAdapterView(withId(R.id.lists))
                .atPosition(0)
                .perform(click());

        // List detail -> click the test item
        onData(anything())
                .inAdapterView(withId(R.id.list_items))
                .atPosition(0)
                .perform(click());

        // Item detail should show the name field
        onView(withId(R.id.name)).check(matches(isDisplayed()));

        // Back to List detail
        Espresso.pressBack();
        onView(withId(R.id.list_items)).check(matches(isDisplayed()));

        // Back to Lists
        Espresso.pressBack();
        onView(withId(R.id.lists)).check(matches(isDisplayed()));

        // Back to Home
        Espresso.pressBack();
        onView(withId(R.id.tile_lists)).check(matches(isDisplayed()));

        scenario.close();
    }
}
