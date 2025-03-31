package com.example.segfaultsquadapplication.impl.comment;

import com.example.segfaultsquadapplication.display.following.CommentsAdapter;
import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manager class for handling comments related to mood events.
 */
public class CommentManager {
    /**
     * Fetches comments for a specific mood event.
     *
     * @param moodId          The ID of the mood event.
     * @param comments        The list to populate with comments.
     * @param callback        The callback for success / failure.
     */
    public static void getCommentsForMood(String moodId, List<Comment> comments, Consumer<Boolean> callback) {
        // Fetch comments from Firestore and add to the comments list
        DbUtils.queryObjects(DbUtils.COLL_COMMENTS, qry -> qry.whereEqualTo("eventId", moodId),
                Comment.class, comments, new DbOpResultHandler<>(
                        result -> {
                            callback.accept(true);
                        },
                        e -> {
                            e.printStackTrace(System.err);
                            callback.accept(false);
                        }
                ));
    }

    /**
     * Submits a comment for a specific mood event.
     *
     * @param comment The comment to submit.
     */
    public static void submitComment(Comment comment) {
        // Submit the comment to Firestore
        DbUtils.addObjectToCollection(DbUtils.COLL_COMMENTS, comment, new DbOpResultHandler<>(
                null,
                e -> e.printStackTrace(System.err)
        ));
    }

    /**
     * Deletes all comments for a specific mood event.
     *
     * @param eventId The event to remove comments for.
     */
    public static void removeAllComments(String eventId) {
        ArrayList<Comment> toRemove = new ArrayList<>();
        getCommentsForMood(eventId, toRemove,
                isSuccess -> {
                    if (! isSuccess) return;
                    // Delete those comments
                    for (Comment cmt : toRemove) {
                        DbUtils.operateDocumentById(DbUtils.COLL_COMMENTS, cmt.getDbFileId(),
                                DocumentReference::delete, new DbOpResultHandler<>(
                                        null,
                                        e -> e.printStackTrace(System.err)
                                ));
                    }
                });
    }
}