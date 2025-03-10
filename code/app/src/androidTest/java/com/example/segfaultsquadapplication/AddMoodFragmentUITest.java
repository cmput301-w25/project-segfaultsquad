package com.example.segfaultsquadapplication;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class AddMoodFragmentUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() throws InterruptedException {
        TestLoginUtil.handleSplashAndLogin(activityRule, "user1@gmail.com", "password");
        Thread.sleep(1000);
    }

    @Test
    public void testAddMoodUI() {
        onView(withId(R.id.fabAddMood)).perform(click());
        onView(withText("Confusion")).perform(click());
        onView(withId(R.id.editTextReason)).perform(ViewActions.typeText("Feeling Down"));


        onView(withId(R.id.editTextTrigger)).perform(ViewActions.scrollTo()).perform(ViewActions.typeText("Android Studio"));
        closeSoftKeyboard();

        onView(withId(R.id.spinnerSocialSituation)).perform(ViewActions.scrollTo()).perform(click());
        onView(withText("Alone")).perform(click());

        onView(withId(R.id.buttonConfirm)).perform(click());
    }
}
