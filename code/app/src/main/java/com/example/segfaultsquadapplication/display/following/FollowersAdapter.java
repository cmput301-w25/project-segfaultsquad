package com.example.segfaultsquadapplication.display.following;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Classname: FollowersAdapter
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 *
 * This class is an adapter for displaying a list of followers in a
 * RecyclerView.
 * It binds follower data to the UI components and handles user interactions
 * such as
 * removing a follower or following back a user.
 *
 * Outstanding Issues: None
 */
public class FollowersAdapter extends RecyclerView.Adapter<FollowersAdapter.ViewHolder> {
    // attributes
    private List<User> followersList;
    private OnFollowerClickListener listener;

    /**
     * Interface for handling follower click events.
     */
    public interface OnFollowerClickListener {
        /**
         * Called when a follower is removed.
         *
         * @param user The user to be removed.
         */
        void onRemoveFollower(User user);

        /**
         * Called when a follow back action is initiated.
         *
         * @param user       The user to follow back.
         * @param viewHolder The ViewHolder associated with the user.
         */
        void onFollowBack(User user, ViewHolder viewHolder);
    }

    /**
     * constructor
     * 
     * @param followersList
     *                      the list of User objects who are folllowing current user
     * @param listener
     *                      listener object to update the list
     */
    public FollowersAdapter(List<User> followersList, OnFollowerClickListener listener) {
        this.followersList = followersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_follower, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = followersList.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return followersList.size();
    }

    /**
     * sub class viewholder for holding follwer items
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // attributes
        TextView username;
        ImageView profilePicture;
        Button removeButton;
        Button followBackButton;
        private boolean isFollowing = false;
        private boolean followRequestSent = false;

        /**
         * Constructor for the ViewHolder.
         *
         * @param itemView The view for the item.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            profilePicture = itemView.findViewById(R.id.profile_picture);
            removeButton = itemView.findViewById(R.id.remove_button);
            followBackButton = itemView.findViewById(R.id.follow_back_button);
        }

        /**
         * Binds the user data to the UI components.
         *
         * @param user     The user to bind.
         * @param listener The listener for click events.
         */
        public void bind(User user, OnFollowerClickListener listener) {
            username.setText(user.getUsername());

            // Set profile picture
            List<Integer> profilePicData = user.getProfilePicUrl();
            if (profilePicData != null && !profilePicData.isEmpty()) {
                // Convert List<Integer> back to byte array
                byte[] imageBytes = new byte[profilePicData.size()];
                for (int i = 0; i < profilePicData.size(); i++) {
                    imageBytes[i] = profilePicData.get(i).byteValue();
                }

                // Convert to Bitmap and set
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    profilePicture.setImageBitmap(bitmap);
                } else {
                    profilePicture.setImageResource(R.drawable.ic_person);
                }
            } else {
                profilePicture.setImageResource(R.drawable.ic_person);
            }

            checkFollowStatus(user);

            // Set click listeners for buttons
            removeButton.setOnClickListener(v -> listener.onRemoveFollower(user));
            followBackButton.setOnClickListener(v -> {
                if (!isFollowing && !followRequestSent) {
                    listener.onFollowBack(user, this);
                }
            });
        }

        /**
         * method to update following status
         * 
         * @param isFollowing
         *                          bool value for if user is following the other
         * @param followRequestSent
         *                          bool value for is a following request has been sent
         */
        public void updateFollowStatus(boolean isFollowing, boolean followRequestSent) {
            this.isFollowing = isFollowing;
            this.followRequestSent = followRequestSent;
            setFollowBackButtonState();
        }

        /**
         * settor method for the follow back button
         */
        private void setFollowBackButtonState() {
            if (isFollowing) {
                followBackButton.setText("Following");
                followBackButton.setEnabled(false);
            } else if (followRequestSent) {
                followBackButton.setText("Requested");
                followBackButton.setEnabled(false);
            } else {
                followBackButton.setText("Follow Back");
                followBackButton.setEnabled(true);
            }
        }

        /**
         * helper method to check if one user is following another
         * 
         * @param user
         *             user to check (if they are following current user)
         */
        private void checkFollowStatus(User user) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String currentUserId = auth.getCurrentUser().getUid();

            db.collection("following")
                    .whereEqualTo("followerId", currentUserId)
                    .whereEqualTo("followedId", user.getDbFileId())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        boolean isFollowingNow = !querySnapshot.isEmpty();

                        db.collection("users")
                                .document(user.getDbFileId())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    List<String> followRequests = (List<String>) documentSnapshot.get("followRequests");
                                    boolean followRequestSentNow = followRequests != null
                                            && followRequests.contains(currentUserId);

                                    this.isFollowing = isFollowingNow;
                                    this.followRequestSent = followRequestSentNow;
                                    setFollowBackButtonState();
                                });
                    });
        }
    }
}
