package com.example.segfaultsquadapplication;

// imports
import com.google.firebase.firestore.GeoPoint;
import java.util.Date;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class MoodEvent {
    // attributes
    private String moodId;
    private String userId;
    private Timestamp timestamp;
    private MoodType moodType;
    private String reasonText;
    private String reasonImageUrl;
    private SocialSituation socialSituation;
    private GeoPoint location;

    // Enum for mood types
    public enum MoodType {
        HAPPY, SAD, ANGRY, EXCITED, TIRED, SCARED, SURPRISED
    }

    // Enum for social situations
    public enum SocialSituation {
        ALONE,
        WITH_ONE_PERSON,
        WITH_GROUP,
        IN_CROWD
    }

    // Constructor(s)
    public MoodEvent(String userId, MoodType moodType, String reasonText) {
        this.userId = userId;
        this.moodType = moodType;
        this.reasonText = reasonText;
        this.timestamp = new Timestamp(new Date());
    }

    // Add a no-argument constructor for Firestore
    public MoodEvent() {
        // Required empty constructor for Firestore
    }

    // Other Methods
    // Getters and setters
    public String getMoodId() {
        return moodId;
    }

    public void setMoodId(String moodId) {
        this.moodId = moodId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestampDate() {
        return timestamp != null ? timestamp.toDate() : null;
    }

    public MoodType getMoodType() {
        return moodType;
    }

    public void setMoodType(MoodType moodType) {
        this.moodType = moodType;
    }

    public String getReasonText() {
        return reasonText;
    }

    public void setReasonText(String reasonText) {
        if (reasonText != null && reasonText.length() <= 20) {
            this.reasonText = reasonText;
        } else {
            throw new IllegalArgumentException("Reason text must be 20 characters or less");
        }
    }

    public String getReasonImageUrl() {
        return reasonImageUrl;
    }

    public void setReasonImageUrl(String reasonImageUrl) {
        this.reasonImageUrl = reasonImageUrl;
    }

    public SocialSituation getSocialSituation() {
        return socialSituation;
    }

    public void setSocialSituation(SocialSituation socialSituation) {
        this.socialSituation = socialSituation;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

}