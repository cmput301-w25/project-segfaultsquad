package com.example.segfaultsquadapplication.display.following;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.display.profile.CircularImageView;
import com.example.segfaultsquadapplication.impl.comment.Comment;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Adapter for displaying comments in a RecyclerView, binds each comment object to a view
 * shows username, comment text, and profile picture.
 */
public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
    private List<Comment> comments;

    /**
     * initializing the adapter with a list of comments.
     * @param comments list of comments to be displayed in the RecyclerView.
     */
    public CommentsAdapter(List<Comment> comments) {
        this.comments = comments;
    }

    /**
     * when a new ViewHolder is created, inflates the layout for each comment item
     * @param parent parent view group the new item view will be attached to
     * @param viewType view type of the new item
     * @return new CommentViewHolder that holds the item view for a comment
     */
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    /**
     * Called to bind a comment to the view holder, sets the username, comment text, and profile picture for each comment
     * @param holder CommentViewHolder to bind the data to.
     * @param position position of the comment in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.username.setText(comment.getUsername());
        holder.commentText.setText(comment.getText());

        // Load profile picture for the comment user
        loadProfilePicture(comment.getUserId(), holder.profileIcon);
    }

    /**
     * Loads the profile picture for a user and sets it to the provided ImageView
     * @param userId ID of the user whose profile picture to load
     * @param profileIcon CircularImageView to set the profile picture to
     */
    private void loadProfilePicture(String userId, CircularImageView profileIcon) {
        // Default profile picture in case user data can't be loaded
        profileIcon.setImageResource(R.drawable.ic_person);

        // Load user data to get profile picture
        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(userId, userHolder, isSuccess -> {
            if (isSuccess) {
                User user = userHolder.get();
                if (user != null) {
                    List<Integer> profilePicData = user.getProfilePicUrl();
                    if (profilePicData != null && !profilePicData.isEmpty()) {
                        // Convert List<Integer> to byte[]
                        byte[] imageBytes = new byte[profilePicData.size()];
                        for (int i = 0; i < profilePicData.size(); i++) {
                            imageBytes[i] = profilePicData.get(i).byteValue();
                        }

                        // Convert byte[] to Bitmap
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        if (bitmap != null) {
                            profileIcon.setImageBitmap(bitmap);
                        }
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    /**
     * ViewHolder class that holds references to the UI elements for each comment item in the RecyclerView.
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        TextView commentText;
        CircularImageView profileIcon;

        /**
         * initializing the ViewHolder with the item view.
         * @param itemView item view containing the UI elements.
         */
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.comment_username);
            commentText = itemView.findViewById(R.id.comment_text);
            profileIcon = itemView.findViewById(R.id.comment_profile_icon);
        }
    }
}