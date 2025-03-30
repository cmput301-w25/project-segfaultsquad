/**
 * Classname: FollowersAdapter
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
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
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FollowersAdapter extends RecyclerView.Adapter<FollowersAdapter.ViewHolder> {

    private List<User> followersList;
    private OnFollowerClickListener listener;

    public interface OnFollowerClickListener {
        void onRemoveFollower(User user);

        void onFollowBack(User user, ViewHolder viewHolder);
    }

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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        ImageView profilePicture;
        Button removeButton;
        Button followBackButton;
        private boolean isFollowing = false;
        private boolean followRequestSent = false;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            profilePicture = itemView.findViewById(R.id.profile_picture);
            removeButton = itemView.findViewById(R.id.remove_button);
            followBackButton = itemView.findViewById(R.id.follow_back_button);
        }

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

        public void updateFollowStatus(boolean isFollowing, boolean followRequestSent) {
            this.isFollowing = isFollowing;
            this.followRequestSent = followRequestSent;
            setFollowBackButtonState();
        }

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


        private void checkFollowStatus(User user) {
            String currentUserId = UserManager.getUserId();
            AtomicReference<User> holder = new AtomicReference<>();
            UserManager.loadUserData(user.getDbFileId(), holder,
                    isSuccess -> {
                        if (isSuccess) {
                            boolean isFollowingNow = holder.get().getFollowers() != null &&
                                    holder.get().getFollowers().contains(currentUserId);
                            boolean followRequestSentNow = holder.get().getFollowRequests() != null &&
                                    holder.get().getFollowRequests().contains(currentUserId);

                            this.isFollowing = isFollowingNow;
                            this.followRequestSent = followRequestSentNow;
                            setFollowBackButtonState();
                        }
                    });
        }
    }
}
