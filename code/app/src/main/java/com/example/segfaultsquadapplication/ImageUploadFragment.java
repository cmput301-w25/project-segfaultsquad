package com.example.segfaultsquadapplication;


import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ImageUploadFragment extends Fragment {
    private ImageView gallery;
    private Button btnGallery;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pic_upload, container, false);

        gallery = view.findViewById(R.id.uploaded_img);
        btnGallery = view.findViewById(R.id.upload_button);

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Gallery Opened", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                imagePickerLauncher.launch(intent); //launch image picker
            }
        });

        return view;
    }

    //find a way to choose image, check if under 65536mb, and store
}

