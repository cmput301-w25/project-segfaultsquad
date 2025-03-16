package com.example.segfaultsquadapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertTrue;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.example.segfaultsquadapplication.display.LoginFragment;
import com.example.segfaultsquadapplication.display.SplashFragment;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Contains misc navigation features that can be used during fragment test
 */
public class TestLoginUtil {
    public static boolean waitUntil(ActivityScenarioRule<MainActivity> scenario,
                                    Predicate<Fragment> cond, int loopTimes, int loopIntervalMs) {
        AtomicBoolean ended = new AtomicBoolean(false);
        for (int i = 0; i <= loopTimes; i ++) {
            System.out.println("Waiting...");
            scenario.getScenario().onActivity(activity -> {
                NavHostFragment navHost = (NavHostFragment) activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                if (cond.test(navHost.getChildFragmentManager().getFragments().get(0)) )
                    ended.set(true);
            });
            try {
                Thread.sleep(loopIntervalMs);
            } catch (Exception ignored) {}
            if (ended.get())
                break;
        }
        return ended.get();
    }
    public static boolean isCurrFragInstanceof(ActivityScenarioRule<MainActivity> scenario, Class<? extends Fragment> cls) {
        return waitUntil(scenario, cls::isInstance, 1, 1 );
    }
    public static void handleSplashAndLogin(ActivityScenarioRule<MainActivity> scenario, String email, String pwd) throws InterruptedException {
        // Wait for splash
        assertTrue(waitUntil(scenario, (f) -> !(f instanceof SplashFragment), 20, 500));
        // Find if login is needed
        if (isCurrFragInstanceof(scenario, LoginFragment.class)) {
            // Enter login info
            onView(withId(R.id.editTextEmail)).perform(ViewActions.typeText(email));
            onView(withId(R.id.editTextPassword)).perform(ViewActions.typeText(pwd));
            onView(withId(R.id.buttonLogin)).perform(ViewActions.click());
            System.out.println("Login clicked");
            // Timeout; NOTE: some data entering time should be reserved.
            assertTrue( waitUntil(scenario, (f) -> !(f instanceof LoginFragment), 20, 500) );
        }
    }
}
