package com.example.segfaultsquadapplication.display.moodhistory;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

import androidx.navigation.NavController;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Classname: MyMoodHistoryFragment
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 * 
 * Mood History fragment (Not really, just loads of startup)
 */
public class MyMoodHistoryFragment extends Fragment implements MoodAdapter.OnMoodClickListener {
    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;
    private RecyclerView moodRecyclerView;
    private MoodAdapter moodAdapter;
    // Use static arraylist, so subsequent visits to history page will not start out
    // blank
    private static List<MoodEvent> allMoods = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_mood_history, container, false);

        // Initialize views
        filterButton = view.findViewById(R.id.filterButton);
        filterMenu = view.findViewById(R.id.filterMenu);
        moodRecyclerView = view.findViewById(R.id.moodRecyclerView);

        // Setup RecyclerView
        setupRecyclerView();

        // Load moods
        loadMoods();

        // Setup filter button
        filterButton.setOnClickListener(v -> toggleFilterMenu());

        // Setup filter options
        setupFilterOptions(view);

        // Setup FAB for adding new mood
        view.findViewById(R.id.fabAddMood).setOnClickListener(
                v -> Navigation.findNavController(v).navigate(R.id.action_myMoodHistory_to_addMood));

        // Setup FAB for viewing mood analytics
        FloatingActionButton fabViewAnalytics = view.findViewById(R.id.fabViewAnalytics);
        fabViewAnalytics.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.action_to_mood_analytics);
        });

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
        // Display with last fetched moods for now, will be updated when new moods are
        // fetched
        moodAdapter.updateMoods(allMoods);
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
        // debugging
        Log.d("MoodHistory", "Loading moods for user: " + DbUtils.getUserId());

        // get the moods
        ArrayList<MoodEvent> temp = new ArrayList<>();
        MoodEventManager.getAllMoodEvents(DbUtils.getUserId(), MoodEventManager.MoodEventFilter.ALL, temp,
                isSuccess -> {
                    if (isSuccess) {
                        allMoods.clear(); // Clear previous moods
                        // debugging
                        Log.d("MoodHistory", "Number of moods retrieved: " + temp.size());

                        for (MoodEvent mood : temp) {
                            allMoods.add(mood); // add to arraylist
                            Log.d("MoodHistory",
                                    "Loaded mood: " + mood.getMoodType() + " with ID: " + mood.getDbFileId());
                        }

                        // // Sort the moods in memory instead
                        // Collections.sort(allMoods, (a, b) ->
                        // b.getTimestamp().compareTo(a.getTimestamp()));
                        moodAdapter.updateMoods(allMoods);
                    } else {
                        Toast.makeText(getContext(), "Error loading moods", Toast.LENGTH_SHORT).show();
                    }
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
        Bundle args = new Bundle();
        args.putString("moodId", mood.getDbFileId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_myMoodHistory_to_moodDetails, args);
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
        String[] moods = { "ANGER", "CONFUSION", "DISGUST", "FEAR", "HAPPINESS", "SADNESS", "SHAME", "SURPRISE" };
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

        // then filter the mood events
        for (MoodEvent mood : allMoods) {
            // .name() gets the actual name value instead of the whole moodType enum
            if (mood.getMoodType().name().equals(moodType)) {
                filteredMoods.add(mood);
            }
        }

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

        // a LinearLayout for the EditText
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        // EditText (input field)
        final EditText input = new EditText(getContext());
        input.setHint("Type search word here...");
        // Setting margins programmatically for the input field to match the title's
        // indent
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 0, 40, 0);
        input.setLayoutParams(params);
        layout.addView(input); // add the EditText tot he layout
        builder.setView(layout); // set the layout as the dialog

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

    /**
     * method to clear all applied filters
     */
    private void clearAllFilters() {
        moodAdapter.updateMoods(allMoods); // Reset to show all moods
    }
}