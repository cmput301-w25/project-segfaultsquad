/**
 * Classname: MoodAdapter
 * Version Info: Initial
 * Date: Feb 18, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.display.moodhistory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.MoodViewHolder> {
    // attributes
    private List<MoodEvent> moodList;
    private OnMoodClickListener listener;
    private String currentUserId;
    private User currentUser; // To hold user data

    // interfaces
    public interface OnMoodClickListener {
        void onMoodClick(MoodEvent mood);
    }

    // methods
    // constructor(s)
    /**
     * constructor method
     *
     * @param listener
     *                 listerner to alert the adapter of updates
     */
    public MoodAdapter(OnMoodClickListener listener) {
        this.moodList = new ArrayList<>();
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user ID
        fetchCurrentUser(); // Fetch user details
    }

    // Method to fetch current user details from Firestore
    private void fetchCurrentUser() {
        AtomicReference<User> holder = new AtomicReference<>();
        DbUtils.getObjectByDocId(DbUtils.COLL_USERS, currentUserId, User.class, holder,
                new DbOpResultHandler<>(
                        result -> currentUser = holder.get(),
                        e -> Log.e("MoodAdapter", "Error fetching user data", e)));
    }

    // parent class methods
    @NonNull
    @Override
    public MoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood, parent, false);
        return new MoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodViewHolder holder, int position) {
        MoodEvent mood = moodList.get(position);
        holder.bind(mood);
    }

    @Override
    public int getItemCount() {
        return moodList.size();
    }

    // new methods
    /**
     * method to update the moods in ArrayList
     *
     * @param newMoods
     */
    public void updateMoods(List<MoodEvent> newMoods) {
        this.moodList = newMoods;
        notifyDataSetChanged();
    }

    /**
     * nested class for View holding Moods
     */
    class MoodViewHolder extends RecyclerView.ViewHolder {
        // attributes
        private MaterialCardView moodCard;
        private TextView moodEmoji;
        private TextView textMoodType;
        private TextView textReason;
        private TextView textTimestamp;
        private TextView textSocialSituation;
        private ImageView profilePicture;
        private TextView username;

        /**
         * constructor
         *
         * @param itemView
         *                 the view of the mood card
         */
        public MoodViewHolder(@NonNull View itemView) {
            super(itemView);
            moodCard = (MaterialCardView) itemView;
            moodEmoji = itemView.findViewById(R.id.moodEmoji);
            textMoodType = itemView.findViewById(R.id.textMoodType);
            textReason = itemView.findViewById(R.id.textReason);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textSocialSituation = itemView.findViewById(R.id.textSocialSituation);
            profilePicture = itemView.findViewById(R.id.profile_picture);
            username = itemView.findViewById(R.id.username);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMoodClick(moodList.get(position));
                }
            });
        }

        /**
         * method to create the UI of the mood event
         *
         * @param mood
         *             mood information object (MoodEvent)
         */
        public void bind(MoodEvent mood) {
            // Set mood emoji
            moodEmoji.setText(mood.getMoodType().getEmoticon());
            // set reason text ("IMAGE if image reason)")
            textMoodType.setText(mood.getMoodType().name());
            if (mood.getReasonText().isEmpty() && mood.getImageData() != null) {
                textReason.setText("IMAGE");
            } else {
                textReason.setText(mood.getReasonText());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault());
            textTimestamp.setText(sdf.format(mood.getTimestampDate()));

            if (mood.getSocialSituation() != null) {
                textSocialSituation.setText(mood.getSocialSituation().toString());
                textSocialSituation.setVisibility(View.VISIBLE);
            } else {
                textSocialSituation.setVisibility(View.GONE);
            }

            // Set profile picture
            if (currentUser != null) {
                List<Integer> profilePicData = currentUser.getProfilePicUrl();
                if (profilePicData != null && !profilePicData.isEmpty()) {
                    // Convert List<Integer> back to byte array
                    byte[] imageBytes = new byte[profilePicData.size()];
                    for (int i = 0; i < profilePicData.size(); i++) {
                        imageBytes[i] = profilePicData.get(i).byteValue();
                    }

                    // Decode byte array to Bitmap
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    if (bitmap != null) {
                        profilePicture.setImageBitmap(bitmap);
                    } else {
                        profilePicture.setImageResource(R.drawable.ic_person); // Default image
                    }
                } else {
                    profilePicture.setImageResource(R.drawable.ic_person); // Default image
                }

                // Set username
                username.setText(currentUser.getUsername());
            }

            // Set mood colors
            int moodColor = mood.getMoodType().getPrimaryColor(itemView.getContext());
            int backgroundColor = mood.getMoodType().getSecondaryColor(itemView.getContext());

            moodCard.setStrokeColor(moodColor);
            moodCard.setCardBackgroundColor(backgroundColor);
        }
    }

    // AsyncTask to load image from URL
    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;

        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("LoadImageTask", "Error loading image", e);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                imageView.setImageBitmap(result);
            } else {
                imageView.setImageResource(R.drawable.ic_person); // Default image
            }
        }
    }
}