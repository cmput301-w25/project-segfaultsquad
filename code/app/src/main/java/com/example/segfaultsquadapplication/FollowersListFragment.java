/**
 * Classname: FollowersListfragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FollowersListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FollowersAdapter followersAdapter;
    private List<User> followersList; // Assume User is a model class for user data

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

        // Load followers data (this should be replaced with actual data retrieval
        // logic)
        loadFollowersData();

        return view;
    }

    private void loadFollowersData() {
        // Mock data for demonstration
        followersList.add(new User("User1", "url_to_profile_picture", "user_email"));
        followersList.add(new User("User2", "url_to_profile_picture", "user_email"));
        followersAdapter.notifyDataSetChanged();
    }

    private void onFollowerAction(User user, boolean isFollowing) {
        // Handle follow/unfollow actions here
        if (isFollowing) {
            // Unfollow logic
        } else {
            // Follow back logic
        }
    }
}