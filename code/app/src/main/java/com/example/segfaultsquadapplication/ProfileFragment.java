/**
 * Classname: ProfileFragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private ImageView profilePicture;
    private TextView username;
    private TextView followersCount;
    private TextView followingCount;
    private RecyclerView recyclerViewRecentActivity;
    private RecentActivityAdapter recentActivityAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<MoodEvent> recentActivities = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profilePicture = view.findViewById(R.id.profile_picture);
        username = view.findViewById(R.id.username);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        recyclerViewRecentActivity = view.findViewById(R.id.recycler_view_recent_activity);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set user data
        setUserData();

        // Set up RecyclerView for recent activity
        setupRecyclerView();

        // Load recent activity
        loadRecentActivity();

        // Set click listeners
        followersCount.setOnClickListener(v -> navigateToFollowersList());
        followingCount.setOnClickListener(v -> navigateToFollowingList());

        return view;
    }

    private void setUserData() {
        // Replace with actual user data retrieval logic
        username.setText("John Doe");
        loadFollowerAndFollowingCounts();
        // Load profile picture if available
        // profilePicture.setImageBitmap(...);
    }

    private void loadFollowerAndFollowingCounts() {
        String userId = auth.getCurrentUser().getUid(); // Get user ID

        // Load followers count
        db.collection("followers")
                .whereEqualTo("followedId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int followers = queryDocumentSnapshots.size();
                    followersCount.setText(String.valueOf(followers));
                });

        // Load following count
        db.collection("following")
                .whereEqualTo("followerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int following = queryDocumentSnapshots.size();
                    followingCount.setText(String.valueOf(following));
                });
    }

    private void setupRecyclerView() {
        recentActivityAdapter = new RecentActivityAdapter(recentActivities);
        recyclerViewRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewRecentActivity.setAdapter(recentActivityAdapter);
    }

    private void loadRecentActivity() {
        String userId = auth.getCurrentUser().getUid(); // Get user ID

        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10) // Get the most recent 10 moods of followed people
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    recentActivities.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MoodEvent mood = doc.toObject(MoodEvent.class);
                        recentActivities.add(mood);
                    }
                    recentActivityAdapter.notifyDataSetChanged();
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
}