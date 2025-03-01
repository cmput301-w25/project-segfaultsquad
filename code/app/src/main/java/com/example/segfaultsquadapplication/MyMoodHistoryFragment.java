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
import android.app.AlertDialog;
import android.widget.EditText;
import java.util.Calendar;
import java.util.Date;

public class MyMoodHistoryFragment extends Fragment implements MoodAdapter.OnMoodClickListener {
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private RecyclerView moodRecyclerView;
    private MoodAdapter moodAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<MoodEvent> allMoods = new ArrayList<>();

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

    /**
     * sets up the recycler view for mood events display, including attaching
     * adapter and layout mgr
     */
    private void setupRecyclerView() {
        moodAdapter = new MoodAdapter(this);
        moodRecyclerView.setAdapter(moodAdapter);
        moodRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    /**
     * setsup (find) filter option elements from layout and calls helper method to
     * implement/activate the specified filter
     * 
     * @param view
     *             the view/activity where the filter options will be
     *             inflated/overlayed onto
     */
    private void setupFilterOptions(View view) {
        view.findViewById(R.id.filter1).setOnClickListener(v -> {
            applyFilter("Last Week");
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

    /**
     * helper method to get this user's moods, sorted in reverse chronological order
     */
    private void loadMoods() {
        String userId = auth.getCurrentUser().getUid(); // user id
        // debugging
        Log.d("MoodHistory", "Loading moods for user: " + userId);

        // get the mods
        db.collection("moods")
                .whereEqualTo("userId", userId) // of this user
                .orderBy("timestamp", Query.Direction.DESCENDING) // reverse chrono sort
                .addSnapshotListener((value, error) -> {
                    // error handling and toast loggin for failed mood events
                    if (error != null) {
                        Log.e("MoodHistory", "Error loading moods", error);
                        Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // construct moods arraylist
                    allMoods.clear(); // Clear previous moods
                    if (value != null) {
                        // debugging
                        Log.d("MoodHistory", "Number of moods retrieved: " + value.size());

                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                MoodEvent mood = doc.toObject(MoodEvent.class); // make MoodEvent object per fetched
                                                                                // mood
                                mood.setMoodId(doc.getId());
                                allMoods.add(mood); // add to arraylist
                                Log.d("MoodHistory",
                                        "Loaded mood: " + mood.getMoodType() + " with ID: " + mood.getMoodId());
                            } catch (Exception e) {
                                Log.e("MoodHistory", "Error converting document: " + doc.getId(), e);
                            }
                        }

                        // Sort the moods in memory instead
                        Collections.sort(allMoods, (a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
                    }
                    moodAdapter.updateMoods(allMoods);
                });
    }

    /**
     * mood event click listener handling method
     * 
     * @param mood
     *             the clicked on MoodEvent object instance
     */
    @Override
    public void onMoodClick(MoodEvent mood) {
        // TODO: Navigate to mood details view
        Toast.makeText(getContext(), "Clicked: " + mood.getMoodType(), Toast.LENGTH_SHORT).show();
    }

    /**
     * toggle method for the filter overlay display
     */
    private void toggleFilterMenu() {
        isFilterMenuVisible = !isFilterMenuVisible;
        filterMenu.setVisibility(isFilterMenuVisible ? View.VISIBLE : View.GONE);
    }

    /**
     * buffer method, probably could have the filter functionality of each filter
     * here, but whatever
     * 
     * @param filterType
     *                   its the string value of the filter being applied. "Last
     *                   Week", "By Mood", or "By Reason"
     */
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

    /**
     * filter functionality for filtering by last week
     */
    private void filterLastWeek() {
        // Get the current date and time
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.WEEK_OF_YEAR, -1); // Subtract one week
        Date lastWeekDate = calendar.getTime();

        // Filter moods
        List<MoodEvent> filteredMoods = new ArrayList<>();
        for (MoodEvent mood : allMoods) { // Assuming allMoods is your complete list of moods
            if (mood.getTimestamp().toDate().after(lastWeekDate)) { // Convert Timestamp to Date
                filteredMoods.add(mood);
            }
        }
        moodAdapter.updateMoods(filteredMoods); // Update the RecyclerView
    }

    /**
     * the dialog fragment for the filter functionality for filtering by specified
     * mood
     */
    private void showMoodFilterDialog() {
        String[] moods = { "HAPPY", "SAD", "ANGRY", "EXCITED", "TIRED", "SCARED", "SURPRISED" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Mood")
                .setItems(moods, (dialog, which) -> {
                    String selectedMood = moods[which];
                    filterByMood(selectedMood);
                });
        builder.show();
    }

    /**
     * filter functionality for filtering by mood type
     * 
     * @param moodType
     *                 the mood user seleected to see through filter
     */
    private void filterByMood(String moodType) {
        List<MoodEvent> filteredMoods = new ArrayList<>();
        for (MoodEvent mood : allMoods) {
            if (mood.getMoodType().equals(moodType)) {
                filteredMoods.add(mood);
            }
        }

        // debugging
        Log.d("MyMoodHistoryFragment", "Filtered moods count for " + moodType + ": " + filteredMoods.size());

        // alert adapter
        moodAdapter.updateMoods(filteredMoods);
    }

    /**
     * the dialog fragment for the filter functionality for filtering by specified
     * mood
     */
    private void showReasonFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter Reason Keyword");

        final EditText input = new EditText(getContext());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String keyword = input.getText().toString();
            filterByReason(keyword);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * * filter functionality for filtering by specified word inside mood
     * desc/reason
     * 
     * @param keyword
     *                the keyword to filter by
     */
    private void filterByReason(String keyword) {
        List<MoodEvent> filteredMoods = new ArrayList<>();
        for (MoodEvent mood : allMoods) {
            if (mood.getReasonText() != null && mood.getReasonText().toLowerCase().contains(keyword.toLowerCase())) {
                filteredMoods.add(mood);
            }
        }
        moodAdapter.updateMoods(filteredMoods);
    }

    private void clearAllFilters() {
        moodAdapter.updateMoods(allMoods); // Reset to show all moods
    }
}