/**
 * Classname: MoodEvent
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication;

// imports
import android.util.Log;
import android.content.Context;

import com.google.firebase.firestore.GeoPoint;
import java.util.Date;
import java.util.List;
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
    // since firestore made the Storage thing private recently, im gonna store
    // images as BLOBs in the db itself and de-construct and reconstruct as
    // necessary
    // private List<Byte> imageData; // For storing the image as a byte array
    // Change from byte[] to List<Byte> cuz firestore doesn allow serializing of
    // byte arrays directly
    // NOPE
    private List<Integer> imageData; // For storing the image as a integer array
    // apparetnly firestore also doesnt support serializing Byte objects, so using
    // numeric objects (int) now

    // Enum for mood types
    public enum MoodType {
        ANGER, CONFUSION, DISGUST, FEAR, HAPPINESS, SADNESS, SHAME, SURPRISE
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
    /**
     * Constructor. Was needed in earlier iteration of code. Not being used
     * anywhere.
     */
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

    // image data methods
    // getter and setters for imageData
    /**
     * gets the image data from int array to deconstruct images
     * 
     * @return
     *         returns the int array constructed
     */
    public List<Integer> getImageData() {
        return imageData;
    }

    /**
     * * gets the image data from int array to reconstruct images
     * 
     * @param imageData
     *                  the gotten int array imageData
     */
    public void setImageData(List<Integer> imageData) {
        this.imageData = imageData;
    }

    public int getPrimaryColor(Context context) {
        switch (moodType) {
            case ANGER:
                Log.d("MoodEvent", "RECOGNIZED ANGER");
                return context.getColor(R.color.mood_anger);
            case CONFUSION:
                return context.getColor(R.color.mood_confusion);
            case DISGUST:
                return context.getColor(R.color.mood_disgust);
            case FEAR:
                return context.getColor(R.color.mood_fear);
            case HAPPINESS:
                return context.getColor(R.color.mood_happiness);
            case SADNESS:
                return context.getColor(R.color.mood_sadness);
            case SHAME:
                return context.getColor(R.color.mood_shame);
            case SURPRISE:
                return context.getColor(R.color.mood_surprise);
            default:
                return context.getColor(R.color.mood_default);
        }
    }

    public int getSecondaryColor(Context context) {
        switch (moodType) {
            case ANGER:
                return context.getColor(R.color.mood_anger_light);
            case CONFUSION:
                return context.getColor(R.color.mood_confusion_light);
            case DISGUST:
                return context.getColor(R.color.mood_disgust_light);
            case FEAR:
                return context.getColor(R.color.mood_fear_light);
            case HAPPINESS:
                return context.getColor(R.color.mood_happiness_light);
            case SADNESS:
                return context.getColor(R.color.mood_sadness_light);
            case SHAME:
                return context.getColor(R.color.mood_shame_light);
            case SURPRISE:
                return context.getColor(R.color.mood_surprise_light);
            default:
                return context.getColor(R.color.mood_default);
        }
    }
}
