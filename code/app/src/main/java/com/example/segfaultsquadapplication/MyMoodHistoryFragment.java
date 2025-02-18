package com.example.segfaultsquadapplication;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import androidx.navigation.Navigation;
import android.util.Log;

public class MyMoodHistoryFragment extends Fragment implements MoodAdapter.OnMoodClickListener {
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private RecyclerView moodRecyclerView;
    private MoodAdapter moodAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_mood_history, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        filterButton = view.findViewById(R.id.filterButton);
        filterMenu = view.findViewById(R.id.filterMenu);
        moodRecyclerView = view.findViewById(R.id.moodRecyclerView);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup filter button
        filterButton.setOnClickListener(v -> toggleFilterMenu());

        // Setup filter options
        setupFilterOptions(view);

        // Setup FAB
        view.findViewById(R.id.fabAddMood).setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_myMoodHistory_to_addMood));

        // Load moods
        loadMoods();

        return view;
    }

    private void setupRecyclerView() {
        moodAdapter = new MoodAdapter(this);
        moodRecyclerView.setAdapter(moodAdapter);
        moodRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupFilterOptions(View view) {
        view.findViewById(R.id.filter1).setOnClickListener(v -> {
            applyFilter("Last Week");
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter2).setOnClickListener(v -> {
            applyFilter("By Mood");
            toggleFilterMenu();
        });

        view.findViewById(R.id.filter3).setOnClickListener(v -> {
            applyFilter("By Reason");
            toggleFilterMenu();
        });
    }

    private void loadMoods() {
        String userId = auth.getCurrentUser().getUid();
        Log.d("MoodHistory", "Loading moods for user: " + userId);

        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MoodHistory", "Error loading moods", error);
                        Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<MoodEvent> moods = new ArrayList<>();
                    if (value != null) {
                        Log.d("MoodHistory", "Number of moods retrieved: " + value.size());
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                MoodEvent mood = doc.toObject(MoodEvent.class);
                                mood.setMoodId(doc.getId());
                                moods.add(mood);
                                Log.d("MoodHistory", "Loaded mood: " + mood.getMoodType() +
                                        " with ID: " + mood.getMoodId());
                            } catch (Exception e) {
                                Log.e("MoodHistory", "Error converting document: " + doc.getId(), e);
                            }
                        }

                        // Sort the moods in memory instead
                        Collections.sort(moods, (a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
                    }
                    moodAdapter.updateMoods(moods);
                });
    }

    @Override
    public void onMoodClick(MoodEvent mood) {
        // TODO: Navigate to mood details view
        Toast.makeText(getContext(), "Clicked: " + mood.getMoodType(), Toast.LENGTH_SHORT).show();
    }

    private void toggleFilterMenu() {
        isFilterMenuVisible = !isFilterMenuVisible;
        filterMenu.setVisibility(isFilterMenuVisible ? View.VISIBLE : View.GONE);
    }

    private void applyFilter(String filterType) {
        // TODO: Implement filtering logic
        switch (filterType) {
            case "Last Week":
                filterLastWeek();
                break;
            case "By Mood":
                showMoodFilterDialog();
                break;
            case "By Reason":
                showReasonFilterDialog();
                break;
        }
    }

    // TODO:
    // Filter implementation methods to be added later
    private void filterLastWeek() {
    }

    private void showMoodFilterDialog() {
    }

    private void showReasonFilterDialog() {
    }
}