/**
 * Classname: EditMoodFragment
 * Purpose: Allows users to edit an existing mood event.
 * Current Issues: N/A
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 *
 * This fragment allows users to update details of a previously created mood
 * event including the mood type, reason, social situation, and associated image.
 */
package com.example.segfaultsquadapplication.display.moodaddedit;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
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
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This fragment allows users to update details of a previously created mood event including the mood type, reason, social situation, and associate image.
 *
 * Current Issues: N/A
 */
public class EditMoodFragment extends Fragment {
    // attribute
    private static final String TAG = "EditMoodFragment";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI components
    private GridLayout moodGrid;
    private TextView textDateTime;
    private EditText reasonInput;
    private Spinner socialSituationSpinner;
    private ImageView imageUpload;
    private Uri selectedImageUri;
    private Switch togglePublicPrivate;

    // Date and time related variables
    private Calendar selectedDateTime;

    // Data
    private String moodId;
    private MoodEvent currentMood;
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
        View view = inflater.inflate(R.layout.edit_mood, container, false);

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
        EditMoodFragmentInflater.setupMoodGrid(getContext(), getResources(),
                v -> {
                    selectedMoodType = (MoodEvent.MoodType) v.getTag();
                    updateMoodSelection((MaterialCardView) v);
                }, moodGrid);
        EditMoodFragmentInflater.setupSocialSituationSpinner(getContext(), socialSituationSpinner);
        setupImageUpload();
        setupButtons(view);

        // Load the current mood data
        loadMoodData();

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

        // Make the textDateTime clickable to show date/time pickers
        textDateTime.setOnClickListener(v -> showDateTimePicker());

        reasonInput = view.findViewById(R.id.editTextReason);
        reasonInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(200) }); // Set max length to 200
        socialSituationSpinner = view.findViewById(R.id.spinnerSocialSituation);
        imageUpload = view.findViewById(R.id.imageUpload);
        togglePublicPrivate = view.findViewById(R.id.togglePublicPrivate);

        // Update the title to "Edit Mood" instead of "Add Mood"
        TextView titleTextView = view.findViewById(R.id.textViewTitle);
        if (titleTextView != null) {
            titleTextView.setText("Edit Mood");
        }
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

        // Save changes button
        view.findViewById(R.id.buttonConfirm).setOnClickListener(v -> updateMood());

        // Cancel button
        view.findViewById(R.id.buttonCancel).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    /**
     * Shows date and time picker dialogs for selecting a custom timestamp.
     * First shows the date picker, then the time picker sequentially.
     */
    private void showDateTimePicker() {
        // If we haven't initialized the selected date/time yet, use the current mood's timestamp
        if (selectedDateTime == null) {
            selectedDateTime = Calendar.getInstance();
            if (currentMood != null) {
                selectedDateTime.setTime(currentMood.getTimestampDate());
            }
        }

        // Show date picker first
        showDatePicker();
    }

    /**
     * Shows the date picker dialog to select a date.
     * After date selection, automatically shows the time picker.
     */
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    // Update the selected date
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // After date is selected, show time picker
                    showTimePicker();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    /**
     * Shows the time picker dialog to select a time.
     * After time selection, updates the displayed date/time.
     */
    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                getContext(),
                (view, hourOfDay, minute) -> {
                    // Update the selected time
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    selectedDateTime.set(Calendar.SECOND, 0);

                    // Update the display
                    updateDateTimeDisplay();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    /**
     * Updates the date/time display with the currently selected date and time.
     * Also adds visual indication if the date has been modified.
     */
    private void updateDateTimeDisplay() {
        // Format the date/time for display
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());
        textDateTime.setText(sdf.format(selectedDateTime.getTime()));

        if (currentMood != null &&
                !selectedDateTime.getTime().equals(currentMood.getTimestampDate())) {
            textDateTime.setTextColor(getResources().getColor(R.color.text_colour));
        } else {
            textDateTime.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    /**
     * Loads the current mood data from Firestore.
     * Retrieves the mood document using the provided mood ID.
     */
    private void loadMoodData() {
        AtomicReference<MoodEvent> holder = new AtomicReference<>();
        MoodEventManager.getMoodEventById(moodId, holder, isSuccess -> {
            if (isSuccess) {
                currentMood = holder.get();
                populateUI(currentMood);
            } else {
                Toast.makeText(getContext(), "Error loading mood data", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    /**
     * Populates the UI with data from the loaded mood event.
     *
     * @param mood
     *             The MoodEvent object containing all mood data
     */
    private void populateUI(MoodEvent mood) {
        // Set the mood type
        selectedMoodType = mood.getMoodType();
        highlightSelectedMood(selectedMoodType);

        // Set the reason text
        reasonInput.setText(mood.getReasonText());

        // Initialize selectedDateTime with the mood's timestamp
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.setTime(mood.getTimestampDate());

        // Format and display the date/time
        updateDateTimeDisplay();

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

        // Set the public/private toggle based on the mood's visibility
        togglePublicPrivate.setChecked(mood.isPublic());
    }

    /**
     * Highlights the selected mood in the mood grid.
     * Updates the background and text colors of all mood cards.
     *
     * @param moodType
     *                 The MoodType to highlight as selected
     */
    private void highlightSelectedMood(MoodEvent.MoodType moodType) {
        for (int i = 0; i < moodGrid.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) moodGrid.getChildAt(i);
            MoodEvent.MoodType cardMoodType = (MoodEvent.MoodType) card.getTag();

            boolean isSelected = cardMoodType == moodType;
            card.setStrokeWidth(isSelected ? 0 : 1);

            // Use mood-specific colors instead of just color_primary
            if (isSelected) {
                card.setCardBackgroundColor(moodType.getPrimaryColor(getContext()));
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
     * Updates the visual selection state when a new mood is selected.
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
     * Updates the mood event in Firestore with the user's changes.
     * Validates input data before updating the database.
     * Includes the custom selected date/time in the update.
     */
    private void updateMood() {
        String reason = reasonInput.getText().toString().trim();
        // Update the mood in Firestore
        MoodEvent.SocialSituation situation = null;
        if (socialSituationSpinner.getSelectedItem() != null) { // set optional social situation field if provided
            situation = (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem();
        }

        // Pass the selected date/time to the update method
        MoodEventManager.updateMoodEventWithTimestamp(
                getContext(),
                currentMood,
                selectedMoodType,
                reason,
                togglePublicPrivate.isChecked(),
                situation,
                selectedImageUri,
                selectedDateTime.getTime(), // Pass the new timestamp
                isSuccess -> {
                    if (isAdded()) {
                        if (isSuccess) {
                            Toast.makeText(getContext(), "Mood updated successfully", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireView()).navigateUp();
                        } else {
                            Toast.makeText(getContext(), "Error saving the modification", Toast.LENGTH_SHORT).show();
                        }
                        navigateBackSafely();// navigate back if fragment exists
                    }
                });

        if (!isNetworkAvailable()) { //even if no internet connection, navigate back
            Toast.makeText(getContext(), "No internet connection. Mood will be saved upon connection.", Toast.LENGTH_LONG).show();
            Navigation.findNavController(requireView()).navigateUp(); // navigate back even if offline
        }
    }

    /**
     * for offline functionality
     *
     * @return
     *         returns bool of if network is avilible
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * navigates to previous fragment
     */
    private void navigateBackSafely() {
        if (isAdded() && getView() != null) {
            Navigation.findNavController(requireView()).navigateUp();
        }
    }
}