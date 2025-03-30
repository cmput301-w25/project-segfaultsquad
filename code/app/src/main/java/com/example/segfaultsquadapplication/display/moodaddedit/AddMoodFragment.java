/**
 * Classname: AddMoodFragment
 * Purpose: Allow user to add a mood event to thier history
 * Current Issues: N/A
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.display.moodaddedit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.location.LocationManager;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.google.android.material.card.MaterialCardView;

import java.util.Date;

import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Locale;

import android.widget.LinearLayout;
import android.util.Log;
import android.text.InputFilter;
import android.widget.Switch;

public class AddMoodFragment extends Fragment {
    // attributes
    private static final int PICK_IMAGE_REQUEST = 1;
    private GridLayout moodGrid;
    private TextView textDateTime;
    private EditText reasonInput;
    private Spinner socialSituationSpinner;
    private ImageView imageUpload;
    private Uri selectedImageUri;
    private MoodEvent.MoodType selectedMoodType = null;
    private Switch togglePublicPrivate; // Declare the Switch
    private boolean isPublicMood = false; // Default to private

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_mood, container, false);

        // Initialize location services
        LocationManager.prepareLocationProvider(getActivity());
        // fusedLocationClient =
        // LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize views
        initializeViews(view);
        setupDateTime();
        EditMoodFragmentInflater.setupMoodGrid(getContext(), getResources(),
                v -> {
                    selectedMoodType = (MoodEvent.MoodType) v.getTag();
                    updateMoodSelection((MaterialCardView) v);
                }, moodGrid);
        EditMoodFragmentInflater.setupSocialSituationSpinner(getContext(), socialSituationSpinner);
        setupImageUpload();
        setupButtons(view);

        return view;
    }

    /**
     * helper method to locate/assign/initialize view components
     *
     * @param view
     *             the view being projected onto
     */
    private void initializeViews(View view) {
        moodGrid = view.findViewById(R.id.moodGrid);
        textDateTime = view.findViewById(R.id.textDateTime);
        reasonInput = view.findViewById(R.id.editTextReason);
        reasonInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(200) }); // Set max length to 200
        socialSituationSpinner = view.findViewById(R.id.spinnerSocialSituation);
        imageUpload = view.findViewById(R.id.imageUpload);
        togglePublicPrivate = view.findViewById(R.id.togglePublicPrivate); // Initialize the Switch
    }

    /**
     * setup for the used date and time format
     */
    private void setupDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());
        textDateTime.setText(sdf.format(new Date()));
    }


    /**
     * helper method to setup image upload for mood reason (picture)
     */
    private void setupImageUpload() {
        // debugging
        Log.d("AddMoodFragment", "entered setupImageUpload()");
        // click listener on the imageUplaod view section
        imageUpload.setOnClickListener(v -> {
            // setup intents to carry to uplaoding area / devidce gallery
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            // start uplaod image request
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });
        // debugging
        Log.d("AddMoodFragment", "completed setupImageUpload()");
    }

    /**
     * helper method to setup buttons for confirming moodevent creation or
     * cancellation
     *
     * @param view
     *             the view being projected onto
     */
    private void setupButtons(View view) {
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.buttonConfirm).setOnClickListener(v -> saveMood());
        view.findViewById(R.id.buttonCancel).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    /**
     * method to handle completion of this fragment
     * NOTE: NOT BEING USED ANYMORE
     *
     * @param requestCode
     *                    the request code (e.g. 200, 404, 201, etc)
     * @param resultCode
     *                    the result code
     * @param data
     *                    the data transmitted as intent
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // debugging
        Log.d("AddMoodFragment", "entered onActivityRequest()");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageUpload.setImageURI(selectedImageUri);
            imageUpload.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageUpload.setPadding(0, 0, 0, 0);
        }
        Log.d("AddMoodFragment", "completed onActivityRequest()");
    }

    /**
     * visual highlight for the selected mood option
     *
     * @param selectedCard
     *                     the mood type card selected in the "How are you feeling?"
     *                     section
     */
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

    /**
     * confirm button for mood event creation
     */
    private void saveMood() {
        Log.d("AddMoodFragment", "entered saveMood()");
        MoodEvent.SocialSituation situation = null;
        if (socialSituationSpinner.getSelectedItem() != null) { // set optional social situation field if provided
            situation = (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem();
        }
        if (togglePublicPrivate.isChecked()) { // set optional social situation field if provided
            isPublicMood = true;
        }
        MoodEventManager.createMoodEvent(getContext(), selectedMoodType,
                reasonInput.getText().toString().trim(), isPublicMood, situation,
                selectedImageUri, isSuccess -> {
                    if (isAdded()) { //meaning that fragment not destroyed, need so app doesn't crash upon reconnection
                        if (isSuccess) {
                            Toast.makeText(getContext(), "Successfully saved mood event!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Could not save mood event...", Toast.LENGTH_SHORT).show();
                        }
                        navigateBackSafely();//navigate back if fragment exists
                    }
                });

        if (!isNetworkAvailable()) { //even if no internet connection, navigate back
            Toast.makeText(getContext(), "No internet connection. Mood will be saved upon connection.", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp(); // navigate back even if offline
        }

        Log.d("AddMoodFragment", "completed saveMood()");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void navigateBackSafely() {
        if (isAdded() && getView() != null) {
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

}