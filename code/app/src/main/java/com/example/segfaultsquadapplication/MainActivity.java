/**
 * Classname: MainActivity
 * Version Info: Initial
 * Date: Feb 18, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.navigation.Navigation;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    // attributes
    private BottomNavigationView bottomNavigationView;
    private NavController navController;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Initialize the bottom navigation and NavController
        try {
            bottomNavigationView = findViewById(R.id.BottomNavBar);
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                NavigationUI.setupWithNavController(bottomNavigationView, navController);

                // Hide bottom navigation on login screen, moodanalyticsfragment and landing
                // screen and update menu
                // based on destination
                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    if (destination.getId() == R.id.navigation_login) {
                        bottomNavigationView.setVisibility(View.GONE);
                    } else if (destination.getId() == R.id.navigation_splash) {
                        bottomNavigationView.setVisibility(View.GONE);
                    } else if (destination.getId() == R.id.navigation_mood_analytics) {
                        // Current behavior preserved
                    } else if (destination.getId() == R.id.navigation_mood_details) {
                        bottomNavigationView.setVisibility(View.GONE); // Hide on MoodDetails page
                    } else if (destination.getId() == R.id.navigation_edit_mood) {
                        bottomNavigationView.setVisibility(View.GONE); // Hide on EditMood page
                    } else {
                        bottomNavigationView.setVisibility(View.VISIBLE);
                        updateBottomNavMenu(destination.getId());
                    }
                });
            } else {
                Log.e("MainActivity", "NavHostFragment is null, cannot initialize NavController");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing NavController: " + e.getMessage());
        }

        // Bottom navigation item selection
        bottomNavigationView.setOnItemSelectedListener(this);
    }

    /**
     * method to update the bottom nav bar based upon the destination
     * 
     * @param destinationId the id of the view/activity user is navigating to
     */
    private void updateBottomNavMenu(int destinationId) {
        bottomNavigationView.getMenu().clear(); // Clear existing menu items
        if (destinationId == R.id.navigation_my_mood_history) {
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_home);
        } else if (destinationId == R.id.navigation_map) {
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_map);
        } else if (destinationId == R.id.navigation_following) {
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_following);
        } else if (destinationId == R.id.navigation_follow_requests) {
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_requests);
        } else if (destinationId == R.id.navigation_profile) {
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_profile);
        } else if (destinationId == R.id.navigation_FollowersListFragment) {
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_profile); // im not going to make seperate
                                                                              // navigations for this... just have the
                                                                              // same as its parent profile fragment
        } else if (destinationId == R.id.navigation_FollowingListFragment) {
            bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu_profile); // im not going to make seperate
                                                                              // navigations for this... just have the
                                                                              // same as its parent profile fragment
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        navController.navigate(item.getItemId());
        return true;
    }
}