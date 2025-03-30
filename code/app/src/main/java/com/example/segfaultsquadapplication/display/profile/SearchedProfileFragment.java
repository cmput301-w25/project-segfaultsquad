package com.example.segfaultsquadapplication.display.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.following.FollowingManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.firebase.firestore.FieldValue;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SearchedProfileFragment extends Fragment {
    private ImageView profilePicture;
    private TextView username;
    private TextView followersCount;
    private TextView followingCount;
    private String searchedUserId;
    private String currentUserId;
    private Button followButton;
    private boolean currentUserFollowingSearched; //if current user already searched
    private boolean followRequestSent; //if follow request has from current user to searched

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_searched_profile, container, false);

        // Get searched user ID from arguments
        searchedUserId = getArguments().getString("searchedUserID");

        // Initialize views
        profilePicture = view.findViewById(R.id.profile_picture);
        username = view.findViewById(R.id.username);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        ImageButton backButton = view.findViewById(R.id.backButton);

        // Variables pertaining to current user
        currentUserId = getArguments().getString("currentUserID");
        followButton = view.findViewById(R.id.follow_profile_button);
        currentUserFollowingSearched = false;

        // Load user data
        loadUserData();

        // Set back button click listener
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        followButton.setOnClickListener(v -> sendFollowRequest());

        return view;
    }

    private void loadUserData() {
        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(searchedUserId, userHolder,
                isSuccess -> {
                    if (isSuccess) {
                        setUserData(userHolder.get());
                    }
                });
    }

    private void setUserData(User searchedUser) {
        username.setText(searchedUser.getUsername());
        adaptUI(searchedUser);

        // Set profile picture
        List<Integer> profilePicData = searchedUser.getProfilePicUrl();
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

    /**
     * Adapts the UI contents based on the searched user's information.
     * @param searchedUser The searched user.
     */
    private void adaptUI(User searchedUser) {
        followersCount.setText(String.valueOf(searchedUser.getFollowers().size()));
        followingCount.setText(String.valueOf(searchedUser.getFollowing().size()));
        // The following button style based on relationship status
        if (searchedUser.getFollowers().contains(currentUserId)) {
            currentUserFollowingSearched = true;
            updateFollowButton();
        } else if (searchedUser.getFollowRequests().contains(currentUserId)) {
            followRequestSent = true;
            updateFollowButton();
        }
    }

    private void updateFollowButton() {
        if (currentUserFollowingSearched) {
            followButton.setText("Following");
            followButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), com.google.android.material.R.color.button_material_dark));
        }
        else if (followRequestSent) {
            followButton.setText("Requested to Follow");
            followButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), com.google.android.material.R.color.button_material_dark));
        }
    }

    private void sendFollowRequest() {
        if (!currentUserFollowingSearched & !followRequestSent) {
            FollowingManager.sendFollowRequest(searchedUserId);
            Toast.makeText(getContext(), "Follow Request Sent", Toast.LENGTH_SHORT).show();
            followRequestSent = true;
            updateFollowButton();
        }
    }
}