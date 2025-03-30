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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;

import java.util.ArrayList;
import java.util.List;
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
            public void onFollowBack(User user, FollowersAdapter.ViewHolder holder) {
                Log.d("FollowersListFragment", "Follow back clicked for: " + user.getUsername());

                sendFollowRequest(user, holder); //when button pressed
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
                    if (isSuccess) {
                        // Now fetch the user details for each followed user
                        for (String followingUserId : userHolder.get().getFollowers()) {
                            AtomicReference<User> flwUserHolder = new AtomicReference<>();
                            UserManager.loadUserData(followingUserId, flwUserHolder,
                                    isFlwSuccess -> {
                                        if (isFlwSuccess) {
                                            followersList.add(flwUserHolder.get());
                                            followersAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
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
        String currentUserId = UserManager.getUserId();
        FollowingManager.makeUnfollow(follower.getDbFileId(), currentUserId);
        followersList.remove(follower);
        followersAdapter.notifyDataSetChanged();
    }

    /**
     * method to send follow request if not already following
     *
     * @param userToFollow the other user
     *                 user id for the user to follow
     * @param holder view holder for adapter
     *                 check variables in adapter to change adatpter followback button
     */
    private void sendFollowRequest(User userToFollow, FollowersAdapter.ViewHolder holder) {
        FollowingManager.sendFollowRequest(userToFollow.getDbFileId(),
                isSuccess -> {
                    if (getContext() == null) return;
                    holder.updateFollowStatus(false, true);
                    String msg = isSuccess ? "Follow Request Sent" : "Could Not Send Follow Request";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                });
    }
}