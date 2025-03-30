package com.example.segfaultsquadapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.segfaultsquadapplication.TestLoginUtil.waitUntil;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.util.Log;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.example.segfaultsquadapplication.display.LoginFragment;
import com.example.segfaultsquadapplication.display.following.FollowRequestsFragment;
import com.example.segfaultsquadapplication.display.following.FollowersListFragment;
import com.example.segfaultsquadapplication.display.following.FollowingFragment;
import com.example.segfaultsquadapplication.display.following.FollowingListFragment;
import com.example.segfaultsquadapplication.display.profile.ProfileFragment;
import com.example.segfaultsquadapplication.display.profile.SearchedProfileFragment;
import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
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
import java.util.Objects;


/**
 * This test tests for features related to following. </br>
 * Based on the nature of this test, info availability across accounts & logout are automatically tested. </br>
 * That is, follow request, follow back, remove follower, see followed users' events. </br>
 * These tests are integrated into one to prevent wasting time to excessive login / splash simulation
 * and simulate a user's real-world usage of the App. </br>
 * Reference: https://stackoverflow.com/questions/28476507/using-espresso-to-click-view-inside-recyclerview-item
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserFollowingTest {
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
     * thus all smaller tests are put together to mitigate displaying splash screen too many times </br>
     * Also, many test cases would involve with different users and their internal ID,
     * making it impossible to be independently setup before individual tests.
     * @throws InterruptedException If the test thread's waiting behavior is somehow interrupted
     */
    @Test
    public void testAll() throws InterruptedException {
        // Test for user search & follow request; also create stub mood events to test for later.
        testUser2FlwRequest();
        createMoodEvents("u2");
        testUser3FlwRequest();
        createMoodEvents("u3");
        // Go back to user 1, allow u2 and decline u3;
        // Follow back u2 and check events are shown (i.e. follow-back does not need permission)
        checkUser1FollowingStatus();
        checkFollowedUserEvts();
        // Unfollow mechanisms (remove follower and unfollow)
        checkRemoveFlw();
    }

    // Login as user 2; send some follow requests
    private void testUser2FlwRequest() throws InterruptedException {
        TestLoginUtil.handleSplashAndLogin(scenario, "user2@gmail.com", "password");
        System.out.println("Login as user2 to send follow requests for user1 and user 3");

        // Go to profile page
        onView(withId(R.id.navigation_profile)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        // Search for user 1
        onView(withId(R.id.headerSearchButton)).perform(click());
        onView(withId(R.id.searchEditText)).perform(typeText("u"));
        // Make sure the user suggestion is working.
        onView(withText("user1")).check(matches(isDisplayed())).perform((click()));
        assertTrue(waitUntil(scenario, (f) -> (f instanceof SearchedProfileFragment), 5, 500));
        // Make sure the button displays the correct status
        onView(withId(R.id.follow_profile_button)).check(matches(withText("Follow"))).perform(click());
        onView(withId(R.id.follow_profile_button)).check(matches(withText("Requested to Follow")));
        onView(withId(R.id.backButton)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));

        // Can't search for oneself.
        onView(withId(R.id.headerSearchButton)).perform(click());
        onView(withId(R.id.searchEditText)).perform(typeText("user"));
        onView(withText("user2")).check(doesNotExist());

        // Follow user 3; this time we use the goto button
        onView(withId(R.id.searchEditText)).perform(clearText()).perform(typeText("user3"));
        onView(withId(R.id.searchButton)).check(matches(isDisplayed())).perform((click()));
        assertTrue(waitUntil(scenario, (f) -> (f instanceof SearchedProfileFragment), 5, 500));
        onView(withId(R.id.follow_profile_button)).check(matches(withText("Follow"))).perform(click());
        onView(withId(R.id.backButton)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
    }

    // Login as user 3; send some follow requests
    private void testUser3FlwRequest() throws InterruptedException {
        // Logout & login
        onView(withId(R.id.logoutDropdown)).perform(click());
        onView(withText("Logout")).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof LoginFragment), 5, 500));
        TestLoginUtil.handleSplashAndLogin(scenario, "user3@gmail.com", "password");
        System.out.println("Login as user3 to send follow requests for user1");

        // Go to profile page
        onView(withId(R.id.navigation_profile)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        // Search for user 1
        onView(withId(R.id.headerSearchButton)).perform(click());
        onView(withId(R.id.searchEditText)).perform(typeText("u"));
        // Make sure the user suggestion is working.
        onView(withText("user1")).check(matches(isDisplayed())).perform((click()));
        assertTrue(waitUntil(scenario, (f) -> (f instanceof SearchedProfileFragment), 5, 500));
        // Make sure the button displays the correct status
        onView(withId(R.id.follow_profile_button)).check(matches(withText("Follow"))).perform(click());
        onView(withId(R.id.backButton)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
    }

    // Creates mood events for demonstration later
    private void createMoodEvents(String prefix) throws InterruptedException {
        // Make 4 public and 1 private
        for (int i = 1; i <= 5; i ++) {
            MoodEvent evt = new MoodEvent(UserManager.getUserId(), MoodEvent.MoodType.CONFUSION,
                    prefix + "E" + i, null, null, i <= 4);
            DbUtils.addObjectToCollection(DbUtils.COLL_MOOD_EVENTS, evt,
                    new DbOpResultHandler<>(null, null));
            Thread.sleep(100);
        }
    }

    // Checks following mechanism (acceptance etc.) with user 1
    private void checkUser1FollowingStatus() throws InterruptedException {
        // Logout & login
        onView(withId(R.id.logoutDropdown)).perform(click());
        onView(withText("Logout")).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof LoginFragment), 5, 500));
        TestLoginUtil.handleSplashAndLogin(scenario, "user1@gmail.com", "password");
        System.out.println("Login as user1 to checkout follow requests");

        // Go to profile page
        onView(withId(R.id.navigation_profile)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        // Check: now have no follower, before accepting user 2.
        onView(withId(R.id.followers_count)).check(matches(withText("0")));

        // Go to follow requests page
        onView(withId(R.id.navigation_follow_requests)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof FollowRequestsFragment), 5, 500));
        // Accept user 2's request and deny user 3's
        onView(withText("user2")).perform(clickChildViewWithId(R.id.acceptButton));
        onView(withText("user3")).perform(clickChildViewWithId(R.id.denyButton));
        Thread.sleep(500);
        // Make sure those requests go away.
        onView(withText("user2")).check(doesNotExist());
        onView(withText("user3")).check(doesNotExist());

        // Go to profile; make sure we have the right follower.
        onView(withId(R.id.navigation_profile)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        onView(withId(R.id.followers_count)).check(matches(withText("1"))).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof FollowersListFragment), 5, 500));
        onView(withText("user2")).check(matches(isDisplayed()));
        // Follow back user 2.
        onView(withText("Follow Back")).perform(click());
        // Back
        onView(withId(R.id.buttonBack)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
    }

    // Checks proper mood events are displayed.
    private void checkFollowedUserEvts() throws InterruptedException {
        // To following mood events page
        onView(withId(R.id.navigation_following)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof FollowingFragment), 5, 500));
        // Only displays the last 3 public evts of followed user
        onView(withText("u2E1")).check(doesNotExist());
        onView(withText("u2E2")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("u2E3")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("u2E4")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("u2E5")).check(doesNotExist());
        // Events of other users are not shown whatsoever
        onView(withText("u3E1")).check(doesNotExist());
        onView(withText("u3E2")).check(doesNotExist());
        onView(withText("u3E3")).check(doesNotExist());
        onView(withText("u3E4")).check(doesNotExist());
        onView(withText("u3E5")).check(doesNotExist());
    }

    // Checks whether removing the follower and unfollowing works.
    private void checkRemoveFlw() throws InterruptedException {
        // To profile then followers page
        onView(withId(R.id.navigation_profile)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        onView(withId(R.id.followers_count)).check(matches(withText("1"))).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof FollowersListFragment), 5, 500));
        // Remove follower user 2
        onView(withText("user2")).perform(clickChildViewWithId(R.id.remove_button));
        onView(withText("Yes")).perform(click());
        onView(withId(R.id.backButton)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        // Check: no more followers.
        onView(withId(R.id.followers_count)).check(matches(withText("0")));

        // Unfollow user 2
        onView(withId(R.id.following_count)).check(matches(withText("1"))).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof FollowingListFragment), 5, 500));
        // Unfollow user 2
        onView(withText("user2")).perform(clickChildViewWithId(R.id.following_button));
        onView(withText("Yes")).perform(click());
        onView(withId(R.id.buttonBack)).perform(click());
        assertTrue(waitUntil(scenario, (f) -> (f instanceof ProfileFragment), 5, 500));
        // Check: no more following.
        onView(withId(R.id.following_count)).check(matches(withText("0")));
    }

    /**
     * Reference: https://stackoverflow.com/questions/28476507/using-espresso-to-click-view-inside-recyclerview-item
     */
    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.getRootView().findViewById(id);
                v.performClick();
            }
        };
    }
}
