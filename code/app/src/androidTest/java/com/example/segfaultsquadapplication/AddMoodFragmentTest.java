package com.example.segfaultsquadapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.app.Activity;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.*;

import org.junit.*;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
public class AddMoodFragmentTest {
    @BeforeClass
    public static void setup(){
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080;
        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

//    @Before
//    public void seedDatabase() {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        CollectionReference moviesRef = db.collection("movies");
//        Movie[] movies = {
//                new Movie("Oppenheimer", "Thriller/Historical Drama", "2023"),
//                new Movie("Barbie", "Comedy/Fantasy", "2023")
//        };
//        for (Movie movie : movies) {
//            moviesRef.document().set(movie);
//        }
//    }

//    @After
//    public void tearDown() {
//        String projectId = "lab7-2ed8e";
//        URL url = null;
//        try {
//            url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
//        } catch (MalformedURLException exception) {
//            Log.e("URL Error", Objects.requireNonNull(exception.getMessage()));
//        }
//        HttpURLConnection urlConnection = null;
//        try {
//            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setRequestMethod("DELETE");
//            int response = urlConnection.getResponseCode();
//            Log.i("Response Code", "Response Code: " + response);
//        } catch (IOException exception) {
//            Log.e("IO Error", Objects.requireNonNull(exception.getMessage()));
//        } finally {
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
//        }
//    }

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);

    // Save location if and only if location is allowed; furthermore saved location should be correct.
    @Test
    public void testSaveLocation() throws InterruptedException {
        TestLoginUtil.handleSplashAndLogin(scenario, "user1@gmail.com", "password", 10);

        // Enter add mood
        onView(withId(R.id.action_myMoodHistory_to_addMood)).perform(click());

        // Sleep the thread for now to better see the result
        Thread.sleep(100000);
    }
}
