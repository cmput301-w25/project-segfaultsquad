/**
 * Classname: FollowingListFragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 *
 * This fragment displays a list of users that the current user is following. It initializes
 * the RecyclerView and loads the following data from Firestore. The user can unfollow users
 * from this fragment.
 *
 * Outstanding Issues: None
 */

package com.example.segfaultsquadapplication.display.following;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment displays a list of users that the current user is following. It initializes the RecyclerView and loads the following data from Firestore. The user can unfollow users from this fragment.
 * Outstanding Issues: None
 */
public class FollowingListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FollowingAdapter followingAdapter;
    private List<User> followingList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following_list, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views and adapter
        recyclerView = view.findViewById(R.id.recycler_view_following);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        followingList = new ArrayList<>();
        followingAdapter = new FollowingAdapter(followingList, this::onFollowingAction);
        recyclerView.setAdapter(followingAdapter);

        // Load following data
        loadFollowingData();

        // Set up back button
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        return view;
    }

    /**
     * Loads the following data from Firestore.
     */
    private void loadFollowingData() {
        String currentUserId = auth.getCurrentUser().getUid();

        // Query the following collection for users that the current user follows
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> followedUserIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        followedUserIds.add(document.getString("followedId"));
                    }

                    // Now fetch the user details for each followed user
                    for (String userId : followedUserIds) {
                        db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User user = userDoc.toObject(User.class);
                                        // Set the user ID explicitly
                                        user.setDbFileId(userDoc.getId());
                                        followingList.add(user);
                                        followingAdapter.notifyDataSetChanged();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FollowingList", "Error fetching user details", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingList", "Error fetching following data", e);
                });
    }

    /**
     * Handles the action of following or unfollowing a user.
     *
     * @param user
     *             The user to follow or unfollow.
     */
    private void onFollowingAction(User user) {
        // Show confirmation dialog before unfollowing
        new AlertDialog.Builder(requireContext())
                .setTitle("Unfollow User")
                .setMessage("Are you sure you want to unfollow " + user.getUsername() + "?")
                .setPositiveButton("Yes", (dialog, which) -> unfollowUser(user))
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Unfollows the specified user.
     *
     * @param userToUnfollow The user to unfollow.
     */
    private void unfollowUser(User userToUnfollow) {
        Log.d("FollowingListFragment", "-0");
        String currentUserId = auth.getCurrentUser().getUid();
        String userToUnfollowId = userToUnfollow.getDbFileId();

        // Delete the following relationship from Firestore
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followedId", userToUnfollowId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    // remove the user from the list
                                    followingList.remove(userToUnfollow);
                                    followingAdapter.notifyDataSetChanged();
                                    Toast.makeText(getContext(),
                                            "Unfollowed " + userToUnfollow.getUsername(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                });

        // remove from user follower following
        db.collection("users").document(currentUserId)
                .update("following", FieldValue.arrayRemove(userToUnfollowId));

        db.collection("users").document(userToUnfollowId)
                .update("followers", FieldValue.arrayRemove(currentUserId));
    }
}