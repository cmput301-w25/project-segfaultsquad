/**
 * Classname: FollowingFragment
 * Purpose: Allow user to see a feed of their followed user's moods (most recent)
 * Current Issues: Its blank
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.display.following;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.display.moodhistory.MoodAdapter;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import androidx.navigation.Navigation;

public class FollowingFragment extends Fragment implements MoodAdapter.OnMoodClickListener {
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private RecyclerView followingRecyclerView;
    private MoodAdapter moodAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<MoodEvent> allMoods = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        filterButton = view.findViewById(R.id.filterButton);
        filterMenu = view.findViewById(R.id.filterMenu);
        followingRecyclerView = view.findViewById(R.id.followingRecyclerView);

        // Setup RecyclerView
        setupRecyclerView();

        // Load moods of followed users
        loadMoods();

        // Setup filter button
        filterButton.setOnClickListener(v -> toggleFilterMenu());

        return view;
    }

    private void setupRecyclerView() {
        moodAdapter = new MoodAdapter(this);
        followingRecyclerView.setAdapter(moodAdapter);
        followingRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void loadMoods() {
        String currentUserId = auth.getCurrentUser().getUid(); // Get current user ID

        // Fetch the list of users that the current user follows
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            List<String> followingList = currentUser.getFollowing(); // Get the following list
                            if (followingList != null && !followingList.isEmpty()) {
                                // Load the most recent moods of followed users
                                loadFollowedUsersMoods(followingList);
                            } else {
                                Toast.makeText(getContext(), "You are not following anyone.", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadFollowedUsersMoods(List<String> followingList) {
        // Fetch the moods of followed users
        for (String userId : followingList) {
            db.collection("moods")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(3) // Get the 3 most recent moods
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            MoodEvent mood = doc.toObject(MoodEvent.class);
                            mood.setDbFileId(doc.getId());
                            allMoods.add(mood);
                        }
                        // Sort the moods in memory
                        Collections.sort(allMoods, (a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
                        moodAdapter.updateMoods(allMoods);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onMoodClick(MoodEvent mood) {
        Bundle args = new Bundle();
        args.putString("moodId", mood.getDbFileId());
        Navigation.findNavController(requireView())
                .navigate(R.id.navigation_mood_details, args);
    }

    private void toggleFilterMenu() {
        isFilterMenuVisible = !isFilterMenuVisible;
        filterMenu.setVisibility(isFilterMenuVisible ? View.VISIBLE : View.GONE);
    }
}
