package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private NavController navController;

    // Filter menu
    private ImageButton filterButton;
    private CardView filterMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the bottom navigation and NavController
        try {
            bottomNavigationView = findViewById(R.id.BottomNavBar);
            // Get the NavHostFragment and initialize the NavController
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                NavigationUI.setupWithNavController(bottomNavigationView, navController);
            } else {
                Log.e("MainActivity", "NavHostFragment is null, cannot initialize NavController");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing NavController: " + e.getMessage());
        }

        // debugging
        if (bottomNavigationView != null) {
            Log.d("MainActivity", "BottomNavigationView is initialized");
        } else {
            Log.e("MainActivity", "BottomNavigationView is null");
        }


        // Filter button and menu setup
        filterButton = findViewById(R.id.filterButton);
        filterMenu = findViewById(R.id.filterMenu);

        filterButton.setOnClickListener(v -> {
            filterMenu.setVisibility(filterMenu.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        // Filter options handling
        TextView filter1 = findViewById(R.id.filter1);
        filter1.setOnClickListener(v -> {
            // Handle filter 1
            filterMenu.setVisibility(View.GONE);  // Hide menu after selection
        });

        TextView filter2 = findViewById(R.id.filter2);
        filter2.setOnClickListener(v -> {
            // Handle filter 2
            filterMenu.setVisibility(View.GONE);  // Hide menu after selection
        });

        TextView filter3 = findViewById(R.id.filter3);
        filter3.setOnClickListener(v -> {
            // Handle filter 3
            filterMenu.setVisibility(View.GONE);  // Hide menu after selection
        });

        // Bottom navigation item selection
        bottomNavigationView.setOnItemSelectedListener(this);

        // debugging
        Log.d("MainActivity", "Reached END of MainActivity onCreate()");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Use NavController to navigate to the selected item
        navController.navigate(item.getItemId());
        return true;
    }

}
