package com.example.segfaultsquadapplication.impl.moodevent;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Encapsulates MoodEvent's related interactions with database. </br>
 * The display section of code should be completely decoupled with document snapshots. </br>
 * Only the logic to handle mood events and respond to exceptions are needed.
 */
public class MoodEventManager {
    private static final String LOG_TITLE = "MoodEventManager";
    public enum MoodEventFilter {
        ALL(UnaryOperator.identity()),
        MOST_RECENT( query -> query.limit(1) );

        private final UnaryOperator<Query> queryFilter;
        MoodEventFilter(UnaryOperator<Query> queryFilter) {
            this.queryFilter = queryFilter;
        }
    }

    public static void getAllMoodEvents(@Nullable String userId, MoodEventFilter filter,
                                        Collection<MoodEvent> holder, Consumer<Boolean> onComplete) {
        // Restrict to the current user & order by time, then apply further filters
        // Recall that the argument in compose is executed first.
        Function<Query, Query> operator = filter.queryFilter;
        if (userId != null) {
            operator = operator.compose( (Query query) -> query.whereEqualTo("userId", userId) );
        }
        operator = operator.compose( (Query query) -> query.orderBy("timestamp", Query.Direction.DESCENDING) );

        DbUtils.queryObjects(DbUtils.COLL_MOOD_EVENTS,
                operator, MoodEvent.class, holder,
                new DbOpResultHandler<>(
                        success -> onComplete.accept(true),
                        error -> {
                            Log.e(LOG_TITLE, "Error retrieving my mood events: ", error);
                            onComplete.accept(false);
                        }
                )
        );
    }

    /**
     * Adds a new mood event into the database.
     * If it successes, its allocated file name.
     * @param moodEvent The mood event object.
     * @param callback The callback when the file is saved(true) or can not be saved(false).
     */
    public static void addMoodEvent(MoodEvent moodEvent, Consumer<Boolean> callback) {
        DbOpResultHandler<DocumentReference> handler = new DbOpResultHandler<>(
                // Success
                Void -> {
                    callback.accept(true);
                },
                // Failure
                e -> {
                    Log.e(LOG_TITLE, "Can not add mood event", e);
                    callback.accept(false);
                }
        );
        DbUtils.addObjectToCollection(DbUtils.COLL_MOOD_EVENTS, moodEvent, handler);
    }

    /**
     * Retrieves a mood event with its document ID in the database.
     * @param moodId The document moodId.
     * @param holder This will contain the mood event
     * @param callback Called when finished, whether the retrieval is a success(true) or failure(false).
     */
    public static void getMoodEventById(String moodId, AtomicReference<MoodEvent> holder, Consumer<Boolean> callback) {
        DbOpResultHandler<DocumentSnapshot> handler = new DbOpResultHandler<>(
                // Success
                Void -> callback.accept(true),
                // Failure
                e -> {
                    Log.e(LOG_TITLE, "Error getting mood event by ID: ", e);
                    callback.accept(false);
                }
        );
        DbUtils.getObjectByDocId(DbUtils.COLL_MOOD_EVENTS, moodId, MoodEvent.class, holder, handler);
    }

    /**
     * Updates a mood event with its internal document ID in the database.
     * @param moodEvent The mood event object.
     * @param callback The callback when the file is saved(true) or can not be saved(false).
     */
    public static void updateMoodEventById(MoodEvent moodEvent, Consumer<Boolean> callback) {
        DbOpResultHandler<Void> handler = new DbOpResultHandler<>(
                // Success
                Void -> callback.accept(true),
                // Failure
                e -> {
                    Log.e(LOG_TITLE, "Can not update mood event by ID", e);
                    callback.accept(false);
                }
        );
        DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, moodEvent.getDbFileId(),
                documentReference -> documentReference.set(moodEvent), handler);
    }

    /**
     * Deletes a mood event with its document ID in the database.
     * @param id The document id.
     * @param callback The callback when the file is deleted(true) or can not be deleted(false).
     */
    public static void deleteMoodEventById(String id, Consumer<Boolean> callback) {
        DbOpResultHandler<Void> handler = new DbOpResultHandler<>(
                // Success
                Void -> callback.accept(true),
                // Failure
                e -> {
                    Log.e(LOG_TITLE, "Can not delete mood event by id", e);
                    callback.accept(false);
                }
        );
        DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, id, DocumentReference::delete, handler);
    }
}
