package com.example.segfaultsquadapplication.impl.following;

import com.example.segfaultsquadapplication.impl.db.IDbData;

public class Following implements IDbData {
    private String dbFileId; // Unique identifier for the following relationship
    private String followerId; // User ID of the follower
    private String followedId; // User ID of the followed

    public Following(String followerId, String followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
    }

    @Override
    public void setDbFileId(String id) {
        dbFileId = id;
    }

    @Override
    public String getDbFileId() {
        return dbFileId;
    }

    public String getFollowerId() {
        return followerId;
    }

    public void setFollowerId(String followerId) {
        this.followerId = followerId;
    }

    public String getFollowedId() {
        return followedId;
    }

    public void setFollowedId(String followedId) {
        this.followedId = followedId;
    }
}
