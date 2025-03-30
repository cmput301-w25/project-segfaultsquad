package com.example.segfaultsquadapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import static com.example.segfaultsquadapplication.TestLoginUtil.waitUntil;

import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.util.Log;
import android.widget.EditText;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.example.segfaultsquadapplication.display.MoodDetailsFragment;
import com.example.segfaultsquadapplication.display.moodaddedit.AddMoodFragment;
import com.example.segfaultsquadapplication.display.moodaddedit.EditMoodFragment;
import com.example.segfaultsquadapplication.display.moodhistory.MyMoodHistoryFragment;
import com.example.segfaultsquadapplication.display.profile.ProfileFragment;
import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


/**
 * This test tests for the mood history display and filtering; <br>
 * new mood event / modify mood event are also tested here by convenience. <br>
 * These tests are integrated into one to prevent wasting time to excessive login / splash simulation
 * and simulate a user's real-world usage of the App.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MoodHistoryAndMoodEventTest {
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
                new MoodEvent("1236478", MoodEvent.MoodType.ANGER, "RRR", null, null, true),
        };
        for (MoodEvent evt : evts) {
            DocumentReference docRef = moodsRef.document();
            evt.setDbFileId(docRef.getId());
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
     * The Android UI test would simulate a realtime user's behavior, <br>
     * thus all smaller tests are put together to mitigate displaying splash screen too many times
     * @throws InterruptedException If the test thread's waiting behavior is somehow interrupted
     */
    @Test
    public void testAll() throws InterruptedException {
        TestLoginUtil.handleSplashAndLogin(scenario, "user1@gmail.com", "password");

        // Test for display only own moods
        testOnlyOwnMood();
        // Test for invalid new moods
        testNewMoodInvalid();
        // Test for regular new moods
        testNewMoodRegular();
        // Test for modify mood event
        testModifyMoodEvent();
        // Test for mood filters
        testMoodFilter();
    }

    // Tests that the mood history only displays one's own moods
    private void testOnlyOwnMood() {
        System.out.println("Test Mood List - only displays the user's own moods");
        onView(withText("RRR")).check(doesNotExist());
    }

    // Test that invalid mood events are not added
    private void testNewMoodInvalid() {
        System.out.println("Test Invalid - Do not select emotion state");
        onView(withId(R.id.fabAddMood)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof AddMoodFragment), 20, 500));
        onView(withId(R.id.editTextReason)).perform(typeText("???"));
        onView(withId(R.id.spinnerSocialSituation))
                .perform(ViewActions.scrollTo()).perform(click());
        onView(withText(MoodEvent.SocialSituation.WITH_GROUP.getDisplayName())).perform(click());
        onView(withId(R.id.buttonConfirm)).perform(click());
        // Toast msg hard & unreliable to test; validate in mood event list.
        try {
            onView(withId(R.id.buttonCancel)).perform(click());
        } catch (Exception ignored) {}
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
        onView(withText("???")).check(doesNotExist());

        System.out.println("Test Invalid - Reason field has length limit");
        onView(withId(R.id.fabAddMood)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof AddMoodFragment), 20, 500));
        // Enter a very long message
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 200; i ++) {
            tooLong.append("A");
        }
        String expected = tooLong.toString();
        tooLong.append("Z");
        onView(withId(R.id.editTextReason)).perform(typeText(tooLong.toString()));
        // Make sure it does not have the last "Z" outside the length limit
        onView(withId(R.id.editTextReason)).check(matches(withText(expected)));
        onView(withId(R.id.buttonCancel)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
    }

    // Mood event; tests for "proper" new mood, up to optional fields.
    private void testNewMoodRegular() {
        System.out.println("Test Regular - All fields filled");
        onView(withId(R.id.fabAddMood)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof AddMoodFragment), 20, 500));
        onView(withText( MoodEvent.MoodType.FEAR.getEmoticon() )).perform(click());
        onView(withId(R.id.editTextReason)).perform(typeText("Reason..."));
        onView(withId(R.id.spinnerSocialSituation))
                .perform(ViewActions.scrollTo()).perform(click());
        onView(withText(MoodEvent.SocialSituation.WITH_GROUP.getDisplayName())).perform(scrollTo()).perform(click());
        onView(withId(R.id.togglePublicPrivate)).perform(scrollTo()).perform(click());
        onView(withId(R.id.buttonConfirm)).perform(click());
        // Somehow it needs to be clicked twice...
        try {
            onView(withId(R.id.buttonConfirm)).perform(click());
        } catch (Exception ignored) {}
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
        onView(withText("Reason...")).perform(scrollTo()).check(matches(isDisplayed()));

        System.out.println("Test Regular - optional fields omitted");
        onView(withId(R.id.fabAddMood)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof AddMoodFragment), 20, 500));
        onView(withText( MoodEvent.MoodType.ANGER.getEmoticon() )).perform(click());
        onView(withId(R.id.editTextReason)).perform(typeText("Fury!"));
        onView(withId(R.id.buttonConfirm)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
        onView(withText("Fury!")).perform(scrollTo()).check(matches(isDisplayed()));

        System.out.println("Test Regular - cancelled");
        onView(withId(R.id.fabAddMood)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof AddMoodFragment), 20, 500));
        onView(withText( MoodEvent.MoodType.ANGER.getEmoticon() )).perform(click());
        onView(withId(R.id.editTextReason)).perform(typeText("CANCEL!"));
        onView(withId(R.id.buttonCancel)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
        onView(withText("CANCEL!")).check(doesNotExist());
    }

    // Modifies a mood event.
    // Also validates whether the data saved by add/edit is correct.
    private void testModifyMoodEvent() {
        System.out.println("Test Modify mood event");
        onView(withText("Reason...")).perform(scrollTo()).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MoodDetailsFragment), 20, 500));
        // Validate the starting mood event info
        onView(withId(R.id.moodEmojiTextView)).check(matches(withText( MoodEvent.MoodType.FEAR.getEmoticon() )));
        onView(withId(R.id.reasonTextView)).check(matches(withText("Reason...")));
        onView(withId(R.id.socialSituationTextView)).check(matches(withText( MoodEvent.SocialSituation.WITH_GROUP.getDisplayName() )));
        onView(withId(R.id.mood_visibility)).check(matches(withText( "Public" )));

        // Start edit
        onView(withId(R.id.editButton)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof EditMoodFragment), 20, 500));
        onView(withText( MoodEvent.MoodType.CONFUSION.getEmoticon() )).perform(click());
        onView(withId(R.id.editTextReason)).perform(clearText()).perform(typeText("Reason text"));
        onView(withId(R.id.spinnerSocialSituation))
                .perform(ViewActions.scrollTo()).perform(click());
        onView(withText(MoodEvent.SocialSituation.IN_CROWD.getDisplayName())).perform(scrollTo()).perform(click());
        onView(withId(R.id.togglePublicPrivate)).perform(scrollTo()).perform(click());
        onView(withId(R.id.buttonConfirm)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MoodDetailsFragment), 20, 500));

        // Enter a very long message
        System.out.println("Test Modify - Reason field has length limit; cancel does not change event.");
        onView(withId(R.id.editButton)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof EditMoodFragment), 20, 500));
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 200; i ++) {
            tooLong.append("A");
        }
        String expected = tooLong.toString();
        tooLong.append("Z");
        onView(withId(R.id.editTextReason)).perform(clearText()).perform(typeText(tooLong.toString()));
        // Make sure it does not have the last "Z" outside the length limit
        onView(withId(R.id.editTextReason)).check(matches(withText(expected)));
        onView(withId(R.id.buttonCancel)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MoodDetailsFragment), 20, 500));

        // Validate edit result; the cancelled modification should not show up.
        onView(withId(R.id.moodEmojiTextView)).check(matches(withText( MoodEvent.MoodType.CONFUSION.getEmoticon() )));
        onView(withId(R.id.reasonTextView)).check(matches(withText("Reason text")));
        onView(withId(R.id.socialSituationTextView)).check(matches(withText( MoodEvent.SocialSituation.IN_CROWD.getDisplayName() )));
        onView(withId(R.id.mood_visibility)).check(matches(withText( "Private" )));
        onView(withId(R.id.backButton)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 20, 500));
    }

    // Check for filter functionality
    private void testMoodFilter() throws InterruptedException {
        System.out.println("Populate data for filtering later on");

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        MoodEvent evtLastMonth = new MoodEvent(uid, MoodEvent.MoodType.ANGER, "Last M.", null, null, true);

        Date newDate = Calendar.getInstance().getTime();
        newDate.setTime(newDate.getTime() - 30L * 24 * 60 * 60 * 1000);
        evtLastMonth.setTimestamp(new Timestamp(newDate));
        DbUtils.addObjectToCollection(DbUtils.COLL_MOOD_EVENTS, evtLastMonth,
                new DbOpResultHandler<>(null, null));

        MoodEvent evtOtherAngry = new MoodEvent(uid, MoodEvent.MoodType.ANGER, "RAGE", null, null, true);
        DbUtils.addObjectToCollection(DbUtils.COLL_MOOD_EVENTS, evtOtherAngry,
                new DbOpResultHandler<>(null, null));

        // Navigate away then navigate back to load events.
        onView(withId(R.id.navigation_profile)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        onView(withId(R.id.navigation_my_mood_history)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 5, 500));

        // Tests start below.

        System.out.println("Test - filter last week");
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("Last Week")).perform(click());
        Thread.sleep(500);
        // The recent mood should be shown
        onView(withText("Reason text")).perform(scrollTo()).check(matches(isDisplayed()));
        // Should not display the event last month!
        onView(withText("Last M.")).check(doesNotExist());

        System.out.println("Test - filter with mood");
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("By Mood")).perform(click());
        onView(withText( MoodEvent.MoodType.ANGER.name() )).perform(click());
        Thread.sleep(500);
        // The recent fury mood should be shown
        onView(withText("Fury!")).perform(scrollTo()).check(matches(isDisplayed()));
        // Should now reveal the angry event last month!
        onView(withText("Last M.")).perform(scrollTo()).check(matches(isDisplayed()));
        // The non-angry event should be hidden
        onView(withText("Reason text")).check(doesNotExist());

        System.out.println("Test - cancel all mood filter");
        onView(withId(R.id.filterButton)).perform(click());
        onView(withText("Clear All Filters")).perform(click());
        Thread.sleep(500);
        // The event last month should be shown
        onView(withText("Last M.")).check(doesNotExist());
        // The non-angry event should also be shown
        onView(withText("Reason text")).perform(scrollTo()).check(matches(isDisplayed()));

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
        onView(withText("RAGE")).perform(scrollTo()).check(matches(isDisplayed()));
    }
}
