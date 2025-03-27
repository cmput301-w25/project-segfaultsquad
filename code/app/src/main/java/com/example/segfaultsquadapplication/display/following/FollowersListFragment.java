/**
 * Classname: FollowersListFragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication.display.following;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;

public class FollowersListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FollowersAdapter followersAdapter;
    private List<User> followersList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private boolean isFollowing = false; //if current user following user
    private boolean followRequestSent = false; //if current user sent request to  user

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_followers_list, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views and adapter
        recyclerView = view.findViewById(R.id.recycler_view_followers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        followersList = new ArrayList<>();
        followersAdapter = new FollowersAdapter(followersList, new FollowersAdapter.OnFollowerClickListener() {
            @Override
            public void onRemoveFollower(User user) {
                Log.d("FollowersListFragment", "Remove follower clicked for: " + user.getUsername());
                showRemoveFollowerDialog(user);
            }

            @Override
            public void onFollowBack(User user) {
                Log.d("FollowersListFragment", "Follow back clicked for: " + user.getUsername());
                checkIfFollowing(user);
            }
        });
        recyclerView.setAdapter(followersAdapter);

        // Load followers data
        loadFollowersData();

        // Set up back button
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        return view;
    }

    private void loadFollowersData() {
        String currentUserId = auth.getCurrentUser().getUid();

        // Add debug logging
        Log.d("FollowersListFragment", "Loading followers for user: " + currentUserId);

        db.collection("following")
                .whereEqualTo("followedId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Debug log the number of followers found
                    Log.d("FollowersListFragment", "Found " + querySnapshot.size() + " followers");

                    List<String> followerUserIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String followerId = document.getString("followerId");
                        followerUserIds.add(followerId);
                        // Debug log each follower ID
                        Log.d("FollowersListFragment", "Found follower with ID: " + followerId);
                    }

                    // Now fetch the user details for each follower
                    for (String userId : followerUserIds) {
                        db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        // Set the user ID explicitly
                                        user.setDbFileId(userDoc.getId());
                                        // Debug log the user details
                                        Log.d("FollowersListFragment", "Loaded follower: " + user.getUsername()
                                                + " with ID: " + user.getDbFileId());
                                        followersList.add(user);
                                        followersAdapter.notifyDataSetChanged();
                                    } else {
                                        Log.e("FollowersListFragment", "User document doesn't exist for ID: " + userId);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FollowersListFragment", "Error fetching user details for ID: " + userId, e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowersListFragment", "Error fetching followers data", e);
                });
    }

    /**
     * method to check if the current user is following a given (other) user
     *
     * @param follower the other useer
     *                 the index position other this other user (User obj) in the
     *                 current user's followers list
     */
    private void checkIfFollowing(User follower) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followedId", follower.getDbFileId())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean isFollowing = !querySnapshot.isEmpty();
                    if (!isFollowing) {
                        sendFollowRequest(follower);
                    } else {
//                        updateFollowBackButton();
                    }
                });
    }

    private void showRemoveFollowerDialog(User follower) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Follower")
                .setMessage("Are you sure you want to remove " + follower.getUsername() + " as a follower?")
                .setPositiveButton("Yes", (dialog, which) -> removeFollower(follower))
                .setNegativeButton("No", null)
                .show();
    }

    private void removeFollower(User follower) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Delete the following relationship from Firestore
        db.collection("following")
                .whereEqualTo("followerId", follower.getDbFileId())
                .whereEqualTo("followedId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    followersList.remove(follower);
                                    followersAdapter.notifyDataSetChanged();
                                    Toast.makeText(getContext(),
                                            "Removed " + follower.getUsername() + " from followers",
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

//    private void updateFollowBackButton() {
//        if (isFollowing) {
//            followBackButton.setText("Following");
//            followBackButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), com.google.android.material.R.color.button_material_dark));
//        }
//        if (followRequestSent) {
//            followBackButton.setText("Requested to Follow");
//            followBackButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), com.google.android.material.R.color.button_material_dark));
//        }
//    }

    private void sendFollowRequest(User userToFollow) {
        String currentUserId = auth.getCurrentUser().getUid();
        if (!isFollowing) {
            db.collection("users").document(userToFollow.getDbFileId()) //update current user followers profile
                    .update("followRequests", FieldValue.arrayUnion(currentUserId));
            followRequestSent = true;
//            updateFollowBackButton();
            Toast.makeText(getContext(), "Follow Request Sent", Toast.LENGTH_SHORT).show();
        }
    }
}