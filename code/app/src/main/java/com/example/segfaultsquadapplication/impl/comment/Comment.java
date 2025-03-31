package com.example.segfaultsquadapplication.impl.comment;

/**
 * Class representing a comment on a mood event, stores information about comment
 */
public class Comment {
    private String userId;
    private String username;
    private String text;

    /**
     * create a new comment object with the details
     * @param userId userid of user who made comment
     * @param username username of user who made comment
     * @param text comment itself
     */
    public Comment(String userId, String username, String text) {
        this.userId = userId;
        this.username = username;
        this.text = text;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getText() {
        return text;
    }
}