package com.example.segfaultsquadapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.segfaultsquadapplication.TestLoginUtil.waitUntil;
import static com.example.segfaultsquadapplication.UserFollowingTest.SUGGESTION_WAIT_TIME;
import static com.example.segfaultsquadapplication.UserFollowingTest.UI_POPULATE_WAIT_TIME;
import static com.example.segfaultsquadapplication.UserFollowingTest.clickCousinViewWithId;
import static com.example.segfaultsquadapplication.UserFollowingTest.handleRequests;
import static com.example.segfaultsquadapplication.UserFollowingTest.loginAs;
import static com.example.segfaultsquadapplication.UserFollowingTest.logout;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.example.segfaultsquadapplication.display.following.FollowingFragment;
import com.example.segfaultsquadapplication.display.moodhistory.MyMoodHistoryFragment;
import com.example.segfaultsquadapplication.display.profile.ProfileFragment;
import com.example.segfaultsquadapplication.display.profile.SearchedProfileFragment;
import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;


/**
 * This test tests for features related to comments. <br>
 * These tests are integrated into one to prevent wasting time to excessive login / splash simulation
 * and simulate a user's real-world usage of the App. <br>
 * For simplicity & consistency, use the login / following mechanisms from UserFollowingTest.
 *
 * NOTE: This is a black-box test; the implementation is not the concern,
 * We only worry that the functionalities are working.
 * Thus, this test is deliberately made large to capture the butterfly effect of potential bug.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventCommentTest {
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
        // Login users to generate user data
        for (int i = 1; i <= 2; i ++) {
            loginAs(scenario, "user" + i);
            logout(scenario);
        }
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
     * thus all smaller tests are put together to mitigate displaying splash screen too many times <br>
     * Also, many test cases would involve with different users and their internal ID,
     * making it impossible to be independently setup before individual tests.
     * @throws InterruptedException If the test thread's waiting behavior is somehow interrupted
     */
    @Test
    public void testAll() throws InterruptedException {
        // Prepare the test; make user2 follow user1
        handleUser2FlwRequest();
        // Go back to user 1, allow u2's request and create mood event
        acceptFollowing();
        createMoodEvents("u1");
        // Add some comments.
        checkAddSelfCmts();
        // Go back to user 2. Check displayed events, then accept user 1's follow request
        checkFollowedUserEvts();
    }

    /**
     * Login as user 2; send some follow requests
     * @throws InterruptedException If this is somehow interrupted
     */
    private void handleUser2FlwRequest() throws InterruptedException {
        System.out.println("Login as user2 to send follow requests for user1");
        loginAs(scenario, "user2");

        // Go to profile page
        onView(withId(R.id.navigation_profile)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        // Search for user 1
        onView(withId(R.id.headerSearchButton)).perform(click());
        onView(withId(R.id.searchEditText)).perform(typeText("u"));
        // Give it a few seconds to make suggestions.
        Thread.sleep(SUGGESTION_WAIT_TIME);
        // Make sure the user suggestion is working.
        onView(withText("user1")).check(matches(isDisplayed())).perform((click()));
        assertTrue(waitUntil(scenario, (f) -> (f instanceof SearchedProfileFragment), 5, 500));
        // Make sure the button displays the correct status
        onView(withId(R.id.follow_profile_button)).check(matches(withText("Follow"))).perform(click());
        onView(withId(R.id.backButton)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
    }

    /**
     * Creates a mood event for demonstration later
     * @param prefix Prefix for mood event
     * @throws InterruptedException If this is somehow interrupted
     */
    private void createMoodEvents(String prefix) throws InterruptedException {
        System.out.println("Populate event with prefix " + prefix);
        // Make a public event
        MoodEvent evt = new MoodEvent(UserManager.getUserId(), MoodEvent.MoodType.HAPPINESS,
                prefix + "E" + 3, null, null, true);
        DbUtils.addObjectToCollection(DbUtils.COLL_MOOD_EVENTS, evt,
                new DbOpResultHandler<>(null, null));
        Thread.sleep(100);
    }

    /**
     * Creates some comments for one's own events
     * @throws InterruptedException If this is somehow interrupted
     */
    private void checkAddSelfCmts() throws InterruptedException {
        System.out.println("Populate comment for user1's own events");
        onView(withId(R.id.navigation_my_mood_history)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof MyMoodHistoryFragment), 5, 500));
        Thread.sleep(UI_POPULATE_WAIT_TIME);
        // Click on event 3's comment
        onView(withText("u1E3")).perform(clickCousinViewWithId(R.id.comment_icon));
        Thread.sleep(UI_POPULATE_WAIT_TIME);
        onView(withId(R.id.commentsTitle)).check(matches(isDisplayed()));
        // Add one comment
        onView(withId(R.id.commentInput)).perform(typeText("User1 was here"));
        onView(withId(R.id.submitCommentButton)).perform(click());
        Thread.sleep(UI_POPULATE_WAIT_TIME);
        // Check displayed
        onView(withText("user1")).check(matches(isDisplayed()));
        onView(withText("User1 was here")).check(matches(isDisplayed()));
        // Go back
        pressBack();
    }

    /**
     * Make user 1 accept follow request
     * @throws InterruptedException If this is somehow interrupted
     */
    private void acceptFollowing() throws InterruptedException {
        // Logout & login
        System.out.println("Login as user1 to checkout follow requests");
        logout(scenario);
        loginAs(scenario, "user1");

        // Accept follow request
        handleRequests(scenario, List.of("user2"), List.of(), () -> {});
    }

    /**
     * Login as user 2, add a comment for event of followed user 1 and check how it goes.
     * @throws InterruptedException If this is somehow interrupted
     */
    private void checkFollowedUserEvts() throws InterruptedException {
        // Login as user 2
        System.out.println("Login as user2 to check comments");
        logout(scenario);
        loginAs(scenario, "user2");
        // To following mood events page
        System.out.println("Check follow user evts");
        onView(withId(R.id.navigation_following)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof FollowingFragment), 5, 500));
        Thread.sleep(UI_POPULATE_WAIT_TIME);
        // Click on event 3's comment
        onView(withText("u1E3")).perform(clickCousinViewWithId(R.id.comment_icon));
        Thread.sleep(UI_POPULATE_WAIT_TIME);
        onView(withId(R.id.commentsTitle)).check(matches(isDisplayed()));
        // Add one comment
        onView(withId(R.id.commentInput)).perform(typeText("User2 was also here"));
        onView(withId(R.id.submitCommentButton)).perform(click());
        Thread.sleep(UI_POPULATE_WAIT_TIME);
        onView(withText("u1E3")).perform(clickCousinViewWithId(R.id.comment_icon));
        Thread.sleep(UI_POPULATE_WAIT_TIME);
        // Check displayed
        onView(withText("user1")).check(matches(isDisplayed()));
        onView(withText("User1 was here")).check(matches(isDisplayed()));
        onView(withText("user2")).check(matches(isDisplayed()));
        onView(withText("User2 was also here")).check(matches(isDisplayed()));
        // Go back
        pressBack();
    }
}
