package com.example.segfaultsquadapplication;

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

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class MoodDetails extends Fragment {
    private static final String TAG = "MoodDetails";

    // UI components
    private ImageButton backButton;
    private TextView titleTextView;
    private TextView dateTimeTextView;
    private TextView moodEmojiTextView;
    private TextView reasonTextView;
    private ImageView reasonImageView;
    private TextView socialSituationTextView;
    private TextView locationTextView;
    private Button editButton;
    private Button deleteButton;

    // Data
    private String moodId;
    private MoodEvent currentMood;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Map of mood types to emojis
    private final Map<MoodEvent.MoodType, String> moodEmojis = Map.of(
            MoodEvent.MoodType.ANGRY, "😡",
            MoodEvent.MoodType.SAD, "😭",
            MoodEvent.MoodType.HAPPY, "😀",
            MoodEvent.MoodType.EXCITED, "😆",
            MoodEvent.MoodType.TIRED, "😴",
            MoodEvent.MoodType.SCARED, "😱",
            MoodEvent.MoodType.SURPRISED, "🤯");

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mood_details, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get the mood ID from arguments
        if (getArguments() != null) {
            moodId = getArguments().getString("moodId");
            if (moodId == null) {
                Toast.makeText(getContext(), "Error: No mood ID provided", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(container).navigateUp();
                return view;
            }
        } else {
            Toast.makeText(getContext(), "Error: No arguments provided", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(container).navigateUp();
            return view;
        }

        // Initialize views
        initializeViews(view);
        setupListeners();

        // Load the mood data
        loadMoodData();

        return view;
    }

    private void initializeViews(View view) {
        backButton = view.findViewById(R.id.backButton);
        titleTextView = view.findViewById(R.id.moodTitleTextView);
        dateTimeTextView = view.findViewById(R.id.dateTimeTextView);
        moodEmojiTextView = view.findViewById(R.id.moodEmojiTextView);
        reasonTextView = view.findViewById(R.id.reasonTextView);
        reasonImageView = view.findViewById(R.id.reasonImageView);
        socialSituationTextView = view.findViewById(R.id.socialSituationTextView);
        locationTextView = view.findViewById(R.id.locationTextView);
        editButton = view.findViewById(R.id.editButton);
        deleteButton = view.findViewById(R.id.deleteButton);
    }

    private void setupListeners() {
        // Back button
        backButton.setOnClickListener(v ->
                Navigation.findNavController(requireView()).navigateUp());

        // Edit button
        editButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("moodId", moodId);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_moodDetails_to_editMood, args);
        });

        // Delete button
        deleteButton.setOnClickListener(v -> confirmDeleteMood());
    }

    private void loadMoodData() {
        db.collection("moods").document(moodId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            // Create a MoodEvent object from the document
                            MoodEvent mood = documentSnapshot.toObject(MoodEvent.class);
                            if (mood != null) {
                                currentMood = mood;
                                currentMood.setMoodId(documentSnapshot.getId());

                                // Fill the UI with the mood data
                                populateUI(mood);
                            } else {
                                Log.e(TAG, "Failed to convert document to MoodEvent object");
                                Toast.makeText(getContext(), "Error loading mood data", Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(requireView()).navigateUp();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing document", e);
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).navigateUp();
                        }
                    } else {
                        Log.e(TAG, "Document does not exist");
                        Toast.makeText(getContext(), "Mood not found", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting document", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                });
    }

    private void populateUI(MoodEvent mood) {
        // Set mood title and emoji
        titleTextView.setText(mood.getMoodType().name().charAt(0) +
                mood.getMoodType().name().substring(1).toLowerCase());

        // Set emoji
// Set emoji
        String emoji = moodEmojis.get(mood.getMoodType());
        if (emoji != null) {
            moodEmojiTextView.setText(emoji);
        }

        // Set date and time
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());
        dateTimeTextView.setText(sdf.format(mood.getTimestampDate()));

        // Set reason text
        if (mood.getReasonText() != null && !mood.getReasonText().isEmpty()) {
            reasonTextView.setText(mood.getReasonText());
            reasonTextView.setVisibility(View.VISIBLE);
        } else {
            reasonTextView.setVisibility(View.GONE);
        }

        // Set reason image
        if (mood.getImageData() != null && !mood.getImageData().isEmpty()) {
            // Convert the List<Integer> back to byte array
            byte[] imageBytes = new byte[mood.getImageData().size()];
            for (int i = 0; i < mood.getImageData().size(); i++) {
                imageBytes[i] = mood.getImageData().get(i).byteValue();
            }

            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                reasonImageView.setImageBitmap(bitmap);
                reasonImageView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error displaying image", e);
                reasonImageView.setVisibility(View.GONE);
            }
        } else {
            reasonImageView.setVisibility(View.GONE);
        }

        // Set social situation
        if (mood.getSocialSituation() != null) {
            String socialSituation = formatSocialSituation(mood.getSocialSituation());
            socialSituationTextView.setText(socialSituation);
            socialSituationTextView.setVisibility(View.VISIBLE);
        } else {
            socialSituationTextView.setVisibility(View.GONE);
        }

        // Set location
        if (mood.getLocation() != null) {
            // In a real app, you would reverse geocode the coordinates
            // For now, just display a placeholder
            locationTextView.setText("University of Alberta, AB");
            locationTextView.setVisibility(View.VISIBLE);
        } else {
            locationTextView.setVisibility(View.GONE);
        }
    }

    private String formatSocialSituation(MoodEvent.SocialSituation situation) {
        switch (situation) {
            case ALONE:
                return "Alone";
            case WITH_ONE_PERSON:
                return "With One Person";
            case WITH_GROUP:
                return "With a Group";
            case IN_CROWD:
                return "In a Crowd";
            default:
                return situation.name();
        }
    }

    private void confirmDeleteMood() {
        // Show a confirmation dialog before deleting
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Mood")
                .setMessage("Are you sure you want to delete this mood entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMood())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMood() {
        db.collection("moods").document(moodId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Mood deleted successfully", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting mood", e);
                    Toast.makeText(getContext(), "Error deleting mood: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
