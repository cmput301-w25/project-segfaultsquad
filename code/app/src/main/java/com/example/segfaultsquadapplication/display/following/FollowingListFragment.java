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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
        // debugging
        Log.d("FollowingListFragment", "Entered loadFollowingData()");

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // debugging
        Log.d("FollowingListFragment", "currentUserID: " + currentUserId);

        // Query where current user is the followerId
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    followingList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String followedId = document.getString("followedId");
                        // debugging
                        Log.d("FollowingListFragment", "followedId: " + followedId);

                        // Fetch the followed user's details
                        db.collection("users")
                                .document(followedId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        User following = userDoc.toObject(User.class);
                                        followingList.add(following);
                                        followingAdapter.notifyDataSetChanged();
                                        // debugging
                                        Log.d("FollowingListFragment", "following:" + followingList);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FollowingListFragment", "Error fetching following user details", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingListFragment", "Error fetching following list", e);
                    Toast.makeText(getContext(), "Error loading following list", Toast.LENGTH_SHORT).show();
                });

    }

    private void onFollowingAction(User user) {
        // Handle unfollow action here
    }
}