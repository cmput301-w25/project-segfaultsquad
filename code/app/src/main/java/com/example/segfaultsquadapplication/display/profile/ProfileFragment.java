/**
 * Classname: ProfileFragment
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication.display.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.widget.ImageButton;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import com.google.firebase.firestore.FieldValue;

public class ProfileFragment extends Fragment {

    private ImageView profilePicture;
    private TextView username;
    private TextView followersCount;
    private TextView followingCount;
    private RecyclerView recyclerViewMoodGrid;
    private MoodGridAdapter moodGridAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<MoodEvent> moodEvents = new ArrayList<>();
    private User currentUser; // To hold user data

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        profilePicture = view.findViewById(R.id.profile_picture);
        username = view.findViewById(R.id.username);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        recyclerViewMoodGrid = view.findViewById(R.id.recycler_view_mood_grid);
        ImageButton overflowButton = view.findViewById(R.id.overflowButton);
        CardView logoutDropdown = view.findViewById(R.id.logoutDropdown);
        TextView logoutOption = view.findViewById(R.id.logoutOption);
        ImageButton editProfilePictureButton = view.findViewById(R.id.editProfilePictureButton);
        ImageButton heartButton = view.findViewById(R.id.heartButton);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // debugging
        Log.d("ProfileFragment", "-0");

        // Set user data
        fetchCurrentUser(); // Fetch user details

        // debugging
        Log.d("ProfileFragment", "-1");

        // Set up RecyclerView for mood grid
        setupRecyclerView();

        // debugging
        Log.d("ProfileFragment", "-2");

        // Load mood events
        loadMoodEvents();

        // debugging
        Log.d("ProfileFragment", "-3");

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

        // debugging
        Log.d("ProfileFragment", "-99");

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
            username.setText(currentUser.getUsername());

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
                    profilePicture.setImageResource(R.drawable.ic_person); // Default image
                }
            } else {
                profilePicture.setImageResource(R.drawable.ic_person); // Default image
            }
            loadFollowerAndFollowingCounts();
        }
    }

    private void loadFollowerAndFollowingCounts() {
        String userId = auth.getCurrentUser().getUid(); // Get user ID

        // Load followers count
        db.collection("followers")
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
        moodGridAdapter = new MoodGridAdapter(getContext(), moodEvents);
        recyclerViewMoodGrid.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns
        recyclerViewMoodGrid.setAdapter(moodGridAdapter);
    }

    private void loadMoodEvents() {
        String userId = auth.getCurrentUser().getUid(); // Get user ID

        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    moodEvents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MoodEvent mood = doc.toObject(MoodEvent.class);
                        moodEvents.add(mood);
                    }
                    // debugging
                    Log.d("ProfileFragment", "Mood events count: " + moodEvents.size());
                    moodGridAdapter.notifyDataSetChanged(); // Notify adapter of data change
                });
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
}