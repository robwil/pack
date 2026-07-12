package me.robwilliams.pack;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import me.robwilliams.pack.data.BagContentProvider;

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

import android.widget.EditText;

@RunWith(AndroidJUnit4.class)
public class BagCrudTest {

    private static final String BAG_NAME = "__crudtest_bag";
    private static final String BAG_NAME_EDITED = "__crudtest_bag_edited";

    @After
    public void tearDown() {
        // Safety cleanup in case a test fails partway through
        ApplicationProvider.getApplicationContext().getContentResolver()
                .delete(BagContentProvider.CONTENT_URI, "name=?", new String[]{BAG_NAME});
        ApplicationProvider.getApplicationContext().getContentResolver()
                .delete(BagContentProvider.CONTENT_URI, "name=?", new String[]{BAG_NAME_EDITED});
    }

    @Test
    public void createEditDeleteBag() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Home -> Bags
        onView(withId(R.id.tile_bags)).perform(click());
        onView(withId(R.id.bags_list)).check(matches(isDisplayed()));

        // --- CREATE ---
        onView(withId(R.id.action_add_bag)).perform(click());
        onView(withText("Add Bag")).check(matches(isDisplayed()));
        onView(isAssignableFrom(EditText.class)).perform(replaceText(BAG_NAME));
        onView(withText("Save")).perform(click());

        // Verify bag appears in the list
        onView(withText(BAG_NAME)).check(matches(isDisplayed()));

        // --- EDIT (rename) ---
        onView(withText(BAG_NAME)).perform(click());
        onView(withText("Edit Bag")).check(matches(isDisplayed()));
        onView(isAssignableFrom(EditText.class)).perform(clearText(), replaceText(BAG_NAME_EDITED));
        onView(withText("Save")).perform(click());

        // Verify old name gone, new name present
        onView(withText(BAG_NAME)).check(doesNotExist());
        onView(withText(BAG_NAME_EDITED)).check(matches(isDisplayed()));

        // --- DELETE ---
        onView(withText(BAG_NAME_EDITED)).perform(longClick());
        onView(withText("Delete Bag")).check(matches(isDisplayed()));
        onView(withText("Delete")).perform(click());

        // Verify bag is gone
        onView(withText(BAG_NAME_EDITED)).check(doesNotExist());

        // Navigate back to Home
        Espresso.pressBack();
        onView(withId(R.id.tile_bags)).check(matches(isDisplayed()));

        scenario.close();
    }
}
