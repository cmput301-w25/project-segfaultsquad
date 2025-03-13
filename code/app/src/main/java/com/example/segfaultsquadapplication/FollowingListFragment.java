/**
 * Classname: FollowingListFragment
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

public class FollowingListFragment extends Fragment {

    private RecyclerView recyclerView;
    private FollowingAdapter followingAdapter;
    private List<User> followingList; // Assume User is a model class for user data

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_following);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize the following list and adapter
        followingList = new ArrayList<>();
        followingAdapter = new FollowingAdapter(followingList, this::onFollowingAction);
        recyclerView.setAdapter(followingAdapter);

        // Load following data (this should be replaced with actual data retrieval
        // logic)
        loadFollowingData();

        // Set up back button
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> {
            requireActivity().onBackPressed(); // Navigate back to ProfileFragment
        });

        return view;
    }

    private void loadFollowingData() {
        // Mock data for demonstration
        followingList.add(new User("User3", "username_1", "user_email"));
        followingList.add(new User("User4", "username_2", "user_email"));
        followingAdapter.notifyDataSetChanged();
    }

    private void onFollowingAction(User user) {
        // Handle unfollow action here
    }
}