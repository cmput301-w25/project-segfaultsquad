package com.example.segfaultsquadapplication.display.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SearchedProfileFragment extends Fragment {
    private ImageView profilePicture;
    private TextView username;
    private TextView followersCount;
    private TextView followingCount;
    private FirebaseFirestore db;
    private String searchedUserId;
    private String currentUserId;
    private Button followButton;
    private boolean currentUserFollowingSearched;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_searched_profile, container, false);

        // Get searched user ID from arguments
        searchedUserId = getArguments().getString("searchedUserId");

        // Initialize views
        profilePicture = view.findViewById(R.id.profile_picture);
        username = view.findViewById(R.id.username);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        ImageButton backButton = view.findViewById(R.id.backButton);

        // Variables pertaining to current user
        currentUserId = getArguments().getString("currentUserId");
        followButton = view.findViewById(R.id.follow_profile_button);
        currentUserFollowingSearched = false;

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Load user data
        loadUserData();

        // Set back button click listener
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void loadUserData() {
        db.collection("users").document(searchedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        setUserData(user);
                    }
                });
    }

    private void setUserData(User user) {
        username.setText(user.getUsername());
        loadFollowerAndFollowingCounts();

        // Set profile picture
        List<Integer> profilePicData = user.getProfilePicUrl();
        if (profilePicData != null && !profilePicData.isEmpty()) {
            byte[] imageBytes = new byte[profilePicData.size()];
            for (int i = 0; i < profilePicData.size(); i++) {
                imageBytes[i] = profilePicData.get(i).byteValue();
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap != null) {
                profilePicture.setImageBitmap(bitmap);
            } else {
                profilePicture.setImageResource(R.drawable.ic_person);
            }
        } else {
            profilePicture.setImageResource(R.drawable.ic_person);
        }
    }

    private void loadFollowerAndFollowingCounts() {
        // Load followers count
        db.collection("following")
                .whereEqualTo("followedId", searchedUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    followersCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                    checkIfFollowing(); //to check if current follows searched
                });

        // Load following count
        db.collection("following")
                .whereEqualTo("followerId", searchedUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    followingCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                });
    }

    private void checkIfFollowing() {
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)//check if current user is following
                .whereEqualTo("followedId", searchedUserId)
                .limit(1) // Optimize by limiting results to 1
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    currentUserFollowingSearched = !queryDocumentSnapshots.isEmpty();
                    followButton.setText("Following");
                });
    }
}