/**
 * Classname: FollowingListFragment
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.FollowingManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FollowingListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FollowingAdapter followingAdapter;
    private List<User> followingList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following_list, container, false);

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

    private void loadFollowingData() {
        String currentUserId = UserManager.getUserId();

        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, userHolder,
                isSuccess -> {
                    if (isSuccess) {
                        // Now fetch the user details for each followed user
                        for (String followedUserId : userHolder.get().getFollowing()) {
                            AtomicReference<User> flwUserHolder = new AtomicReference<>();
                            UserManager.loadUserData(followedUserId, flwUserHolder,
                                    isFlwSuccess -> {
                                        if (isFlwSuccess) {
                                            followingList.add(flwUserHolder.get());
                                            followingAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    }
                });
    }

    private void onFollowingAction(User user) {
        // Show confirmation dialog before unfollowing
        new AlertDialog.Builder(requireContext())
                .setTitle("Unfollow User")
                .setMessage("Are you sure you want to unfollow " + user.getUsername() + "?")
                .setPositiveButton("Yes", (dialog, which) -> unfollowUser(user))
                .setNegativeButton("No", null)
                .show();
    }

    private void unfollowUser(User userToUnfollow) {
        Log.d("FollowingListFragment", "-0");
        String currentUserId = UserManager.getUserId();
        String userToUnfollowId = userToUnfollow.getDbFileId();

        // Delete the following relationship from Firestore
        FollowingManager.makeUnfollow(currentUserId, userToUnfollowId);
        followingList.remove(userToUnfollow);
        followingAdapter.notifyDataSetChanged();
    }
}