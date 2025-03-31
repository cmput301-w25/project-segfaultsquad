package com.example.segfaultsquadapplication.impl.comment;

import com.example.segfaultsquadapplication.impl.db.IDbData;

import java.util.ArrayList;

/**
 * Represents particular comment on a mood event, file used for storing and managing it
 * comments associated with specific mood events.
 */
public class MoodEventComment implements IDbData {
    // attributes
    private String commentId;
    private String eventId;
    private String senderId;
    private String commentContent;

    /**
     * constructor to create a new MoodEventComment with specified details.
     * @param eventId ID of mood event this comment is associated with
     * @param senderId ID of user that made this comment
     * @param commentContent the comment itself
     */
    public MoodEventComment(String eventId, String senderId, String commentContent) {
        this.eventId = eventId;
        this.senderId = senderId;
        this.commentContent = commentContent;
    }

    /**
     * Default constructor required for Firestore to be able to deserialize objects from the database.
     */
    public MoodEventComment() {
        // Required for Firestore to be able to deserialize objects form the db
    }

    @Override
    public void setDbFileId(String id) {
        this.commentId = id;
    }

    @Override
    public String getDbFileId() {
        return commentId;
    }
}
