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

import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;

import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Classname: MoodAdapter
 * Version Info: Initial
 * Date: Feb 18, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 * 
 * Adapter for the mood events list in the MyMoodHistoryFragment
 */
public class MoodAdapter extends RecyclerView.Adapter<MoodAdapter.MoodViewHolder> {
    // attributes
    private List<MoodEvent> moodList;
    private OnMoodClickListener listener;
    private String currentUserId;
    private User currentUser; // To hold user data
    private Map<String, User> userCache; // Cache to store users we've already fetched

    // interfaces
    /**
     * interface for MoodAdapter class
     */
    public interface OnMoodClickListener {
        void onMoodClick(MoodEvent mood);
    }

    /**
     * Interface for receiving user data from Firestore
     */
    public interface UserCallback {
        void onUserLoaded(User user);
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
        this.userCache = new HashMap<>();
        fetchCurrentUser(); // Fetch user details
    }

    /**
     * Method to fetch current user details from Firestore
     */
    private void fetchCurrentUser() {
        AtomicReference<User> holder = new AtomicReference<>();
        UserManager.loadUserData(currentUserId, holder,
                isSuccess -> {
                    if (isSuccess) {
                        currentUser = holder.get();
                    }
                });
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

        // Get the user who created this mood
        getMoodUser(mood, user -> {
            // We now have the user, update the view with this information
            holder.bind(mood, user);
        });
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
     *                 the updated list of MoodEvents
     */
    public void updateMoods(List<MoodEvent> newMoods) {
        this.moodList = newMoods;
        notifyDataSetChanged();
    }

    /**
     * helper method to get the user of the moodevent arg
     * 
     * @param m        the passed mood event object whose user we want to get
     * @param callback callback to receive the user when loaded
     */
    public void getMoodUser(MoodEvent m, UserCallback callback) {
        // get the userId field of this mood event
        String moodUserId = m.getUserId();

        // Use User Manager to get the user
        AtomicReference<User> holder = new AtomicReference<>();
        UserManager.loadUserData(moodUserId, holder,
                isSuccess -> callback.onUserLoaded(isSuccess ? holder.get() : null));
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
        private TextView textMoodVisibility;
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
            textMoodVisibility = itemView.findViewById(R.id.textMoodVisibility);
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
         *                 mood information object (MoodEvent)
         * @param moodUser
         *                 user who created the mood
         */
        public void bind(MoodEvent mood, User moodUser) {
            // Set mood emoji
            moodEmoji.setText(mood.getMoodType().getEmoticon());
            // set reason text ("IMAGE if image reason)")
            textMoodType.setText(mood.getMoodType().name());
            if (mood.getReasonText().isEmpty() & mood.getImageData() != null) {
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

            if (mood.isPublic()) {
                textMoodVisibility.setText("Public");
                textMoodVisibility.setVisibility(View.VISIBLE);
            } else {
                textMoodVisibility.setText("Private");
                textMoodVisibility.setVisibility(View.VISIBLE);
            }

            // Set username
            username.setText(moodUser.getUsername());
            // Set profile picture
            if (moodUser != null) {
                List<Integer> profilePicData = moodUser.getProfilePicUrl();
                // debugging
                Log.d("MoodAdapter", "profilePicData: " + profilePicData);
                if (profilePicData != null) {
                    // debugging
                    Log.d("MoodAdapter", "profilePicData.isEmpty(): " + profilePicData.isEmpty());

                    // if its empty (should even be possible...)
                    if (profilePicData.isEmpty()) {
                        // set to the default pfp
                        profilePicture.setImageResource(R.drawable.profile_icon); // Default image
                    } else {
                        // construct the image from data

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
                            profilePicture.setImageResource(R.drawable.profile_icon); // Default image
                        }
                    }

                } else {
                    // debugging
                    Log.d("MoodAdapter", "profilePicData was null: " + profilePicData);
                    profilePicture.setImageResource(R.drawable.profile_icon); // Default image
                }
            } else {
                Log.d("MoodAdapter", "Fialed to get user info for moodevent: " + mood.getDbFileId());
                // If user data couldn't be loaded, use default image and "Unknown" username
                profilePicture.setImageResource(R.drawable.profile_icon);
                username.setText("Unknown");
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