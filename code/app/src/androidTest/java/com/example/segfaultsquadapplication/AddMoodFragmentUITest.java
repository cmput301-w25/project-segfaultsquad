package com.example.segfaultsquadapplication;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static java.util.regex.Pattern.matches;

import android.graphics.Movie;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * This test is to ensure that mood event is the UI for mood creation event works </br>
 * A seed database is mocked and used, it's also torn down at end of test
 */

@RunWith(AndroidJUnit4.class)
public class AddMoodFragmentUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Before
    public void setUp() throws InterruptedException {
        String androidLocalhost = "10.0.2.2";

        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {  // No user is logged in
            TestLoginUtil.handleSplashAndLogin(activityRule, "user1@gmail.com", "password");
            Thread.sleep(500);
        }
    }

    @Before //grow database, runs before each test
    public void seedDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference moodsRef = db.collection("moods");
    }

    @Test
    public void testAddMoodUI() throws InterruptedException {
        onView(withId(R.id.fabAddMood)).perform(click());
        onView(withText("Confusion")).perform(click());
        onView(withId(R.id.editTextReason)).perform(ViewActions.typeText("Feeling Down"));
        onView(withId(R.id.editTextTrigger)).perform(ViewActions.typeText("Android Studio"));

        onView(withId(R.id.scrollView)).perform(ViewActions.swipeUp());
        Thread.sleep(500);

        closeSoftKeyboard();

        onView(withId(R.id.spinnerSocialSituation)).perform(click());
        onView(withText("With One Person")).perform(click());

        onView(withId(R.id.buttonConfirm)).perform(click());
        Thread.sleep(500);

        onView(withText("CONFUSION")).check(ViewAssertions.matches(isDisplayed()));
        Thread.sleep(200);
    }

    @After
    public void tearDown() { //clear database
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
}
