package com.example.segfaultsquadapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import static com.example.segfaultsquadapplication.TestLoginUtil.waitUntil;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.util.Log;
import android.widget.EditText;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.services.events.TimeStamp;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


/**
 * This test tests for the mood history display, filter & new mood features. </br>
 * These tests are integrated into one to prevent wasting time to excessive login / splash simulation
 * and simulate a user's real-world usage of the App.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodHistoryAndAddMoodTest {
    @BeforeClass
    public static void setup(){
        // Specific address for emulated device to access our localHost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    @Before
    public void seedDatabase() throws InterruptedException {
        // Just in case if last run crashed half-way!
        tearDown();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference moodsRef = db.collection("moods");

        MoodEvent[] evts = {
                new MoodEvent("1236478", MoodEvent.MoodType.ANGRY, "RRR"),
        };
        for (MoodEvent evt : evts) {
            DocumentReference docRef = moodsRef.document();
            evt.setMoodId(docRef.getId());
            docRef.set(evt);
        }

        Thread.sleep(1000);
    }

    @After
    public void tearDown() {
        String projectId = "cmput301-project-a9aad";
        URL url = null;
        try {
            url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
        } catch (MalformedURLException exception) {
            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
        }
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            int response = urlConnection.getResponseCode();
            Log.i("Response Code", "Response Code: " + response);
        } catch (IOException exception) {
            Log.e("IO Error", Objects.requireNonNull(exception.getMessage()));
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);
    @Rule
    public GrantPermissionRule locPermRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    /**
     * The Android UI test would simulate a realtime user's behavior, </br>
     * thus all smaller tests are put together to mitigate displaying splash screen too many times
     * @throws InterruptedException If the test thread's waiting behavior is somehow interrupted
     */
    @Test
    public void testAll() throws InterruptedException {
        TestLoginUtil.handleSplashAndLogin(scenario, "user1@gmail.com", "password");

        // Test for display only own moods
        testOnlyOwnMood();
        // Test for regular new moods
        testNewMoodRegular();
        // Test for mood filters
        testMoodFilter();
    }

    // Tests that the mood history only displays one's own moods
    private void testOnlyOwnMood() {
        System.out.println("Test Mood List - only displays the user's own moods");
        onView(withText("RRR")).check(doesNotExist());
    }

    // Mood event; tests for "proper" new mood, up to optional fields.
    private void testNewMoodRegular() {
        System.out.println("Test Regular - All fields filled");
        onView(withId(R.id.fabAddMood)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof AddMoodFragment), 20, 500));
        onView(withText("😴")).perform(click());
        onView(withId(R.id.editTextReason)).perform(typeText("Reason text"));
        // TODO: Lets not mock photos just yet...
        onView(withId(R.id.spinnerSocialSituation))
                .perform(ViewActions.scrollTo()).perform(click());
        onView(withText("WITH_GROUP")).perform(click());
        onView(withId(R.id.buttonConfirm)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
        onView(withText("Reason text")).check(matches(isDisplayed()));

        System.out.println("Test Regular - optional fields omitted");
        onView(withId(R.id.fabAddMood)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof AddMoodFragment), 20, 500));
        onView(withText("😡")).perform(click());
        onView(withId(R.id.editTextReason)).perform(typeText("Fury!"));
        onView(withId(R.id.buttonConfirm)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
        onView(withText("Fury!")).check(matches(isDisplayed()));

        System.out.println("Test Regular - cancelled");
        onView(withId(R.id.fabAddMood)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof AddMoodFragment), 20, 500));
        onView(withText("😡")).perform(click());
        onView(withId(R.id.editTextReason)).perform(typeText("CANCEL!"));
        onView(withId(R.id.buttonCancel)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
        onView(withText("CANCEL!")).check(doesNotExist());
    }

    // Check for filter functionality
    private void testMoodFilter() throws InterruptedException {
        System.out.println("Populate data for filter");
        CollectionReference collRef = FirebaseFirestore.getInstance().collection("moods");

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        MoodEvent evtLastMonth = new MoodEvent(uid, MoodEvent.MoodType.ANGRY, "Last M.");
        DocumentReference docRef = collRef.document();
        Date newDate = Calendar.getInstance().getTime();
        newDate.setTime(newDate.getTime() - 30L * 24 * 60 * 60 * 1000);
        evtLastMonth.setTimestamp(new Timestamp(newDate));
        evtLastMonth.setMoodId(docRef.getId());
        docRef.set(evtLastMonth);

        MoodEvent evtOtherAngry = new MoodEvent(uid, MoodEvent.MoodType.ANGRY, "RAGE");
        docRef = collRef.document();
        evtOtherAngry.setMoodId(docRef.getId());
        docRef.set(evtOtherAngry);
        Thread.sleep(1000);

        // Tests start below.

        System.out.println("Test - filter last week");
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("Last Week")).perform(click());
        Thread.sleep(500);
        // The recent mood should be shown
        onView(withText("Reason text")).check(matches(isDisplayed()));
        // Should not display the event last month!
        onView(withText("Last M.")).check(doesNotExist());

        System.out.println("Test - filter with mood");
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("By Mood")).perform(click());
        onView(withText("ANGRY")).perform(click());
        Thread.sleep(500);
        // The recent fury mood should be shown
        onView(withText("Fury!")).check(matches(isDisplayed()));
        // Should now reveal the angry event last month!
        onView(withText("Last M.")).check(matches(isDisplayed()));
        // The TIRED event should be hidden
        onView(withText("Reason text")).check(doesNotExist());

        System.out.println("Test - cancel all mood filter");
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("Clear All Filters")).perform(click());
        Thread.sleep(500);
        // The event last month should be shown
        onView(withText("Last M.")).check(matches(isDisplayed()));
        // The TIRED event should also be shown
        onView(withText("Reason text")).check(matches(isDisplayed()));

        System.out.println("Test - filter by reason");
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("By Reason")).perform(click());
        onView(withClassName(Matchers.equalTo(EditText.class.getName())))
                .perform(typeText("RA"));
        onView(withId(android.R.id.button1)).perform(click());
        Thread.sleep(500);
        // The RAGE event without RA should be hidden
        onView(withText("Fury!")).check(doesNotExist());
        // The RAGE event with RA should be shown
        onView(withText("RAGE")).check(matches(isDisplayed()));
    }
}
