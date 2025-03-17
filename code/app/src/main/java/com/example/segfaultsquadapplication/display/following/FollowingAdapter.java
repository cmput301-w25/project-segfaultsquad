/**
 * Classname: FollowingAdapter
 * Version Info: Initial
 * Date: March 7, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */
package com.example.segfaultsquadapplication.display.following;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.user.User;

import java.util.List;

public class FollowingAdapter extends RecyclerView.Adapter<FollowingAdapter.ViewHolder> {

    private List<User> followingList;
    private OnFollowingActionListener listener;

    public interface OnFollowingActionListener {
        void onFollowingAction(User user);
    }

    public FollowingAdapter(List<User> followingList, OnFollowingActionListener listener) {
        this.followingList = followingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_following, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = followingList.get(position);
        holder.username.setText(user.getUsername());
        // Load user profile picture using an image loading library (e.g., Glide or
        // Picasso)
        // Glide.with(holder.itemView).load(user.getProfilePictureUrl()).into(holder.profilePicture);

        // Add click listener to the entire item view
        holder.itemView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("userId", user.getUserId());
            Navigation.findNavController(v).navigate(R.id.action_to_profile, args);
        });

        holder.followingButton.setText("Following");
        holder.followingButton.setOnClickListener(v -> {
            // Toggle follow/unfollow action
            listener.onFollowingAction(user);
            // Optionally, change the button text to "Unfollowed" after clicking
            holder.followingButton.setText("Unfollowed");
        });
    }

    @Override
    public int getItemCount() {
        return followingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        ImageView profilePicture;
        Button followingButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
            profilePicture = itemView.findViewById(R.id.profile_picture);
            followingButton = itemView.findViewById(R.id.following_button);
        }
    }
}
