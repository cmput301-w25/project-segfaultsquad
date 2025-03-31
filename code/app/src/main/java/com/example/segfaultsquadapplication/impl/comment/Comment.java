package com.example.segfaultsquadapplication.impl.comment;

/**
 * Class representing a comment on a mood event.
 */
public class Comment {
    private String userId;
    private String username;
    private String text;

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