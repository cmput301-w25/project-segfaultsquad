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

import androidx.annotation.AnimatorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FollowersListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FollowersAdapter followersAdapter;
    private List<User> followersList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_followers_list, container, false);

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
                FollowingManager.makeFollow(UserManager.getUserId(), user.getDbFileId());
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
        String currentUserId = UserManager.getUserId();

        // Add debug logging
        Log.d("FollowersListFragment", "Loading followers for user: " + currentUserId);

        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, userHolder,
                isSuccess -> {
                    if (! isSuccess) return;
                    List<String> followerUserIds = userHolder.get().getFollowers();
                    // Debug log each follower ID
                    followerUserIds.forEach( flw -> Log.d("FollowersListFragment", "Found follower with ID: " + flw) );

                    // Now fetch the user details for each follower
                    for (String followerUserId : followerUserIds) {
                        AtomicReference<User> userDetailHolder = new AtomicReference<>();
                        UserManager.loadUserData(followerUserId, userDetailHolder,
                                isFlwSuccess -> {
                                    if (! isFlwSuccess) return;
                                    User user = userDetailHolder.get();
                                    // Debug log the user details
                                    Log.d("FollowersListFragment", "Loaded follower: " + user.getUsername() + " with ID: " + user.getDbFileId());
                                    followersList.add(user);
                                    followersAdapter.notifyDataSetChanged();
                                });
                    }
                });
    }

    private void showRemoveFollowerDialog(User follower) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Follower")
                .setMessage("Are you sure you want to remove " + follower.getUsername() + " as a follower?")
                .setPositiveButton("Yes", (dialog, which) -> FollowingManager.removeFollower(follower.getDbFileId()))
                .setNegativeButton("No", null)
                .show();
    }
}