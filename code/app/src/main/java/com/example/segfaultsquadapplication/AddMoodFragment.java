/**
 * Classname: AddMoodFragment
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.view.Gravity;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.GeoPoint;
import android.widget.LinearLayout;
import android.util.Log;

public class AddMoodFragment extends Fragment {
    // attributes
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private GridLayout moodGrid;
    private TextView textDateTime;
    private EditText reasonInput;
    private Spinner socialSituationSpinner;
    private ImageView imageUpload;
    private Uri selectedImageUri;
    private MoodEvent.MoodType selectedMoodType = null;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storageRef;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_mood, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize views
        initializeViews(view);
        setupDateTime();
        setupMoodGrid();
        setupSocialSituationSpinner();
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
        socialSituationSpinner = view.findViewById(R.id.spinnerSocialSituation);
        imageUpload = view.findViewById(R.id.imageUpload);
    }

    /**
     * setup for the used date and time format
     */
    private void setupDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());
        textDateTime.setText(sdf.format(new Date()));
    }

    /**
     * helper method to setup the UI elements of the availible moods to select from.
     * includes setup of boxes, texts and emojis
     */
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

    /**
     * helper mehtod to setup dropdown options dynamically
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
        if (selectedMoodType == null) {
            Toast.makeText(getContext(), "Please select a mood", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for location permission
        if (checkLocationPermission()) {
            getUserLocationAndSaveMood();
        } else {
            requestLocationPermission();
        }

        Log.d("AddMoodFragment", "completed saveMood()");
    }

    /**
     * helper method to check if user gave permission for location use
     * 
     * @return
     *         bool value of permission obtained or denied
     */
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * helper method to obtain user permission for location permisssion
     * 
     * @return
     *         bool value of permission obtained or denied
     */
    private void requestLocationPermission() {
        requestPermissions(
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /**
     * helper method to verify results of location permission request
     * 
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // permission gramted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocationAndSaveMood();
            } else {
                // TODO: ask if this is what we are actualy supposed to do or not...
                // permission denied, save mood without location
                createAndSaveMood(null);
            }
        }
    }

    /**
     * method to get user location and save moodevent
     */
    private void getUserLocationAndSaveMood() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    GeoPoint geoPoint = null;
                    if (location != null) {
                        geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    }
                    createAndSaveMood(geoPoint);
                })
                .addOnFailureListener(e -> {
                    // Failed to get location, save mood without it
                    createAndSaveMood(null);
                });
    }

    /**
     * helper method to actually create and save the mood once the confirm button is
     * clicked
     * 
     * @param location
     *                 the location of the user. Either null or Geopoint type
     */
    private void createAndSaveMood(GeoPoint location) {
        // get all user input fields
        String userId = auth.getCurrentUser().getUid(); // get user Id
        String reason = reasonInput.getText().toString().trim(); // get reason text

        // create mood event and set its location to provided location or null (if not
        // provided)
        MoodEvent newMood = new MoodEvent(userId, selectedMoodType, reason);
        newMood.setLocation(location); // set location using attribute

        if (socialSituationSpinner.getSelectedItem() != null) { // set optional social situation field if provided
            newMood.setSocialSituation(
                    (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem());
        }

        if (selectedImageUri != null) { // if image reason give, use the image handling function
            uploadImageAndSaveMood(newMood);
        } else { // else just the regular text reason handling function
            saveMoodToFirestore(newMood);
        }
    }

    /**
     * method to save mood event to db given image reason
     * 
     * @param mood
     *             the moodEvent being saved
     */
    private void uploadImageAndSaveMood(MoodEvent mood) {
        // Debugging
        Log.d("AddMoodFragment", "Entered uploadImageAndSaveMood()");

        // Check if selectedImageUri is valid
        if (selectedImageUri == null) {
            Log.e("AddMoodFragment", "Selected image URI is null");
            Toast.makeText(getContext(), "No image selected", Toast.LENGTH_SHORT).show();
            return;
        } else {
            // debugging
            Log.d("AddMoodFragment", "Selected image URI: " + selectedImageUri.toString());
        }

        // Get the image size
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
            int imageSize = inputStream.available(); // Get the size in bytes

            // debugging (basically i dont know how to take an image on the emulator thats
            // not the built in default, but i know that default images size is 30kb ===
            // 30,000bytes)
            Log.d("uploadImageAndSaveMood", "imagesize" + imageSize);
            // Yep, works as expected

            // Check if the image size exceeds the limit
            if (imageSize > 65536) { // 65,536 bytes limit
                Toast.makeText(getContext(), "Image size exceeds the limit of 64 KB", Toast.LENGTH_SHORT).show();
                Log.e("AddMoodFragment", "Image size exceeds the limit: " + imageSize + " bytes");
                return;
            }
        } catch (IOException e) {
            Log.e("AddMoodFragment", "Error getting image size", e);
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert the image into a bitarray
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), selectedImageUri);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            // Convert byte array to List<Byte> because apparetly firestore doesnt support
            // serializing bytearrays directly and so we need lists for storing arrays.
            // PREV ERROR: java.lang.IllegalArgumentException: Could not serialize object.
            // Serializing Arrays is not supported, please use Lists instead (found in field
            // 'imageData')
            // List<Byte> byteList = new ArrayList<>();
            // for (byte b : byteArray) {
            // byteList.add(b);
            // }
            // NOPE

            // Apparently firestore also donest allow serializing Byte objects directly, so
            // need to cast to numeric types (int, long, float, etc)
            // Convert byte array to List<Integer>
            List<Integer> byteList = new ArrayList<>();
            for (byte b : byteArray) {
                byteList.add((int) b); // Convert byte to int
            }

            mood.setImageData(byteList); // Set the List<Integer> in the MoodEvent

        } catch (IOException e) {
            Log.e("AddMoodFragment", "Error converting image to byte array", e);
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
            return;
        }

        // save image to firestore
        saveMoodToFirestore(mood);

        Log.d("AddMoodFragment", "Completed uploadImageAndSaveMood()");
    }

    /**
     * method to save mood event given string reason
     * 
     * @param mood
     *             the mood event being saved
     */
    private void saveMoodToFirestore(MoodEvent mood) {
        // Create a map to store the mood data
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("userId", mood.getUserId());
        moodData.put("moodType", mood.getMoodType().name());
        moodData.put("reasonText", mood.getReasonText());
        moodData.put("timestamp", mood.getTimestamp());

        // Add the image data as a BLOB (if image reason)
        if (mood.getImageData() != null) {
            moodData.put("imageData", mood.getImageData());
        }

        // save to firestore db
        db.collection("moods")
                .add(moodData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Mood saved successfully", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(getView()).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error saving mood: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

}