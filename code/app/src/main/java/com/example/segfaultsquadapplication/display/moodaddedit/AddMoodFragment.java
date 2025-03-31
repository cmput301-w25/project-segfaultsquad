/**
 * Classname: CreateMoodFragment
 * Purpose: Allows users to create a new mood event.
 * Current Issues: N/A
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 *
 * This fragment allows users to create a new mood event by selecting the mood type,
 * entering a reason, choosing a social situation, and optionally adding an image.
 */
package com.example.segfaultsquadapplication.display.moodaddedit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This fragment allows users to create a new mood event by selecting the mood type,
 * entering a reason, choosing a social situation, and optionally adding an image.
 *
 * Current Issues: N/A
 */
public class AddMoodFragment extends Fragment {
    // attribute
    private static final String TAG = "CreateMoodFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI components
    private GridLayout moodGrid;
    private TextView textDateTime;
    private EditText reasonInput;
    private Spinner socialSituationSpinner;
    private ImageView imageUpload;
    private Uri selectedImageUri;
    private Switch togglePublicPrivate;

    // Data
    private MoodEvent.MoodType selectedMoodType = null;

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater           The LayoutInflater object that can be used to
     *                           inflate views
     * @param container          If non-null, this is the parent view that the
     *                           fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state
     * @return The View for the fragment's UI
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_mood, container, false);

        // Initialize views
        initializeViews(view);

        // Set up the mood grid with the updated selection behavior
        EditMoodFragmentInflater.setupMoodGrid(getContext(), getResources(),
                v -> {
                    selectedMoodType = (MoodEvent.MoodType) v.getTag();
                    updateMoodSelection((MaterialCardView) v);
                }, moodGrid);

        EditMoodFragmentInflater.setupSocialSituationSpinner(getContext(), socialSituationSpinner);
        setupImageUpload();
        setupButtons(view);

        // Set current date and time
        updateDateTimeText();

        return view;
    }

    /**
     * Initializes all UI components from the view.
     *
     * @param view
     *             The root view containing all UI components
     */
    private void initializeViews(View view) {
        moodGrid = view.findViewById(R.id.moodGrid);
        textDateTime = view.findViewById(R.id.textDateTime);
        reasonInput = view.findViewById(R.id.editTextReason);
        reasonInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(200) }); // Set max length to 200
        socialSituationSpinner = view.findViewById(R.id.spinnerSocialSituation);
        imageUpload = view.findViewById(R.id.imageUpload);
        togglePublicPrivate = view.findViewById(R.id.togglePublicPrivate);
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
     * @param view
     *             The root view containing all buttons
     */
    private void setupButtons(View view) {
        // Navigation back button
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Save mood button
        view.findViewById(R.id.buttonConfirm).setOnClickListener(v -> saveMood());

        // Cancel button
        view.findViewById(R.id.buttonCancel).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    /**
     * Updates the date and time text view with the current date and time.
     */
    private void updateDateTimeText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());
        textDateTime.setText(sdf.format(new Date()));
    }

    /**
     * Updates the visual selection state when a new mood is selected.
     * Uses the mood type's specific color for highlighting.
     *
     * @param selectedCard
     *                     The MaterialCardView that was selected
     */
    private void updateMoodSelection(MaterialCardView selectedCard) {
        MoodEvent.MoodType selectedType = (MoodEvent.MoodType) selectedCard.getTag();

        for (int i = 0; i < moodGrid.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) moodGrid.getChildAt(i);
            MoodEvent.MoodType cardMoodType = (MoodEvent.MoodType) card.getTag();
            boolean isSelected = card == selectedCard;

            card.setStrokeWidth(isSelected ? 0 : 1);

            // Use mood-specific colors
            if (isSelected) {
                card.setCardBackgroundColor(cardMoodType.getPrimaryColor(getContext()));
            } else {
                card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
                card.setStrokeColor(cardMoodType.getPrimaryColor(getContext()));
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
     * @param requestCode
     *                    The request code passed to startActivityForResult()
     * @param resultCode
     *                    The result code returned by the child activity
     * @param data
     *                    An Intent that carries the result data
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
     * Validates input and saves the new mood event to Firestore.
     * Performs validation checks on required fields before saving.
     */
    private void saveMood() {
        // Validate mood selection
        if (selectedMoodType == null) {
            Toast.makeText(getContext(), "Please select a mood", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get reason text
        String reason = reasonInput.getText().toString().trim();

        // Get social situation if selected
        MoodEvent.SocialSituation situation = null;
        if (socialSituationSpinner.getSelectedItem() != null) {
            situation = (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem();
        }

        // Get public/private setting
        boolean isPublic = togglePublicPrivate.isChecked();

        // Create and save the new mood event
        MoodEventManager.createMoodEvent(getContext(), selectedMoodType, reason, isPublic,
                situation, selectedImageUri, isSuccess -> {
                    if (isAdded()) {
                        if (isSuccess) {
                            Toast.makeText(getContext(), "Mood created successfully", Toast.LENGTH_SHORT).show();
                            navigateBackSafely();
                        } else {
                            Toast.makeText(getContext(), "Error saving mood", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        if (!isNetworkAvailable()) {
            Toast.makeText(getContext(), "No internet connection. Mood will be saved upon connection.",
                    Toast.LENGTH_LONG).show();
            navigateBackSafely(); // Navigate back even if offline
        }
    }

    /**
     * Checks if network connectivity is available.
     *
     * @return
     *         true if network is available, false otherwise
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Safely navigates back to the previous screen.
     * Checks if the fragment is still attached to the activity before navigating.
     */
    private void navigateBackSafely() {
        if (isAdded() && getView() != null) {
            Navigation.findNavController(requireView()).navigateUp();
        }
    }
}