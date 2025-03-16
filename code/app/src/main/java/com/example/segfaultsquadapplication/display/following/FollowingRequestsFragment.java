/**
 * Classname: FollowingRequestFragment
 * Purpose: Allow user to look at other user profiles and request to follow them
 * Current Issues: N/A
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.display.following;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FollowingRequestsFragment extends Fragment {
    private RecyclerView requestsRecyclerView;
    private FollowRequestsAdapter requestsAdapter;
    private List<User> followRequests = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_follow_requests, container, false);

        requestsRecyclerView = view.findViewById(R.id.requests_recycler_view);
        requestsAdapter = new FollowRequestsAdapter(followRequests, FollowingManager::handleFollowRequest);
        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestsRecyclerView.setAdapter(requestsAdapter);

        loadFollowRequests();

        return view;
    }

    private void loadFollowRequests() {
        AtomicReference<User> holder = new AtomicReference<>();
        DbUtils.getObjectByDocId(DbUtils.COLL_USERS, DbUtils.getUserId(), User.class, holder,
                new DbOpResultHandler<>(
                        result -> {
                            List<String> requestIds = holder.get().getFollowRequests();
                            if (requestIds != null && !requestIds.isEmpty()) {
                                fetchFollowRequestUsers(requestIds);
                            } else {
                                Toast.makeText(getContext(), "No follow requests", Toast.LENGTH_SHORT).show();
                            }
                        },
                        e -> Toast.makeText(getContext(), "Could not get user info", Toast.LENGTH_SHORT).show()));
    }

    private void fetchFollowRequestUsers(List<String> requestIds) {
        followRequests.clear();
        for (String userId : requestIds) {
            AtomicReference<User> holder = new AtomicReference<>();
            DbUtils.getObjectByDocId(DbUtils.COLL_USERS, userId, User.class, holder,
                    new DbOpResultHandler<>(
                            result -> {
                                followRequests.add(holder.get());
                                requestsAdapter.updateRequests(followRequests);
                            },
                            e -> Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show()));
        }
    }


}
