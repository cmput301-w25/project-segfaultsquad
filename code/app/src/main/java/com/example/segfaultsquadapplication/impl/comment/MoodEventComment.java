package com.example.segfaultsquadapplication.impl.comment;

import com.example.segfaultsquadapplication.impl.db.IDbData;

import java.util.ArrayList;

public class MoodEventComment implements IDbData {
    // attributes
    private String commentId;
    private String eventId;
    private String senderId;
    private String commentContent;

    // Constructor(s)
    public MoodEventComment(String eventId, String senderId, String commentContent) {
        this.eventId = eventId;
        this.senderId = senderId;
        this.commentContent = commentContent;
    }

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
