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

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.firestore.*;

import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;


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
//        CollectionReference moviesRef = db.collection("movies");
//        Movie[] movies = {
//                new Movie("Oppenheimer", "Thriller/Historical Drama", 2023),
//                new Movie("Barbie", "Comedy/Fantasy", 2023)
//        };
//
//        for (Movie movie : movies) {
//            DocumentReference docRef = moviesRef.document();
//            movie.setId(docRef.getId());
//            docRef.set(movie);
//        }
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

        // Test for regular new moods
        testNewMoodRegular();
        // Sleep the thread for now to better see the result
        Thread.sleep(100000);
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
}
