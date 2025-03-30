/**
 * Classname: FollowingFragment
 * Purpose: Allow user to see a feed of their followed user's moods (most recent)
 * Current Issues: Its blank
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.display.following;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.display.moodhistory.MoodAdapter;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FollowingFragment extends Fragment implements MoodAdapter.OnMoodClickListener {
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private RecyclerView followingRecyclerView;
    private MoodAdapter moodAdapter;
    // Make it non-static, init as empty; load moods as needed.
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

        // Display moods of followed users
        loadMoods();

        // Setup filter options
        setupFilterOptions(view);

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

    private void loadFollowedUsersMoods(List<String> followingList) {
        // debugging
        Log.d("FollowingFragment", "loadFollowedUsersMoods()");

        // Fetch the moods of followed users
        allMoods.clear();
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

    @Override
    public void onMoodClick(MoodEvent mood) {
        Bundle args = new Bundle();
        args.putString("moodId", mood.getDbFileId());
        args.putString("userId", mood.getUserId());
        Navigation.findNavController(requireView())
                .navigate(R.id.navigation_mood_details, args);
    }

    private void toggleFilterMenu() {
        isFilterMenuVisible = !isFilterMenuVisible;
        filterMenu.setVisibility(isFilterMenuVisible ? View.VISIBLE : View.GONE);
    }

    private void setupFilterOptions(View view) {
        view.findViewById(R.id.filter1).setOnClickListener(v -> {
            filterLastWeek();
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter2).setOnClickListener(v -> {
            showMoodFilterDialog();
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter3).setOnClickListener(v -> {
            showReasonFilterDialog();
            toggleFilterMenu();
        });

        view.findViewById(R.id.clearFilters).setOnClickListener(v -> {
            clearAllFilters();
            toggleFilterMenu();
        });
    }

    private void filterLastWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, -1);
        Date lastWeekDate = calendar.getTime();

        List<MoodEvent> filteredMoods = new ArrayList<>();
        for (MoodEvent mood : allMoods) {
            if (mood.getTimestamp().toDate().after(lastWeekDate)) {
                filteredMoods.add(mood);
            }
        }
        moodAdapter.updateMoods(filteredMoods);
    }

    private void showMoodFilterDialog() {
        String[] moods = { "ANGER", "CONFUSION", "DISGUST", "FEAR", "HAPPINESS", "SADNESS", "SHAME", "SURPRISE" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Mood")
                .setItems(moods, (dialog, which) -> {
                    String selectedMood = moods[which];
                    filterByMood(selectedMood);
                });
        builder.show();
    }

    private void filterByMood(String moodType) {
        List<MoodEvent> filteredMoods = new ArrayList<>();
        for (MoodEvent mood : allMoods) {
            if (mood.getMoodType().name().equals(moodType)) {
                filteredMoods.add(mood);
            }
        }
        moodAdapter.updateMoods(filteredMoods);
    }

    private void showReasonFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Reason Keyword");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText input = new EditText(getContext());
        input.setHint("Type search word here...");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 0, 40, 0);
        input.setLayoutParams(params);
        layout.addView(input);

        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String keyword = input.getText().toString();
            filterByReason(keyword);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void filterByReason(String keyword) {
        List<MoodEvent> filteredMoods = new ArrayList<>();
        for (MoodEvent mood : allMoods) {
            if (mood.getReasonText() != null &&
                    mood.getReasonText().toLowerCase().contains(keyword.toLowerCase())) {
                filteredMoods.add(mood);
            }
        }
        moodAdapter.updateMoods(filteredMoods);
    }

    private void clearAllFilters() {
        moodAdapter.updateMoods(allMoods);
    }
}
