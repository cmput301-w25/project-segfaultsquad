/**
 * Classname: FollowersListFragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FollowersListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FollowersAdapter followersAdapter;
    private List<User> followersList; // Assume User is a model class for user data
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_followers_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_followers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the followers list and adapter
        followersList = new ArrayList<>();
        followersAdapter = new FollowersAdapter(followersList, this::onFollowerAction);
        recyclerView.setAdapter(followersAdapter);

        // Load followers data
        loadFollowersData();

        // Set up back button
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> {
            requireActivity().onBackPressed(); // Navigate back to ProfileFragment
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        return view;
    }

    private void loadFollowersData() {
        // Mock data for demonstration
        followersList.add(new User("User1", "username_1", "user_email_1"));
        followersList.add(new User("User2", "username_2", "user_email_2"));
        followersAdapter.notifyDataSetChanged();
    }

    private void onFollowerAction(User user) {
        // Check if the user is following
        checkIfFollowing(user, isFollowing -> {
            if (isFollowing) {
                // Unfollow logic
                Toast.makeText(getContext(), "Unfollowed " + user.getUsername(), Toast.LENGTH_SHORT).show();
            } else {
                // Follow back logic
                Toast.makeText(getContext(), "Followed " + user.getUsername(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkIfFollowing(User user, FollowingCheckCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user ID

        // Query Firestore to check if the current user is following the specified user
        db.collection("followers")
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followedId", user.getUserId()) // Assuming User has a method to get user ID
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isFollowing = !task.getResult().isEmpty(); // User is following if the result is not
                                                                           // empty
                        callback.onResult(isFollowing); // Return the result via callback
                    } else {
                        Log.e("FollowersListFragment", "Error checking following status: " + task.getException());
                        callback.onResult(false); // Default to false on error
                    }
                });
    }

    // Callback interface for checking following status
    public interface FollowingCheckCallback {
        void onResult(boolean isFollowing);
    }
}