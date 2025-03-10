package com.example.segfaultsquadapplication;

import static com.example.segfaultsquadapplication.MoodEvent.SocialSituation.ALONE;
import static com.example.segfaultsquadapplication.MoodEvent.SocialSituation.IN_CROWD;
import static com.example.segfaultsquadapplication.MoodEvent.SocialSituation.WITH_GROUP;
import static com.example.segfaultsquadapplication.MoodEvent.SocialSituation.WITH_ONE_PERSON;

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

/**
 * A Fragment that displays detailed information about a specific mood event.
 * This class retrieves a mood event from Firestore using the mood ID passed as an argument,
 * and displays all related information including mood type, date/time, reason text, images,
 * social situation, and location.
 */
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
    private TextView triggerTextView;
    private Button editButton;
    private Button deleteButton;

    // Data
    private String moodId;
    private MoodEvent currentMood;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    /**
     * Map that associates each MoodType with its corresponding emoji representation.
     * This map is used to display the appropriate emoji for each mood type.
     */
    private final Map<MoodEvent.MoodType, String> moodEmojis = Map.of(
            MoodEvent.MoodType.ANGER, "😡",
            MoodEvent.MoodType.CONFUSION, "😵‍💫",
            MoodEvent.MoodType.DISGUST, "🤢",
            MoodEvent.MoodType.FEAR, "😱",
            MoodEvent.MoodType.HAPPINESS, "😀",
            MoodEvent.MoodType.SADNESS, "😭",
            MoodEvent.MoodType.SHAME, "😳",
            MoodEvent.MoodType.SURPRISE, "🤯");

    /**
     * Inflates the mood details layout and initializes the fragment.
     * Retrieves the mood ID from arguments and sets up the UI components.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container The parent view that the fragment's UI should be attached to
     * @param savedInstanceState Previous state of the fragment if it was saved
     * @return The View for the fragment's UI
     */
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

    /**
     * Initializes all UI components from the inflated view.
     *
     * @param view The root view containing all UI components
     */
    private void initializeViews(View view) {
        backButton = view.findViewById(R.id.backButton);
        titleTextView = view.findViewById(R.id.moodTitleTextView);
        dateTimeTextView = view.findViewById(R.id.dateTimeTextView);
        moodEmojiTextView = view.findViewById(R.id.moodEmojiTextView);
        reasonTextView = view.findViewById(R.id.reasonTextView);
        reasonImageView = view.findViewById(R.id.reasonImageView);
        socialSituationTextView = view.findViewById(R.id.socialSituationTextView);
        locationTextView = view.findViewById(R.id.locationTextView);
        triggerTextView = view.findViewById(R.id.triggerTextView);
        editButton = view.findViewById(R.id.editButton);
        deleteButton = view.findViewById(R.id.deleteButton);
    }

    /**
     * Sets up click listeners for interactive UI components.
     * This includes navigation actions for the back and edit buttons,
     * and confirmation dialog for the delete button.
     */
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

    /**
     * Loads the mood data from Firestore using the mood ID.
     * When successful, it converts the Firestore document to a MoodEvent object
     * and populates the UI with the retrieved data.
     * On failure, it shows an error message and navigates back.
     */
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

    /**
     * Populates the UI components with data from the MoodEvent object.
     * This includes setting the mood title, emoji, date/time, reason text and image,
     * social situation, trigger, and location information.
     * Components are hidden if their corresponding data is not available.
     *
     * @param mood The MoodEvent object containing all the mood data
     */
    private void populateUI(MoodEvent mood) {
        // Set mood title and emoji
        titleTextView.setText(mood.getMoodType().name().charAt(0) +
                mood.getMoodType().name().substring(1).toLowerCase());

        // Set emoji
        String emoji = moodEmojis.get(mood.getMoodType());
        if (emoji != null) {
            moodEmojiTextView.setText(emoji);
        } else {
            // Log the mood type for debugging
            Log.e(TAG, "Emoji not found for mood type: " + mood.getMoodType());
            // Provide a default emoji
            moodEmojiTextView.setText("😐");
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

        // Set trigger text
        if (mood.getTrigger() != null && !mood.getTrigger().isEmpty()) {
            triggerTextView.setText(mood.getTrigger());
            triggerTextView.setVisibility(View.VISIBLE);
        } else {
            triggerTextView.setVisibility(View.GONE);
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
            GeoPoint locationPoint = mood.getLocation();
            String locationText = "Lat: " + locationPoint.getLatitude() + ", Long: " + locationPoint.getLongitude();
            locationTextView.setText(locationText);
            locationTextView.setVisibility(View.VISIBLE);
        } else {
            locationTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Formats the SocialSituation enum value into a user-friendly string.
     *
     * @param situation The SocialSituation enum value to format
     * @return A formatted string representation of the social situation
     */
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

    /**
     * Shows a confirmation dialog before deleting a mood event.
     * If confirmed, it calls the deleteMood method to perform the actual deletion.
     */
    private void confirmDeleteMood() {
        // Show a confirmation dialog before deleting
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Mood")
                .setMessage("Are you sure you want to delete this mood entry?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMood())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the current mood event from Firestore.
     * On success, it shows a confirmation message and navigates back.
     * On failure, it shows an error message.
     */
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