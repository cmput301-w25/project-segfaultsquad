/**
 * Classname: FollowingAdapter
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
                holder.profilePicture.setImageBitmap(bitmap);
            } else {
                holder.profilePicture.setImageResource(R.drawable.ic_person);
            }
        } else {
            holder.profilePicture.setImageResource(R.drawable.ic_person);
        }

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
