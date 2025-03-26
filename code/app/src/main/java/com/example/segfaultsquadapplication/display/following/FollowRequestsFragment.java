package com.example.segfaultsquadapplication.display.following;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FollowRequestsFragment extends Fragment {
    private RecyclerView requestsRecyclerView;
    private FollowRequestsAdapter requestsAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<User> followRequests = new ArrayList<>();
    private Button accept_button;
    private Button deny_button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_requests, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        requestsRecyclerView = view.findViewById(R.id.requests_recycler_view);
        requestsAdapter = new FollowRequestsAdapter(followRequests, this::handleFollowRequest);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsRecyclerView.setAdapter(requestsAdapter);

        accept_button = requestsRecyclerView.findViewById(R.id.acceptButton);
        deny_button = requestsRecyclerView.findViewById(R.id.denyButton);

        loadFollowRequests();

        return view;
    }

    private List<User> getDummyRequests() {
        List<User> dummyUsers = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setDbFileId("UserUser" + i);
            user.setUsername("UserUser2" + i);
            dummyUsers.add(user);
        }
        return dummyUsers;
    }

    private void loadFollowRequests() {
        String currentUserId = auth.getCurrentUser().getUid();
        Log.d("MoodAdapter", "Loading Requests");

//        //-- dummy follow requests for testing --//
//        List<User> dummyRequests = getDummyRequests();
//        if (!dummyRequests.isEmpty()) {
//            followRequests.clear();
//            followRequests.addAll(dummyRequests);
//            requestsAdapter.updateRequests(followRequests);
//        } else {
//            Toast.makeText(getContext(), "No follow requests", Toast.LENGTH_SHORT).show();
//        }
//        //-- dummy follow requests for testing --//

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
                                user.setDbFileId(documentSnapshot.getId()); //doc id for easy follow handling
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
            db.collection("users").document(currentUserId) //update current user followers profile
                    .update("followers", FieldValue.arrayUnion(user.getDbFileId()));
            db.collection("users").document(user.getDbFileId()) //update user that followed following profile
                    .update("following", FieldValue.arrayUnion(currentUserId));

            Map<String, Object> newFollow = new HashMap<>();
            newFollow.put("followerId", "yourFollowerId");
            newFollow.put("followedId", "yourFollowedId");
            newFollow.put("timestamp", FieldValue.serverTimestamp());

            db.collection("following")
                    .add(newFollow)  // Automatically generates a document ID
                    .addOnSuccessListener(documentReference -> {
                        Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                    })
                    .addOnFailureListener(e -> {
                        Log.w("Firestore", "Error adding document", e);
                    });

            db.collection("users").document(currentUserId) //remove user follow request list
                    .update("followRequests", FieldValue.arrayRemove(user.getDbFileId()));

            followRequests.remove(user); //update UI for the thing
            requestsAdapter.notifyItemRemoved(followRequests.indexOf(user));

            Toast.makeText(getContext(), user.getUsername() + " is now following you", Toast.LENGTH_SHORT).show();

        } else { //if denied
            db.collection("users").document(currentUserId)
                    .update("followRequests", FieldValue.arrayRemove(user.getDbFileId()));

            followRequests.remove(user);
            requestsAdapter.notifyItemRemoved(followRequests.indexOf(user));

            Toast.makeText(getContext(), user.getUsername() + "'s follow request denied", Toast.LENGTH_SHORT).show();
        }
    }
}
