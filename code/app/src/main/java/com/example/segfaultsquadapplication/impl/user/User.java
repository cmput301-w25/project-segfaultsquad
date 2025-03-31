/**
 * Classname: User
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.impl.user;

import com.example.segfaultsquadapplication.impl.db.IDbData;

import java.util.List;
import java.util.ArrayList;

/**
 * This file is to save the details of a user upon retrieval from firebase
 * Uses getters and setters for user details querying
 */
public class User implements IDbData {
    // atteibutes
    private String userId;
    private String username;
    private String email;
    private List<Integer> profilePicUrl;
    private List<String> followers;
    private List<String> following;
    private List<String> followRequests;

    // Constructor(s)

    /**
     * take in user data and assign it to variables for this class
     * @param userId is the userID of current user
     * @param username is the username of current user
     * @param email is the email of current user
     */
    public User(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profilePicUrl = new ArrayList<>();
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
        this.followRequests = new ArrayList<>();
    }

    public User() {
        // Required for Firestore to be able to deserialize objects form the db
    }

    // Getters and setters
    @Override
    public String getDbFileId() {
        return userId;
    }

    @Override
    public void setDbFileId(String id) {
        this.userId = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public List<Integer> getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(List<Integer> profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }

    public List<String> getFollowers() {
        return followers;
    }

    public List<String> getFollowing() {
        return following;
    }

    public void addFollower(String userId) {
        if (!followers.contains(userId)) {
            followers.add(userId);
        }
    }

    public void addFollowing(String userId) {
        if (!following.contains(userId)) {
            following.add(userId);
        }
    }

    public void removeFollower(String userId) {
        followers.remove(userId);
    }

    public void removeFollowing(String userId) {
        following.remove(userId);
    }

    public List<String> getFollowRequests() {
        return followRequests;
    }

    public int getFollowRequestCount() {
        return followRequests.size();
    }

    public void addFollowRequest(String userId) {
        if (!followRequests.contains(userId)) {
            followRequests.add(userId);
        }
    }

    public void removeFollowRequest(String userId) {
        followRequests.remove(userId);
    }
}
