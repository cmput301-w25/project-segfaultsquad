/**
 * Classname: User
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.impl.user;

import java.util.List;
import java.util.ArrayList;

public class User {
    // atteibutes
    private String userId;
    private String username;
    private String email;
    private String profilePicUrl;
    private List<String> followers;
    private List<String> following;

    // Constructor(s)
    public User(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profilePicUrl = ""; // init as empty
        this.followers = new ArrayList<>(); // init as empty
        this.following = new ArrayList<>(); // init as empty
    }

    // Getters and setters
    public String getUserId() {
        return userId;
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

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
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
}
