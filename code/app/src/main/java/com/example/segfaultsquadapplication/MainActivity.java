/**
 * Classname: MainActivity
 * Version Info: Initial
 * Date: Feb 18, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/**
 * MainActivity for the SegFault Squad application. This activity manages the bottom navigation bar
 * and handles the navigation between different fragments using the Android Navigation component
 * The activity updates the visibility and styling of the bottom navigation bar based on the
 * destination in the nav graph.
 */
public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    // attributes
    private BottomNavigationView bottomNavigationView;
    private NavController navController = null;

    /**
     * when the activity is first created. Sets up the navigation controller and bottom nav
     * @param savedInstanceState saved instance state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the bottom navigation and NavController
        setupNavController();

        getBottomNavigationView().setLabelVisibilityMode(
                NavigationBarView.LABEL_VISIBILITY_UNLABELED);

        updateBottomNavMenu(R.id.navigation_splash);

        // Bottom navigation item selection
        getBottomNavigationView().setOnItemSelectedListener(this);
    }

    /**
     * method to update the bottom nav bar based upon the destination
     * @param destinationId the id of the view/activity user is navigating to
     */
    private void updateBottomNavMenu(int destinationId) {
        // During these phase, the menu is invisible.
        if (destinationId == R.id.navigation_login ||
                destinationId == R.id.navigation_splash ||
                destinationId == R.id.navigation_add_mood ||
                destinationId == R.id.navigation_edit_mood ||
                destinationId == R.id.navigation_mood_details) {
            getBottomNavigationView().setVisibility(View.GONE);
            return;
        }
        getBottomNavigationView().setVisibility(View.VISIBLE);
        // Distinguish the selected button
        int itemId = destinationId;
        // These fragments use the same as its parent, the profile fragment
        if (destinationId == R.id.navigation_FollowersListFragment ||
                destinationId == R.id.navigation_FollowingListFragment) {
            itemId = R.id.navigation_profile;
        }

        // Reset background color for all items
        BottomNavigationView bottomNavigationView = getBottomNavigationView();
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            MenuItem menuItem = bottomNavigationView.getMenu().getItem(i);
            // Reset the background color (for inactive items)
            View itemView = bottomNavigationView.findViewById(menuItem.getItemId());
            if (itemView != null) {
                // reset bgcolor to transparent
                itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }
        }

        View item = getBottomNavigationView().findViewById(itemId);
        if (item != null) {
            item.setBackgroundColor(getResources().getColor(R.color.TopHeaderBackground));
        }
    }

    /**
     * when item in the bottom navigation view is selected
     * @param item The selected menu item
     * @return true if the item was handled, false otherwise
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        getNavController().navigate(item.getItemId());
        return true;
    }

    /**
     * Retrieves the BottomNavigationView.
     * @return The BottomNavigationView instance.
     */
    private BottomNavigationView getBottomNavigationView() {
        // Try to fetch the bottomNavigationView.
        if (bottomNavigationView == null) {
            bottomNavigationView = findViewById(R.id.BottomNavBar);
        }
        return bottomNavigationView;
    }

    /**
     * Retrieves NavController responsible for managing navigation
     * @return The NavController instance.
     */
    private NavController getNavController() {
        // Try to fetch the nav controller.
        if (navController == null) {
            setupNavController();
        }
        return navController;
    }

    /**
     * Initializes the NavController and sets up the navigation view
     * and establish navigation between fragments
     */
    private void setupNavController() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        // Initialized; setup navigation view as well.
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(getBottomNavigationView(), navController);
            // Register the
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                updateBottomNavMenu(destination.getId());
            });
        }
    }
}