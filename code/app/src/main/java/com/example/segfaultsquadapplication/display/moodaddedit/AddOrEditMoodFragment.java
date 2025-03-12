package com.example.segfaultsquadapplication.display.moodaddedit;

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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fragment for adding a new mood event or editing an existing mood event.
 * This fragment allows users to update details of a mood event including
 * the mood type, reason, trigger, social situation, and associated image.
 */
public class AddOrEditMoodFragment extends Fragment {
    private static final String TAG = "AddEditMoodFragment";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());

    // UI components
    private GridLayout moodGrid;
    private TextView textDateTime;
    private EditText reasonInput;
    private EditText triggerInput;
    private Spinner socialSituationSpinner;
    private ImageView imageUpload;
    private Uri selectedImageUri;

    // Data
    private String moodId;
    private MoodEvent currentMood;
    private MoodEvent.MoodType selectedMoodType = null;

    // Frag Type
    private boolean isEditFrag;

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state
     * @return The View for the fragment's UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.edit_mood, container, false);

        // Both triggers have arguments
        if (getArguments() != null) {
            // Edit mood fragment has moodId
            moodId = getArguments().getString("moodId");
            isEditFrag = moodId != null;
        } else {
            Log.e(TAG, "Error: No arguments provided");
            Toast.makeText(getContext(), "Error displaying modification page", Toast.LENGTH_SHORT).show();
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
        if (isEditFrag) {
            loadMoodData();
        } else {
            // Set the timestamp
            textDateTime.setText(SDF.format(new Date()));
        }

        return view;
    }

    /**
     * Initializes all UI components from the view.
     *
     * @param view The root view containing all UI components
     */
    private void initializeViews(View view) {
        moodGrid = view.findViewById(R.id.moodGrid);
        textDateTime = view.findViewById(R.id.textDateTime);
        reasonInput = view.findViewById(R.id.editTextReason);
        triggerInput = view.findViewById(R.id.editTextTrigger);
        socialSituationSpinner = view.findViewById(R.id.spinnerSocialSituation);
        imageUpload = view.findViewById(R.id.imageUpload);

        // Update the title to "Edit Mood" instead of "Add Mood"
        TextView titleTextView = view.findViewById(R.id.textViewTitle);
        if (titleTextView != null) {
            titleTextView.setText(isEditFrag ? "Edit Mood" : "Create Mood");
        }
    }

    /**
     * Sets up the mood selection grid with all available mood types.
     * Creates a card for each mood type with an emoji and text label.
     */
    private void setupMoodGrid() {
        int i = -1;
        for (MoodEvent.MoodType moodType : MoodEvent.MoodType.values()) {
            i ++;
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

            // Use the mood-specific color for the stroke
            moodCard.setStrokeColor(moodType.getPrimaryColor(requireContext()));

            // Create vertical layout for emoji and text
            LinearLayout layout = new LinearLayout(requireContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setPadding(8, 16, 8, 16);

            // Add emoji
            TextView emojiText = new TextView(requireContext());
            emojiText.setText(moodType.getEmoticon());
            emojiText.setTextSize(24);
            emojiText.setGravity(Gravity.CENTER);

            // Add mood name
            TextView moodText = new TextView(requireContext());
            moodText.setText(moodType.name().charAt(0) + moodType.name().substring(1).toLowerCase());
            moodText.setGravity(Gravity.CENTER);
            moodText.setTextSize(12);
            moodText.setPadding(0, 8, 0, 0);

            layout.addView(emojiText);
            layout.addView(moodText);
            moodCard.addView(layout);
            moodCard.setTag(moodType);

            moodCard.setOnClickListener(v -> {
                selectedMoodType = (MoodEvent.MoodType) v.getTag();
                updateMoodSelection((MaterialCardView) v);
            });

            moodGrid.addView(moodCard);
        }
    }

    /**
     * Sets up the social situation spinner with all available options.
     */
    private void setupSocialSituationSpinner() {
        ArrayAdapter<MoodEvent.SocialSituation> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                MoodEvent.SocialSituation.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationSpinner.setAdapter(adapter);
    }

    /**
     * Sets up the image upload functionality.
     * Configures the click listener to launch the image picker.
     */
    private void setupImageUpload() {
        imageUpload.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });
    }

    /**
     * Sets up the navigation and action buttons.
     *
     * @param view The root view containing all buttons
     */
    private void setupButtons(View view) {
        // Navigation back button
        view.findViewById(R.id.buttonBack).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());

        // Save changes button
        view.findViewById(R.id.buttonConfirm).setOnClickListener(v ->
                saveChanges());

        // Cancel button
        view.findViewById(R.id.buttonCancel).setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    /**
     * Loads the current mood data from Firestore.
     * Retrieves the mood document using the provided mood ID.
     */
    private void loadMoodData() {
        AtomicReference<MoodEvent> ref = new AtomicReference<>();
        MoodEventManager.getMoodEventById(moodId, ref, isSuccess -> {
            if (isSuccess) {
                currentMood = ref.get();
                // Fill the UI with the mood data
                populateUI(currentMood);
            } else {
                Toast.makeText(getContext(), "Mood event not found", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    /**
     * Populates the UI with data from the loaded mood event.
     *
     * @param mood The MoodEvent object containing all mood data
     */
    private void populateUI(MoodEvent mood) {
        // Set the mood type
        selectedMoodType = mood.getMoodType();
        highlightSelectedMood(selectedMoodType);

        // Set the reason text
        reasonInput.setText(mood.getReasonText());

        // Set the trigger text if available
        if (mood.getTrigger() != null) {
            triggerInput.setText(mood.getTrigger());
        }

        // Set the timestamp
        textDateTime.setText(SDF.format(mood.getTimestampDate()));

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

    /**
     * Highlights the selected mood in the mood grid.
     * Updates the background and text colors of all mood cards.
     *
     * @param moodType The MoodType to highlight as selected
     */
    private void highlightSelectedMood(MoodEvent.MoodType moodType) {
        for (int i = 0; i < moodGrid.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) moodGrid.getChildAt(i);
            MoodEvent.MoodType cardMoodType = (MoodEvent.MoodType) card.getTag();

            boolean isSelected = cardMoodType == moodType;
            card.setStrokeWidth(isSelected ? 0 : 1);

            // Use mood-specific colors instead of just color_primary
            if (isSelected) {
                card.setCardBackgroundColor(moodType.getPrimaryColor(requireContext()));
            } else {
                card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                card.setStrokeColor(cardMoodType.getPrimaryColor(requireContext()));
            }

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

    /**
     * Updates the visual selection state when a new mood is selected.
     *
     * @param selectedCard The MaterialCardView that was selected
     */
    private void updateMoodSelection(MaterialCardView selectedCard) {
        for (int i = 0; i < moodGrid.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) moodGrid.getChildAt(i);
            MoodEvent.MoodType cardMoodType = (MoodEvent.MoodType) card.getTag();
            boolean isSelected = card == selectedCard;

            card.setStrokeWidth(isSelected ? 0 : 1);

            // Use mood-specific colors
            if (isSelected) {
                card.setCardBackgroundColor(cardMoodType.getPrimaryColor(requireContext()));
            } else {
                card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                card.setStrokeColor(cardMoodType.getPrimaryColor(requireContext()));
            }

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

    /**
     * Handles the result of the image picker activity.
     * Updates the image preview when a new image is selected.
     *
     * @param requestCode The request code passed to startActivityForResult()
     * @param resultCode The result code returned by the child activity
     * @param data An Intent that carries the result data
     */
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

    /**
     * Updates the mood event in Firestore with the user's changes.
     * Validates input data before updating the database.
     */
    private void saveChanges() {
        String reason = reasonInput.getText().toString().trim();
        String trigger = triggerInput.getText().toString().trim();

        MoodEvent.SocialSituation situation;
        if (socialSituationSpinner.getSelectedItem() != null) {
            situation = (MoodEvent.SocialSituation) socialSituationSpinner.getAdapter().getItem(socialSituationSpinner.getSelectedItemPosition());
        } else {
            situation = (MoodEvent.SocialSituation) socialSituationSpinner.getAdapter().getItem(0);
        }

        // Update the mood in Firestore
        try {
            if (isEditFrag) {
                MoodEventManager.updateMoodEvent(getContext(), currentMood, selectedMoodType, reason, trigger,
                        situation, selectedImageUri, isSuccess -> {
                            if (isSuccess) {
                                Log.d(TAG, "Mood update successful");
                                Toast.makeText(getContext(), "Mood updated successfully", Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(requireView()).navigateUp();
                            } else {
                                Toast.makeText(getContext(), "Error updating mood", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
            // Create
            else {
                MoodEventManager.createMoodEvent(getContext(), selectedMoodType, reason, trigger,
                        situation, selectedImageUri, isSuccess -> {
                            if (isSuccess) {
                                Log.d(TAG, "Mood creation successful");
                                Toast.makeText(getContext(), "Mood created successfully", Toast.LENGTH_SHORT).show();
                                Navigation.findNavController(requireView()).navigateUp();
                            } else {
                                Toast.makeText(getContext(), "Error creating mood", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while saving changes", e);
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
