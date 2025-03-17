/**
 * Classname: FollowersListFragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication.display.following;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FollowersListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FollowersAdapter followersAdapter;
    private List<User> followersList; // Assume User is a model class for user data

    @SuppressLint("MissingInflatedId")
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

        return view;
    }

    private void loadFollowersData() {
        // debugging
        Log.d("FollowersListFragment", "Entered loadFollowersData()");

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // debugging
        Log.d("FollowersListFragment", "currentUserID: " + currentUserId);

        // Query where current user is the followedId
        db.collection("following")
                .whereEqualTo("followedId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    followersList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String followerId = document.getString("followerId");

                        // debugging
                        Log.d("FollowersListFragment", "followerId: " + followerId);

                        // Fetch the follower's user details
                        db.collection("users")
                                .document(followerId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User follower = userDoc.toObject(User.class);
                                        followersList.add(follower);
                                        followersAdapter.notifyDataSetChanged();
                                        // debugging
                                        Log.d("FollowersListFragment", "followers:" + followersList);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FollowersListFragment", "Error fetching follower details", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowersListFragment", "Error fetching followers", e);
                    Toast.makeText(getContext(), "Error loading followers", Toast.LENGTH_SHORT).show();
                });
    }

    private void onFollowerAction(User user) {
        // Check if the user is following
        FollowingManager.checkIfCurrentUserFollowing(user, isFollowing -> {
            if (isFollowing) {
                // Unfollow logic
                Toast.makeText(getContext(), "Unfollowed " + user.getUsername(), Toast.LENGTH_SHORT).show();
            } else {
                // Follow back logic
                Toast.makeText(getContext(), "Followed " + user.getUsername(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}