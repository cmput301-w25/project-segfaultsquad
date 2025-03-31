package com.example.segfaultsquadapplication.display.following;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.segfaultsquadapplication.R;
import com.example.segfaultsquadapplication.impl.comment.Comment;

import java.util.List;

/**
 * Adapter for displaying comments in a RecyclerView, binds each comment object to a view
 * shows username and comment text.
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
     * Called to bind a comment to the view holder, sets the username and comment text for each comment
     * @param holder CommentViewHolder to bind the data to.
     * @param position position of the comment in the list.
     */

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.username.setText(comment.getUsername());
        holder.commentText.setText(comment.getText());
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

        /**
         * initializing the ViewHolder with the item view.
         * @param itemView item view containing the UI elements.
         */
        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.comment_username);
            commentText = itemView.findViewById(R.id.comment_text);
        }
    }
}