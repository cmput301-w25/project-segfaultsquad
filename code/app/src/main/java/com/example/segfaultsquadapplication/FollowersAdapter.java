/**
 * Classname: FollowersAdapter
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FollowersAdapter extends RecyclerView.Adapter<FollowersAdapter.ViewHolder> {

    private List<User> followersList;
    private OnFollowerActionListener listener;

    public interface OnFollowerActionListener {
        void onFollowerAction(User user, boolean isFollowing);
    }

    public FollowersAdapter(List<User> followersList, OnFollowerActionListener listener) {
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
        holder.username.setText(user.getUsername());
        // Load user profile picture using an image loading library (e.g., Glide or
        // Picasso)
        // Glide.with(holder.itemView).load(user.getProfilePictureUrl()).into(holder.profilePicture);

        holder.removeButton.setOnClickListener(v -> listener.onFollowerAction(user, true)); // Unfollow
        holder.followBackButton.setOnClickListener(v -> listener.onFollowerAction(user, false)); // Follow back
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
    }
}
