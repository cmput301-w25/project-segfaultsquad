package com.example.segfaultsquadapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditMoodFragment extends Fragment {
    private static final String TAG = "EditMoodFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI components
    private GridLayout moodGrid;
    private TextView textDateTime;
    private EditText reasonInput;
    private Spinner socialSituationSpinner;
    private ImageView imageUpload;
    private Uri selectedImageUri;

    // Data
    private String moodId;
    private MoodEvent currentMood;
    private MoodEvent.MoodType selectedMoodType = null;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_mood, container, false);

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
        setupMoodGrid();
        setupSocialSituationSpinner();
        setupImageUpload();
        setupButtons(view);

        // Load the current mood data
        loadMoodData();

        return view;
    }

    private void initializeViews(View view) {
        moodGrid = view.findViewById(R.id.moodGrid);
        textDateTime = view.findViewById(R.id.textDateTime);
        reasonInput = view.findViewById(R.id.editTextReason);
        socialSituationSpinner = view.findViewById(R.id.spinnerSocialSituation);
        imageUpload = view.findViewById(R.id.imageUpload);

        // Update the title to "Edit Mood" instead of "Add Mood"
        TextView titleTextView = view.findViewById(R.id.textViewTitle);
        if (titleTextView != null) {
            titleTextView.setText("Edit Mood");
        }
    }

    private void setupMoodGrid() {
        // Pair each mood type with its emoji
        String[] moodEmojis = {
                "😡", // ANGRY
                "😭", // SAD
                "😀", // HAPPY
                "😆", // EXCITED
                "😴", // TIRED
                "😱", // SCARED
                "🤯" // SURPRISED
        };

        String[] moodNames = {
                "ANGRY", "SAD", "HAPPY", "EXCITED", "TIRED", "SCARED", "SURPRISED"
        };

        for (int i = 0; i < moodNames.length; i++) {
            MaterialCardView moodCard = new MaterialCardView(requireContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 4, 1f);
            params.rowSpec = GridLayout.spec(i / 4);
            params.setMargins(8, 8, 8, 8);
            moodCard.setLayoutParams(params);
            moodCard.setRadius(8);
            moodCard.setStrokeWidth(1);
            moodCard.setStrokeColor(getResources().getColor(R.color.color_primary));

            // Create vertical layout for emoji and text
            LinearLayout layout = new LinearLayout(requireContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(8, 16, 8, 16);

            // Add emoji
            TextView emojiText = new TextView(requireContext());
            emojiText.setText(moodEmojis[i]);
            emojiText.setTextSize(24);
            emojiText.setGravity(Gravity.CENTER);

            // Add mood name
            TextView moodText = new TextView(requireContext());
            moodText.setText(moodNames[i].charAt(0) + moodNames[i].substring(1).toLowerCase());
            moodText.setGravity(Gravity.CENTER);
            moodText.setTextSize(12);
            moodText.setPadding(0, 8, 0, 0);

            layout.addView(emojiText);
            layout.addView(moodText);
            moodCard.addView(layout);
            moodCard.setTag(MoodEvent.MoodType.valueOf(moodNames[i]));

            moodCard.setOnClickListener(v -> {
                selectedMoodType = (MoodEvent.MoodType) v.getTag();
                updateMoodSelection((MaterialCardView) v);
            });

            moodGrid.addView(moodCard);
        }
    }

    private void setupSocialSituationSpinner() {
        // Create a custom adapter for better text formatting
        ArrayAdapter<MoodEvent.SocialSituation> adapter = new ArrayAdapter<MoodEvent.SocialSituation>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                MoodEvent.SocialSituation.values()) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                MoodEvent.SocialSituation item = getItem(position);

                // Format the text nicely
                if (item != null) {
                    String formattedText = formatSocialSituation(item);
                    textView.setText(formattedText);
                }

                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                MoodEvent.SocialSituation item = getItem(position);

                // Format the text the same way for dropdown items
                if (item != null) {
                    String formattedText = formatSocialSituation(item);
                    textView.setText(formattedText);
                }

                return textView;
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
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(adapter);
    }

    private void setupImageUpload() {
        imageUpload.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });
    }

    private void setupButtons(View view) {
        // Navigation back button
        view.findViewById(R.id.buttonBack).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Save changes button
        view.findViewById(R.id.buttonConfirm).setOnClickListener(v ->
                updateMood());

        // Cancel button
        view.findViewById(R.id.buttonCancel).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
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
        // Set the mood type
        selectedMoodType = mood.getMoodType();
        highlightSelectedMood(selectedMoodType);

        // Set the reason text
        reasonInput.setText(mood.getReasonText());

        // Set the timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());
        textDateTime.setText(sdf.format(mood.getTimestampDate()));

        // Set the social situation if available
        if (mood.getSocialSituation() != null) {
            for (int i = 0; i < socialSituationSpinner.getAdapter().getCount(); i++) {
                if (socialSituationSpinner.getAdapter().getItem(i).equals(mood.getSocialSituation())) {
                    socialSituationSpinner.setSelection(i);
                    break;
                }
            }
        }

        // Set the image if available
        if (mood.getImageData() != null && !mood.getImageData().isEmpty()) {
            // Convert the List<Integer> back to byte array
            byte[] imageBytes = new byte[mood.getImageData().size()];
            for (int i = 0; i < mood.getImageData().size(); i++) {
                imageBytes[i] = mood.getImageData().get(i).byteValue();
            }

            try {
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                imageUpload.setImageBitmap(bitmap);
                imageUpload.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageUpload.setPadding(0, 0, 0, 0);
            } catch (Exception e) {
                Log.e(TAG, "Error displaying image", e);
            }
        }
    }

    private void highlightSelectedMood(MoodEvent.MoodType moodType) {
        for (int i = 0; i < moodGrid.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) moodGrid.getChildAt(i);
            MoodEvent.MoodType cardMoodType = (MoodEvent.MoodType) card.getTag();

            boolean isSelected = cardMoodType == moodType;
            card.setStrokeWidth(isSelected ? 0 : 1);
            card.setCardBackgroundColor(getResources().getColor(
                    isSelected ? R.color.color_primary : android.R.color.white));

            // Update both emoji and text color
            LinearLayout layout = (LinearLayout) card.getChildAt(0);
            TextView emojiText = (TextView) layout.getChildAt(0);
            TextView moodText = (TextView) layout.getChildAt(1);

            int textColor = getResources().getColor(
                    isSelected ? android.R.color.white : R.color.color_primary);
            emojiText.setTextColor(textColor);
            moodText.setTextColor(textColor);
        }
    }

    private void updateMoodSelection(MaterialCardView selectedCard) {
        for (int i = 0; i < moodGrid.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) moodGrid.getChildAt(i);
            boolean isSelected = card == selectedCard;
            card.setStrokeWidth(isSelected ? 0 : 1);
            card.setCardBackgroundColor(getResources().getColor(
                    isSelected ? R.color.color_primary : android.R.color.white));

            // Update both emoji and text color
            LinearLayout layout = (LinearLayout) card.getChildAt(0);
            TextView emojiText = (TextView) layout.getChildAt(0);
            TextView moodText = (TextView) layout.getChildAt(1);

            int textColor = getResources().getColor(
                    isSelected ? android.R.color.white : R.color.color_primary);
            emojiText.setTextColor(textColor);
            moodText.setTextColor(textColor);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageUpload.setImageURI(selectedImageUri);
            imageUpload.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageUpload.setPadding(0, 0, 0, 0);
        }
    }

    private void updateMood() {
        if (selectedMoodType == null) {
            Toast.makeText(getContext(), "Please select a mood", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = reasonInput.getText().toString().trim();

        // Check if reason text is within the limit
        if (reason.length() > 20) {
            Toast.makeText(getContext(), "Reason must be 20 characters or less", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("moodType", selectedMoodType.name());
        updates.put("reasonText", reason);

        if (socialSituationSpinner.getSelectedItem() != null) {
            updates.put("socialSituation", socialSituationSpinner.getSelectedItem().toString());
        }

        // Keep the original timestamp and location

        // Handle image update if there's a new image
        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), selectedImageUri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                // Check image size
                if (byteArray.length > 65536) { // 64 KB limit
                    Toast.makeText(getContext(), "Image size exceeds the limit of 64 KB", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Convert byte array to List<Integer>
                List<Integer> byteList = new ArrayList<>();
                for (byte b : byteArray) {
                    byteList.add((int) b);
                }

                updates.put("imageData", byteList);
            } catch (IOException e) {
                Log.e(TAG, "Error processing image", e);
                Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Update the mood in Firestore
        db.collection("moods").document(moodId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Mood updated successfully", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating mood", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
