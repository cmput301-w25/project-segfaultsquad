package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

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

                // Hide bottom navigation on login screen
                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    if (destination.getId() == R.id.navigation_login) {
                        bottomNavigationView.setVisibility(android.view.View.GONE);
                    } else {
                        bottomNavigationView.setVisibility(android.view.View.VISIBLE);
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        navController.navigate(item.getItemId());
        return true;
    }
}