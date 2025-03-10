package com.example.segfaultsquadapplication;
com.example.segfaultsquadapplication.MoodEvent.MoodType

public class MoodCreateUITest {
    public enum MoodType {
        HAPPY, SAD, ANGRY, EXCITED, TIRED, SCARED, SURPRISED
    }
    private String userId = "User01";
    private MoodEvent.MoodType selectedMoodType = MoodEvent.MoodType.valueOf("HAPPY");
    private String reason = "The tests are taking too long";
    private MoodEvent newMood = new MoodEvent(userId, selectedMoodType, reason);
}
