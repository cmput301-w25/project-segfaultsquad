package com.example.segfaultsquadapplication.display.following;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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
                (user, accept) -> handleFollowRequest(user, accept));
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

        AtomicReference<User> holder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, holder,
                isSuccess -> {
                    if (getContext() == null) return;
                    if (isSuccess) {
                        List<String> requestIds = holder.get().getFollowRequests();
                        if (requestIds != null && !requestIds.isEmpty()) {
                            fetchFollowRequestUsers(requestIds);
                        } else {
                            Toast.makeText(getContext(), "No follow requests", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error loading follow requests", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchFollowRequestUsers(List<String> requestIds) {
        followRequests.clear();

        for (String userId : requestIds) {
            AtomicReference<User> holder = new AtomicReference<>();
            UserManager.loadUserData(userId, holder,
                    isSuccess -> {
                        if (getContext() == null) return;
                        if (isSuccess) {
                            followRequests.add(holder.get());
                            requestsAdapter.updateRequests(followRequests);
                        } else {
                            Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void handleFollowRequest(User user, boolean accept) {
        int idx = followRequests.indexOf(user);
        if (idx != -1) {
            FollowingManager.handleFollowRequest(user.getDbFileId(), accept);
            followRequests.remove(user); //update UI for the thing
            requestsAdapter.notifyItemRemoved(idx);
        }
    }
}