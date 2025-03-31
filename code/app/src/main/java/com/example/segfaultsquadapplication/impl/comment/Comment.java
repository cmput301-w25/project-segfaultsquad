package com.example.segfaultsquadapplication.impl.comment;

import com.example.segfaultsquadapplication.impl.db.IDbData;

/**
 * Class representing a comment on a mood event, stores information about comment
 */
public class Comment implements IDbData {
    private String dbFileId;
    private String eventId;
    private String userId;
    private String username;
    private String text;

    /**
     * create a new comment object with the details
     * @param eventId the corresponding mood event's id
     * @param userId userid of user who made comment
     * @param username username of user who made comment
     * @param text comment itself
     */
    public Comment(String eventId, String userId, String username, String text) {
        this.eventId = eventId;
        this.userId = userId;
        this.username = username;
        this.text = text;
    }

    // Required for Firebase
    public Comment() {}

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getDbFileId() {
        return dbFileId;
    }

    @Override
    public void setDbFileId(String id) {
        this.dbFileId = id;
    }
}