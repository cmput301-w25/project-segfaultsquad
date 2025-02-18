package com.example.segfaultsquadapplication;

import android.app.Activity;
import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.Date;
import java.util.UUID;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.view.Gravity;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AddMoodFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_mood, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize views
        initializeViews(view);
        setupDateTime();
        setupMoodGrid();
        setupSocialSituationSpinner();
        setupImageUpload();
        setupButtons(view);

        return view;
    }

    private void initializeViews(View view) {
        moodGrid = view.findViewById(R.id.moodGrid);
        textDateTime = view.findViewById(R.id.textDateTime);
        reasonInput = view.findViewById(R.id.editTextReason);
        socialSituationSpinner = view.findViewById(R.id.spinnerSocialSituation);
        imageUpload = view.findViewById(R.id.imageUpload);
    }

    private void setupDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault());
        textDateTime.setText(sdf.format(new Date()));
    }

    private void setupMoodGrid() {
        String[] moodNames = { "ANGRY", "SAD", "HAPPY", "EXCITED",
                "TIRED", "SCARED", "SURPRISED" };

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

            TextView moodText = new TextView(requireContext());
            moodText.setText(moodNames[i].charAt(0) + moodNames[i].substring(1).toLowerCase());
            moodText.setGravity(Gravity.CENTER);
            moodText.setPadding(8, 16, 8, 16);

            moodCard.addView(moodText);
            moodCard.setTag(MoodEvent.MoodType.valueOf(moodNames[i]));

            moodCard.setOnClickListener(v -> {
                selectedMoodType = (MoodEvent.MoodType) v.getTag();
                updateMoodSelection((MaterialCardView) v);
            });

            moodGrid.addView(moodCard);
        }
    }

    private void setupSocialSituationSpinner() {
        ArrayAdapter<MoodEvent.SocialSituation> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                MoodEvent.SocialSituation.values());
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
        view.findViewById(R.id.buttonBack).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.buttonConfirm).setOnClickListener(v -> saveMood());
        view.findViewById(R.id.buttonCancel).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageUpload.setImageURI(selectedImageUri);
            imageUpload.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageUpload.setPadding(0, 0, 0, 0);
        }
    }

    private void updateMoodSelection(MaterialCardView selectedCard) {
        for (int i = 0; i < moodGrid.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) moodGrid.getChildAt(i);
            boolean isSelected = card == selectedCard;
            card.setStrokeWidth(isSelected ? 0 : 1);
            card.setCardBackgroundColor(getResources().getColor(
                    isSelected ? R.color.color_primary : android.R.color.white));
            ((TextView) card.getChildAt(0)).setTextColor(getResources().getColor(
                    isSelected ? android.R.color.white : R.color.color_primary));
        }
    }

    private void saveMood() {
        if (selectedMoodType == null) {
            Toast.makeText(getContext(), "Please select a mood", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String reason = reasonInput.getText().toString().trim();
        MoodEvent newMood = new MoodEvent(userId, selectedMoodType, reason);

        // Set social situation
        if (socialSituationSpinner.getSelectedItem() != null) {
            newMood.setSocialSituation(
                    (MoodEvent.SocialSituation) socialSituationSpinner.getSelectedItem());
        }

        // If image selected, upload it first
        if (selectedImageUri != null) {
            uploadImageAndSaveMood(newMood);
        } else {
            saveMoodToFirestore(newMood);
        }
    }

    private void uploadImageAndSaveMood(MoodEvent mood) {
        String imageFileName = "mood_images/" + UUID.randomUUID().toString();
        StorageReference imageRef = storageRef.child(imageFileName);

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        mood.setReasonImageUrl(uri.toString());
                        saveMoodToFirestore(mood);
                    });
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show());
    }

    private void saveMoodToFirestore(MoodEvent mood) {
        db.collection("moods")
                .add(mood)
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