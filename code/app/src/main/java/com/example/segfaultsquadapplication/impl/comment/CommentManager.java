package com.example.segfaultsquadapplication.impl.comment;

import com.example.segfaultsquadapplication.display.following.CommentsAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Manager class for handling comments related to mood events
 */
public class CommentManager {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Fetches comments for a specific mood event
     * @param moodId ID of the mood event.
     * @param comments list to populate with comments.
     * @param commentsAdapter adapter to notify of data changes.
     */
    public static void getCommentsForMood(String moodId, List<Comment> comments, CommentsAdapter commentsAdapter) {
        // Fetch comments from Firestore and add to the comments list
        // Example Firestore query to get comments for the moodId
        db.collection("moodComments").document(moodId).collection("comments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String userId = document.getString("userId");
                            String username = document.getString("username");
                            String text = document.getString("text");
                            comments.add(new Comment(userId, username, text));
                        }
                        commentsAdapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * submits comment for a specific mood event
     * @param moodId  ID of the mood event
     * @param comment comment to submit
     */
    public static void submitComment(String moodId, Comment comment) {
        // Submit the comment to Firestore
        db.collection("moodComments").document(moodId).collection("comments").add(comment)
                .addOnSuccessListener(documentReference -> {
                    // Comment submitted successfully
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                });
    }
}