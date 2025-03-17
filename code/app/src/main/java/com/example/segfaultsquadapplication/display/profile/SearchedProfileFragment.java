package com.example.segfaultsquadapplication.display.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SearchedProfileFragment extends Fragment {
    private ImageView profilePicture;
    private TextView username;
    private TextView followersCount;
    private TextView followingCount;
    private RecyclerView recyclerViewMoodGrid;
    private MoodGridAdapter moodGridAdapter;
    private String displayUserId;
    private User displayedUser;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<MoodEvent> moodEvents = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_searched_profile, container, false);

        // Initialize views
        profilePicture = view.findViewById(R.id.profile_picture);
        username = view.findViewById(R.id.username);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);
        recyclerViewMoodGrid = view.findViewById(R.id.recycler_view_mood_grid);
        ImageButton backButton = view.findViewById(R.id.buttonBack);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get user ID from arguments
        Bundle args = getArguments();
        if (args != null) {
            displayUserId = args.getString("userId");
            fetchUserData();
        }

        // Set up RecyclerView
        setupRecyclerView();

        // Set up back button
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void fetchUserData() {
        db.collection("users").document(displayUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayedUser = documentSnapshot.toObject(User.class);
                        setUserData();
                        loadFollowerAndFollowingCounts();
                        loadMoodEvents();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SearchedProfileFragment", "Error fetching user data", e);
                });
    }

    private void setUserData() {
        if (displayedUser != null) {
            username.setText(displayedUser.getUsername());

            // Load profile picture if available
            List<Integer> profilePicData = displayedUser.getProfilePicUrl();
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
    }

    private void loadFollowerAndFollowingCounts() {
        // Load followers count
        db.collection("following")
                .whereEqualTo("followedId", displayUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int followers = queryDocumentSnapshots.size();
                    followersCount.setText(String.valueOf(followers));
                });

        // Load following count
        db.collection("following")
                .whereEqualTo("followerId", displayUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int following = queryDocumentSnapshots.size();
                    followingCount.setText(String.valueOf(following));
                });
    }

    private void setupRecyclerView() {
        moodGridAdapter = new MoodGridAdapter(getContext(), moodEvents);
        recyclerViewMoodGrid.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerViewMoodGrid.setAdapter(moodGridAdapter);
    }

    private void loadMoodEvents() {
        db.collection("moods")
                .whereEqualTo("userId", displayUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    moodEvents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MoodEvent mood = doc.toObject(MoodEvent.class);
                        moodEvents.add(mood);
                    }
                    moodGridAdapter.notifyDataSetChanged();
                });
    }
}
