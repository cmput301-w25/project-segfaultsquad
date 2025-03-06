package com.example.segfaultsquadapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;

import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.hamcrest.Matcher;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestLoginUtil {
    public static void handleSplashAndLogin(ActivityScenarioRule<MainActivity> scenario, String email, String pwd, int timeoutSec) throws InterruptedException {
        // Wait for splash
        AtomicBoolean shouldWait = new AtomicBoolean(true);
        System.out.println("Start handling splash and login");
        while (shouldWait.get()) {
            System.out.println("Still in splash");
            scenario.getScenario().onActivity(activity -> {
                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                if (! (navHost.getChildFragmentManager().getFragments().get(0) instanceof SplashFragment) )
                    shouldWait.set(false);
            });
            Thread.sleep(200);
        }
        // Find if login is needed
        shouldWait.set(true);
        scenario.getScenario().onActivity(activity -> {
                    NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                    System.out.println("Check for login frag");
                    if (! (navHost.getChildFragmentManager().getFragments().get(0) instanceof LoginFragment)) {
                        shouldWait.set(false);
                    }
                });
        // If no need to login
        if (! shouldWait.get())
            return;
        // Enter login info
        onView(withId(R.id.editTextEmail)).perform(ViewActions.typeText(email));
        onView(withId(R.id.editTextPassword)).perform(ViewActions.typeText(pwd));
        onView(withId(R.id.buttonLogin)).perform(ViewActions.click());
        System.out.println("Login clicked");
        // Timeout; NOTE: some data entering time should be reserved.
        for (int i = 0; i < timeoutSec; i ++) {
            int finalI = i;
            scenario.getScenario().onActivity(activity -> {
                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                System.out.println("Check for login frag - " + finalI + "/" + timeoutSec);
                if (! (navHost.getChildFragmentManager().getFragments().get(0) instanceof LoginFragment)) {
                    shouldWait.set(false);
                }
            });
            if (! shouldWait.get())
                break;
            Thread.sleep(1000);
        }
        // Final check: did we finish login?
        scenario.getScenario().onActivity(activity -> {
            NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHost.getChildFragmentManager().getFragments().get(0) instanceof LoginFragment) {
                throw new RuntimeException("Login failed");
            }
        });
    }
}
