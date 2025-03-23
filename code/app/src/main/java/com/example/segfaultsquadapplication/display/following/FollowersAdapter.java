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

import java.util.List;

public class FollowersAdapter extends RecyclerView.Adapter<FollowersAdapter.ViewHolder> {

    private List<User> followersList;
    private OnFollowerClickListener listener;

    public interface OnFollowerClickListener {
        void onRemoveFollower(User user);

        void onFollowBack(User user);
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

            // Set click listeners for buttons
            removeButton.setOnClickListener(v -> listener.onRemoveFollower(user));
            followBackButton.setOnClickListener(v -> listener.onFollowBack(user));
        }

        // Add method to control button visibility
        public void setFollowBackButtonVisibility(boolean show) {
            followBackButton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}
