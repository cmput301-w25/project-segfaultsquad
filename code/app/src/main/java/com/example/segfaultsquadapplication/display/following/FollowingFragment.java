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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.AnimatorRes;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.display.moodhistory.MoodAdapter;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import androidx.navigation.Navigation;


/**
 * Classname: FollowingFragment
 * Purpose: Allow user to see a feed of their followed user's moods (most recent)
 * Current Issues: Its blank
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
public class FollowingFragment extends Fragment implements MoodAdapter.OnMoodClickListener {
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private RecyclerView followingRecyclerView;
    private MoodAdapter moodAdapter;
    private List<MoodEvent> allMoods = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following, container, false);

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

    /**
     * method to setup the list recycler view holder
     */
    private void setupRecyclerView() {
        moodAdapter = new MoodAdapter(this);
        followingRecyclerView.setAdapter(moodAdapter);
        followingRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    /**
     * loads all of the user's moods from firebase
     */
    private void loadMoods() {
        // debugging
        Log.d("FollowingFragment", "entered loadMoods()");

        String currentUserId = UserManager.getUserId();
        // debugging
        Log.d("FollowingFragment", "currentUserId: " + currentUserId);

        // Fetch the list of users that the current user follows
        AtomicReference<User> currUserHolder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, currUserHolder,
                isSuccess -> {
                    if (! isSuccess) return;
                    List<String> followingList = currUserHolder.get().getFollowing();
                    // debugging
                    Log.d("FollowingFragment", "followingList: " + followingList);
                    if (followingList != null && !followingList.isEmpty()) {
                        // Load the most recent moods of followed users
                        loadFollowedUsersMoods(followingList);
                    } else if (getContext() != null) {
                        Toast.makeText(getContext(), "You are not following anyone.", Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    /**
     * loads all of the user's followed moods from firebase
     * @param followingList
     * the list of the followed users
     */
    private void loadFollowedUsersMoods(List<String> followingList) {
        // debugging
        Log.d("FollowingFragment", "loadFollowedUsersMoods()");

        // Fetch the moods of followed users
        for (String userId : followingList) {
            ArrayList<MoodEvent> eventsHolder = new ArrayList<>(3);
            MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.PUBLIC_MOST_RECENT_3,
                    eventsHolder, isSuccess -> {
                        if (! isSuccess) {
                            if (getContext() != null)
                                Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
                        } else {
                            allMoods.addAll(eventsHolder);
                            // Sort the moods in memory
                            Collections.sort(allMoods, (a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
                            moodAdapter.updateMoods(allMoods);
                        }
                    });
        }
    }

    /**
     * listener to goto mood detals
     * @param mood
     * the mood event clicked on
     */
    @Override
    public void onMoodClick(MoodEvent mood) {
        Bundle args = new Bundle();
        args.putString("moodId", mood.getDbFileId());
        Navigation.findNavController(requireView())
                .navigate(R.id.navigation_mood_details, args);
    }

    /**
     * helper method to toggle visibility of filter menu
     */
    private void toggleFilterMenu() {
        isFilterMenuVisible = !isFilterMenuVisible;
        filterMenu.setVisibility(isFilterMenuVisible ? View.VISIBLE : View.GONE);
    }
}
