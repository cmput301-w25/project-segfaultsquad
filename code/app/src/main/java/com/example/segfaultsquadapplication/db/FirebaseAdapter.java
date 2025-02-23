package com.example.segfaultsquadapplication.db;

import androidx.annotation.Nullable;

import com.example.segfaultsquadapplication.MoodEvent;


/**
 * Can be seen as the local "copy" of the database.
 * For now it is in place for reducing IO operation latency;
 * Later on, we iteratively include offline supports with this framework.
 */
public class FirebaseAdapter {
    // The local "database" copies; uses DataInstanceHolders.
    public static final LocalDbCopy<MoodEvent> moodEventDb = new LocalDbCopy<>("moods", MoodEvent.class);
}
