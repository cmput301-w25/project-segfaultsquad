/**
 * Classname: MoodEvent
 * Version Info: Initial
 * Date: Feb 16, 2025
 * CopyRight Notice: All rights Reserved Suryansh Khranger 2025
 */

package com.example.segfaultsquadapplication.impl.moodevent;

// imports
import android.content.Context;

import com.example.segfaultsquadapplication.R;
import com.google.firebase.firestore.GeoPoint;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.firebase.Timestamp;

public class MoodEvent {
    // Required attributes
    private String moodId; // Unique identifier for the mood event
    private String userId; // User ID of the person creating the mood event
    private Timestamp timestamp; // Date and time of the mood event
    private MoodType moodType; // Emotional state
    private String reasonText; // Reason text (optional)
    private List<Integer> imageData; // Reason image data (optional)
    private GeoPoint location; // Location of the mood event

    // Optional attributes
    private String trigger; // Trigger for the mood (optional)
    private SocialSituation SocialSituation; // Social situation (optional)

    // Enum for mood types
    public enum MoodType {
        ANGER(R.color.mood_anger, R.color.mood_anger_light, "😡"),
        CONFUSION(R.color.mood_confusion, R.color.mood_confusion_light, "😵‍💫"),
        DISGUST(R.color.mood_disgust, R.color.mood_disgust_light, "🤢"),
        FEAR(R.color.mood_fear, R.color.mood_fear_light, "😨"),
        HAPPINESS(R.color.mood_happiness, R.color.mood_happiness_light, "😀"),
        SADNESS(R.color.mood_sadness, R.color.mood_sadness_light, "😭"),
        SHAME(R.color.mood_shame, R.color.mood_shame_light, "😳"),
        SURPRISE(R.color.mood_surprise, R.color.mood_surprise_light, "🤯");

        final int colorId, colorSecondaryId;
        final String emoticon;
        MoodType(int colorId, int colorSecondaryId, String emoticon) {
            this.colorId = colorId;
            this.colorSecondaryId = colorSecondaryId;
            this.emoticon = emoticon;
        }

        public int getPrimaryColor(Context ctx) {
            return ctx.getColor(colorId);
        }

        public int getSecondaryColor(Context ctx) {
            return ctx.getColor(colorSecondaryId);
        }

        public String getEmoticon() {
            return emoticon;
        }

        public static String[] getAllEmoticons() {
            return Arrays.stream(values())
                    .map(MoodType::getEmoticon)
                    .toArray(String[]::new);
        }
    }

    // Enum for social situations
    public enum SocialSituation {
        ALONE("Alone"),
        WITH_ONE_PERSON("With One Person"),
        WITH_GROUP("With a Group"),
        IN_CROWD("In a Crowd");

        private final String displayName;

        SocialSituation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Constructor
    public MoodEvent(String userId, MoodType moodType, String reasonText, List<Integer> imageData,
                     GeoPoint location) {
        this.userId = userId;
        this.timestamp = new Timestamp(new Date());
        this.moodType = moodType;
        this.reasonText = reasonText;
        this.imageData = imageData;
        this.location = location;

        // Validate that at least one of reasonText or imageData is provided
        if (reasonText == null && (imageData == null || imageData.isEmpty())) {
            throw new IllegalArgumentException("Either reasonText or imageData must be provided.");
        }
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

    public SocialSituation getSocialSituation() {
        return SocialSituation;
    }

    public void setSocialSituation(SocialSituation SocialSituation) {
        this.SocialSituation = SocialSituation;
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

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }
}