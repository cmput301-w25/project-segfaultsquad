package com.example.segfaultsquadapplication.display.following;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FollowRequestsFragment extends Fragment {
    private RecyclerView requestsRecyclerView;
    private FollowRequestsAdapter requestsAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<User> followRequests = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_requests, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        requestsRecyclerView = view.findViewById(R.id.requests_recycler_view);
        requestsAdapter = new FollowRequestsAdapter(followRequests, this::handleFollowRequest);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsRecyclerView.setAdapter(requestsAdapter);

        loadFollowRequests();

        return view;
    }

    private void loadFollowRequests() {
        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            List<String> requestIds = currentUser.getFollowRequests();
                            if (requestIds != null && !requestIds.isEmpty()) {
                                fetchFollowRequestUsers(requestIds);
                            } else {
                                Toast.makeText(getContext(), "No follow requests", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading follow requests", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchFollowRequestUsers(List<String> requestIds) {
        followRequests.clear();
        for (String userId : requestIds) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                followRequests.add(user);
                                requestsAdapter.updateRequests(followRequests);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void handleFollowRequest(User user, boolean accept) {
        String currentUserId = auth.getCurrentUser().getUid();
        if (accept) {
            // Add to followers and following lists
            db.collection("users").document(currentUserId)
                    .update("followers", FieldValue.arrayUnion(user.getDbFileId()));
            db.collection("users").document(user.getDbFileId())
                    .update("following", FieldValue.arrayUnion(currentUserId));
        } else {
            // Remove the follow request
            db.collection("users").document(currentUserId)
                    .update("followRequests", FieldValue.arrayRemove(user.getDbFileId()));
        }
    }
}
