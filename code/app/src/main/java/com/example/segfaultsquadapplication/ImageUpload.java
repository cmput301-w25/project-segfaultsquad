package com.example.segfaultsquadapplication;

import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ImageUpload {
    private View view;
    private ImageView gallery;
    private Button btnGallery;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public ImageUpload(View view) {
        this.view = view;
    }

    public void view() {
        gallery = view.findViewById(R.id.uploaded_img);
        btnGallery = view.findViewById(R.id.upload_button);

        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); //action pick image fro m external content data source
                imagePickerLauncher.launch(intent); // Launch image picker
            }
        });
    }
}
