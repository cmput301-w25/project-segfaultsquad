/**
 * Classname: ProfileFragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication.display.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.following.Following;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import android.widget.ImageButton;
import androidx.cardview.widget.CardView;

public class ProfileFragment extends Fragment {

    private ImageView profilePicture;
    private TextView username;
    private TextView followersCount;
    private TextView followingCount;
    private RecyclerView recyclerViewMoodGrid;
    private MoodGridAdapter moodGridAdapter;
    private static int numFollower = 0, numFollowing = 0;

    private List<MoodEvent> moodEvents = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        profilePicture = view.findViewById(R.id.profile_picture);
        username = view.findViewById(R.id.username);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        recyclerViewMoodGrid = view.findViewById(R.id.recycler_view_mood_grid);
        ImageButton overflowButton = view.findViewById(R.id.overflowButton);
        CardView logoutDropdown = view.findViewById(R.id.logoutDropdown);
        TextView logoutOption = view.findViewById(R.id.logoutOption);

        followingCount.setText(String.valueOf(numFollowing));
        followersCount.setText(String.valueOf(numFollower));

        // Set user data
        setUserData();

        // Set up RecyclerView for mood grid first
        setupRecyclerView();

        // Load mood events
        loadMoodEvents();

        // Set click listeners
        followersCount.setOnClickListener(v -> navigateToFollowersList());
        followingCount.setOnClickListener(v -> navigateToFollowingList());

        // Toggle logout dropdown visibility
        overflowButton.setOnClickListener(v -> {
            if (logoutDropdown.getVisibility() == View.VISIBLE) {
                logoutDropdown.setVisibility(View.GONE);
            } else {
                logoutDropdown.setVisibility(View.VISIBLE);
            }
        });

        // Handle logout option click
        logoutOption.setOnClickListener(v -> {
            logoutUser();
            logoutDropdown.setVisibility(View.GONE); // Hide dropdown after logout
        });

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enable options menu
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile_menu, menu); // Inflate the menu
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser(); // Call the logout method
            return true;
        }
        return false;
    }

    private void setUserData() {
        // TODO: Replace with actual user data retrieval logic
        username.setText("John Doe");
        loadFollowerAndFollowingCounts();
        // Load profile picture if available
        // profilePicture.setImageBitmap(...);
    }

    private void loadFollowerAndFollowingCounts() {
        // Load followers count
        ArrayList<Following> followerHolder = new ArrayList<>();
        FollowingManager.getAllFollowed(DbUtils.getUserId(), followerHolder, isSuccess -> {
            if (isSuccess) {
                numFollower = followerHolder.size();
                followersCount.setText(String.valueOf(numFollower));
            } else {
                Toast.makeText(getContext(), "Failed to get number of followers", Toast.LENGTH_SHORT).show();
            }
        });
        // Load following count
        ArrayList<Following> followingHolder = new ArrayList<>();
        FollowingManager.getAllFollowing(DbUtils.getUserId(), followingHolder, isSuccess -> {
            if (isSuccess) {
                numFollowing = followingHolder.size();
                followingCount.setText(String.valueOf(numFollowing));
            } else {
                Toast.makeText(getContext(), "Failed to get number of followed users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        moodGridAdapter = new MoodGridAdapter(getContext(), moodEvents);
        recyclerViewMoodGrid.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns
        recyclerViewMoodGrid.setAdapter(moodGridAdapter);
    }

    private void loadMoodEvents() {
        ArrayList<MoodEvent> holder = new ArrayList<>();
        MoodEventManager.getAllMoodEvents(DbUtils.getUserId(), MoodEventManager.MoodEventFilter.ALL, holder, isSuccess -> {
            if (isSuccess) {
                moodEvents.clear();
                moodEvents.addAll(holder);
                // debugging
                Log.d("ProfileFragment", "Mood events count: " + moodEvents.size());
                moodGridAdapter.notifyDataSetChanged(); // Notify adapter of data change
            } else {
                Toast.makeText(getContext(), "Failed to get mood events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToFollowersList() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.action_to_followers_list); // Ensure this action exists in your nav_graph.xml
    }

    private void navigateToFollowingList() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.action_to_following_list); // Ensure this action exists in your nav_graph.xml
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut(); // Sign out the user
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Navigate back to the login screen
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.navigation_login); // Ensure this action exists in your nav_graph.xml
    }
}