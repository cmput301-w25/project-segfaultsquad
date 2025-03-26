/**
 * Classname: ProfileFragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication.display.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.display.moodhistory.MoodAdapter;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.widget.ImageButton;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.net.Uri;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

public class ProfileFragment extends Fragment implements MoodAdapter.OnMoodClickListener {

    private ImageView profilePicture;
    private TextView username;
    private TextView followersCount;
    private TextView followingCount;
    private RecyclerView moodRecyclerView;
    private MoodAdapter moodAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<MoodEvent> moodEvents = new ArrayList<>();
    private User currentUser; // To hold user data

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageButton filterButton;
    private CardView filterMenu;
    private boolean isFilterMenuVisible = false;

    private EditText searchEditText;
    private RecyclerView searchResultsRecyclerView;
    private SearchResultAdapter searchResultAdapter;

    private ImageButton searchButton;

    private LinearLayout searchSection;
    private ImageButton headerSearchButton;

    private CardView searchResultsCard;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        profilePicture = view.findViewById(R.id.profile_picture);
        username = view.findViewById(R.id.username);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        moodRecyclerView = view.findViewById(R.id.moodRecyclerView);
        ImageButton overflowButton = view.findViewById(R.id.overflowButton);
        CardView logoutDropdown = view.findViewById(R.id.logoutDropdown);
        TextView logoutOption = view.findViewById(R.id.logoutOption);
        ImageButton editProfilePictureButton = view.findViewById(R.id.editProfilePictureButton);
        ImageButton heartButton = view.findViewById(R.id.heartButton);
        filterButton = view.findViewById(R.id.filterButton);
        filterMenu = view.findViewById(R.id.filterMenu);
        searchEditText = view.findViewById(R.id.searchEditText);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        searchButton = view.findViewById(R.id.searchButton);
        searchSection = view.findViewById(R.id.searchSection);
        headerSearchButton = view.findViewById(R.id.headerSearchButton);
        searchResultsCard = view.findViewById(R.id.searchResultsCard);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set user data
        fetchCurrentUser(); // Fetch user details

        // Set up RecyclerView for mood list
        setupRecyclerView();

        // Load mood events
        loadMoodEvents();

        // Set click listeners
        followersCount.setOnClickListener(v -> navigateToFollowersList());
        followingCount.setOnClickListener(v -> navigateToFollowingList());

        // Toggle logout dropdown visibility
        overflowButton.setOnClickListener(v -> {
            if (logoutDropdown.getVisibility() == View.VISIBLE) {
                logoutDropdown.setVisibility(View.GONE);
            } else {
                logoutDropdown.setVisibility(View.VISIBLE);
            }
        });

        // Handle logout option click
        logoutOption.setOnClickListener(v -> {
            logoutUser();
            logoutDropdown.setVisibility(View.GONE); // Hide dropdown after logout
        });

        // Handle profile picture edit button click
        editProfilePictureButton.setOnClickListener(v -> openImagePicker());

        // Set click listener for the heart button
        heartButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_to_follow_requests);
        });

        // Setup filter button
        filterButton.setOnClickListener(v -> toggleFilterMenu());

        // Setup filter options
        setupFilterOptions(view);

        // Setup search functionality
        searchResultAdapter = new SearchResultAdapter(this::onSearchResultClick);
        searchResultsRecyclerView.setAdapter(searchResultAdapter);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Toggle functionality for search section
        headerSearchButton.setOnClickListener(v -> {
            if (searchSection.getVisibility() == View.VISIBLE) {
                searchSection.setVisibility(View.GONE);
                searchResultsCard.setVisibility(View.GONE);
                searchEditText.setText(""); // Clear search when hiding
            } else {
                searchSection.setVisibility(View.VISIBLE);
                searchEditText.requestFocus(); // Automatically focus the search field
            }
        });

        // Replace the existing search button click listener with this
        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                // Search for exact username match
                searchExactUsername(query);
            }
        });

        // Add TextWatcher to searchEditText for real-time search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    searchResultsCard.setVisibility(View.GONE);
                } else {
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enable options menu
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile_menu, menu); // Inflate the menu
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser(); // Call the logout method
            return true;
        }
        return false;
    }

    private void fetchCurrentUser() {
        String currentUserId = auth.getCurrentUser().getUid(); // Get current user ID
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        setUserData(); // Set user data in UI
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Error fetching user data", e);
                });
    }

    private void setUserData() {
        if (currentUser != null) {
            username.setText(currentUser.getUsername()); // username
            loadFollowerAndFollowingCounts(); // follower and following count

            // Load profile picture if available
            List<Integer> profilePicData = currentUser.getProfilePicUrl();
            if (profilePicData != null && !profilePicData.isEmpty()) {
                // Convert List<Integer> back to byte array
                byte[] imageBytes = new byte[profilePicData.size()];
                for (int i = 0; i < profilePicData.size(); i++) {
                    imageBytes[i] = profilePicData.get(i).byteValue();
                }

                // Decode byte array to Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    profilePicture.setImageBitmap(bitmap);
                } else {
                    profilePicture.setImageResource(R.drawable.profile_icon); // Default image
                }
            } else {
                profilePicture.setImageResource(R.drawable.profile_icon); // Default image
            }
        }
    }

    /**
     * method to update the counts of followers and following on the profile page,
     * as well as set those to the
     */
    private void loadFollowerAndFollowingCounts() {
        String userId = auth.getCurrentUser().getUid(); // Get user ID

        // Load followers count
        db.collection("following")
                .whereEqualTo("followedId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int followers = queryDocumentSnapshots.size();
                    followersCount.setText(String.valueOf(followers));
                });

        // Load following count
        db.collection("following")
                .whereEqualTo("followerId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int following = queryDocumentSnapshots.size();
                    followingCount.setText(String.valueOf(following));
                });
    }

    private void setupRecyclerView() {
        moodAdapter = new MoodAdapter(this);
        moodRecyclerView.setAdapter(moodAdapter);
        moodRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void loadMoodEvents() {
        String userId = auth.getCurrentUser().getUid(); // Get user ID

        // Use MoodEventManager to get all mood events for the current user
        MoodEventManager.getAllMoodEvents(userId, MoodEventManager.MoodEventFilter.ALL, moodEvents, isSuccess -> {
            if (isSuccess) {
                Log.d("ProfileFragment", "Mood events count: " + moodEvents.size());
                moodAdapter.updateMoods(moodEvents); // Update the adapter with the fetched mood events
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
                .navigate(R.id.action_to_moodDetails, args);
    }

    private void navigateToFollowersList() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.action_to_followers_list); // Ensure this action exists in your nav_graph.xml
    }

    private void navigateToFollowingList() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.action_to_following_list); // Ensure this action exists in your nav_graph.xml
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut(); // Sign out the user
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Navigate back to the login screen
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.navigation_login); // Ensure this action exists in your nav_graph.xml
    }

    // Method to open image picker
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Handle the result of the image picker
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            uploadProfilePicture(selectedImageUri);
        }
    }

    // Method to upload the profile picture
    private void uploadProfilePicture(Uri imageUri) {
        // Convert the image to a byte array
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
            byte[] imageData = new byte[inputStream.available()];
            inputStream.read(imageData);
            inputStream.close();

            // Convert byte array to List<Integer>
            List<Integer> byteList = new ArrayList<>();
            for (byte b : imageData) {
                byteList.add((int) b); // Convert byte to int
            }

            // Update Firestore with the image data
            String currentUserId = auth.getCurrentUser().getUid();
            db.collection("users").document(currentUserId)
                    .update("profilePicUrl", byteList) // Store image data as List<Integer>
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ProfileFragment", "SUCCESS updating profile picture");
                        Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                        // Reload the user data to refresh the profile picture
                        fetchCurrentUser();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileFragment", "Error updating profile picture", e);
                        Toast.makeText(getContext(), "Error updating profile picture", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error uploading image", e);
            Toast.makeText(getContext(), "Error uploading image", Toast.LENGTH_SHORT).show();
        }
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
        for (MoodEvent mood : moodEvents) {
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
        for (MoodEvent mood : moodEvents) {
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
        for (MoodEvent mood : moodEvents) {
            if (mood.getReasonText() != null &&
                    mood.getReasonText().toLowerCase().contains(keyword.toLowerCase())) {
                filteredMoods.add(mood);
            }
        }
        moodAdapter.updateMoods(filteredMoods);
    }

    private void clearAllFilters() {
        moodAdapter.updateMoods(moodEvents);
    }

    private void performSearch(String query) {
        String currentUserId = auth.getCurrentUser().getUid();
        Log.d("Search", "Performing search with query: " + query);

        if (query.isEmpty()) {
            searchResultsCard.setVisibility(View.GONE);
            return;
        }

        db.collection("users")
                .orderBy("username")
                .startAt(query.toLowerCase()) // Convert to lowercase for case-insensitive search
                .endAt(query.toLowerCase() + "\uf8ff")
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("Search", "Query returned " + queryDocumentSnapshots.size() + " results");
                    List<User> results = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Log.d("Search", "Processing user: " + document.getString("username"));
                        if (!document.getId().equals(currentUserId)) {
                            User user = document.toObject(User.class);
                            user.setDbFileId(document.getId());
                            results.add(user);
                        }
                    }

                    if (!results.isEmpty()) {
                        Log.d("Search", "Showing " + results.size() + " results");
                        searchResultsCard.setVisibility(View.VISIBLE);
                        searchResultAdapter.updateResults(results);
                    } else {
                        Log.d("Search", "No results to show");
                        searchResultsCard.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Search", "Error performing search", e);
                    searchResultsCard.setVisibility(View.GONE);
                });
    }

    private void searchExactUsername(String username) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        if (!document.getId().equals(currentUserId)) {
                            User user = document.toObject(User.class);
                            user.setDbFileId(document.getId());
                            navigateToSearchedProfile(user);
                        }
                    }
                });
    }

    private void onSearchResultClick(User user) {
        navigateToSearchedProfile(user);
    }

    private void navigateToSearchedProfile(User user) {
        Bundle args = new Bundle();
        args.putString("searchedUserID", user.getDbFileId());
        args.putString("currentUserID", auth.getCurrentUser().getUid());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_profile_to_searched_profile, args);

        // Clear search
        searchEditText.setText("");
        searchResultsCard.setVisibility(View.GONE);
        searchSection.setVisibility(View.GONE);
    }
}