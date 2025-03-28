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
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FollowRequestsFragment extends Fragment {
    private RecyclerView requestsRecyclerView;
    private FollowRequestsAdapter requestsAdapter;
    private List<User> followRequests = new ArrayList<>();
    private Button accept_button;
    private Button deny_button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_requests, container, false);

        requestsRecyclerView = view.findViewById(R.id.requests_recycler_view);
        requestsAdapter = new FollowRequestsAdapter(followRequests,
                (user, isAccept) -> FollowingManager.handleFollowRequest(user.getDbFileId(), isAccept));
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
        String currentUserId = UserManager.getUserId();
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
        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, userHolder,
                isSuccess -> {
                    if (! isSuccess) {
                        if (getContext() != null)
                            Toast.makeText(getContext(), "Error loading follow requests", Toast.LENGTH_SHORT).show();
                    } else {
                        List<String> requestIds = userHolder.get().getFollowRequests();
                        if (requestIds != null && !requestIds.isEmpty()) {
                            fetchFollowRequestUsers(requestIds);
                        } else {
                            Toast.makeText(getContext(), "No follow requests", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchFollowRequestUsers(List<String> requestIds) {
        followRequests.clear();

        for (String userId : requestIds) {
            AtomicReference<User> userHolder = new AtomicReference<>();
            UserManager.loadUserData(userId, userHolder,
                    isSuccess -> {
                        if (! isSuccess) {
                            if (getContext() != null)
                                Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                        } else {
                            followRequests.add(userHolder.get());
                            requestsAdapter.updateRequests(followRequests);
                        }
                    });
        }
    }
}
