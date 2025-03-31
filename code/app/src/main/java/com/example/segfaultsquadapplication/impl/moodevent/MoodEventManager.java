package com.example.segfaultsquadapplication.impl.moodevent;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import com.example.segfaultsquadapplication.impl.comment.CommentManager;
import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.location.LocationManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Encapsulates MoodEvent's related interactions with database. <br>
 * The display section of code should be completely decoupled with document snapshots. <br>
 * Only the logic to handle mood events and respond to exceptions are needed.
 */
public class MoodEventManager {
    private static final int REASON_LIMIT = 200;

    /**
     * Define all mood event filters as per user wants
     */
    public static class MoodEventFilter {
        public static final Function<Query, Query> ALL = UnaryOperator.identity();
        public static final Function<Query, Query> MOST_RECENT_1 = query -> query.limit(1);
        public static final Function<Query, Query> MOST_RECENT_3 = query -> query.limit(3);
        public static final Function<Query, Query> PUBLIC_ONLY = query -> query.whereEqualTo("public", true);
        // Function within compose is executed first
        public static final Function<Query, Query> PUBLIC_MOST_RECENT_1 = MOST_RECENT_1.compose(PUBLIC_ONLY);
        // Function within compose is executed first
        public static final Function<Query, Query> PUBLIC_MOST_RECENT_3 = MOST_RECENT_3.compose(PUBLIC_ONLY);
    }

    /*
     * BELOW: mood event "creation"/"modification" functions
     */

    /**
     * Creates a mood event from the specified params
     * @param ctx Android context
     * @param moodType Mood type
     * @param reason Text reason
     * @param isPublic Whether the event is public
     * @param situation Social situation; defaults to MoodEvent's default situation if is null
     * @param imgUri Image Uri (optional)
     * @param callback Callback on success/failure
     * @throws RuntimeException Exception thrown when data is invalid / encounters IO exception reading image
     */
    public static void createMoodEvent(Context ctx, MoodEvent.MoodType moodType,
                                       String reason, boolean isPublic,
                                       @Nullable MoodEvent.SocialSituation situation, @Nullable Uri imgUri,
                                       Consumer<Boolean> callback) {
        try {
            validateMoodEvent(moodType, reason);
        } catch (RuntimeException e) {
            callback.accept(false);
            return;
        }

        List<Integer> imgBytes = null;
        try {
            imgBytes = encodeImg(ctx, imgUri);
        } catch (Exception ignored ) {}

        addMoodEvent(moodType, reason, imgBytes, situation, isPublic, callback);
    }

    /**
     * Helper function for createMoodEvent.
     * @param moodType Mood type
     * @param reason String reason
     * @param imgBytes Parsed image bytes
     * @param situation Social situation; defaults to MoodEvent's default situation if is null
     * @param isPublic Whether the event is publicly visible
     * @param callback Success/failure callback
     */
    private static void addMoodEvent(MoodEvent.MoodType moodType,
                                    String reason, @Nullable List<Integer> imgBytes,
                                    @Nullable MoodEvent.SocialSituation situation, boolean isPublic,
                                    Consumer<Boolean> callback) {
        MoodEvent moodEvent = new MoodEvent(DbUtils.getUserId(), moodType, reason, imgBytes, null, isPublic);
        if (situation != null) {
            moodEvent.setSocialSituation(situation);
        }
        DbOpResultHandler<DocumentReference> handler = new DbOpResultHandler<>(
                // Success
                Void -> {
                    callback.accept(true);
                    // Lazy-init geoPoint for reactive-ness of the application
                    AtomicReference<GeoPoint> holder = new AtomicReference<>();
                    System.out.println(System.currentTimeMillis());
                    LocationManager.getGeoPoint(holder, isSuccess -> {
                        System.out.println("TIME INNER: " + System.currentTimeMillis());
                        DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, moodEvent.getDbFileId(),
                                docRef -> docRef.update("location", holder.get()), new DbOpResultHandler<>(null, null));
                    });
                },
                // Failure
                e -> {
                    e.printStackTrace(System.err);
                    callback.accept(false);
                }
        );
        DbUtils.addObjectToCollection(DbUtils.COLL_MOOD_EVENTS, moodEvent, handler);
    }

    /**
     * Updates the mood event and saves it to the database.
     * @param ctx Android context
     * @param moodEvent Mood event
     * @param moodType New mood type
     * @param reason Reason text
     * @param isPublic Whether the mood event is publicly visible
     * @param situation Social situation
     * @param imgUri Optional - image Uri
     * @param callback Success/failure callback
     * @throws RuntimeException Exception thrown on invalid data / IO exception
     */
    public static void updateMoodEvent(Context ctx, MoodEvent moodEvent, MoodEvent.MoodType moodType,
                                       String reason, boolean isPublic, MoodEvent.SocialSituation situation,
                                       @Nullable Uri imgUri, Consumer<Boolean> callback) throws RuntimeException {
        try {
            validateMoodEvent(moodType, reason);
        } catch (RuntimeException e) {
            callback.accept(false);
            return;
        }

        List<Integer> imgBytes = null;
        try {
            imgBytes = encodeImg(ctx, imgUri);
        } catch (Exception ignored) {}

        Map<String, Object> updates = new HashMap<>();
        updates.put("moodType", moodType);
        updates.put("reasonText", reason);
        // The field is named isPublic but the key in db is "public"; for safety update both.
        updates.put("isPublic", isPublic);
        updates.put("public", isPublic);
        updates.put("SocialSituation", situation);
        if (imgBytes != null) {
            updates.put("imageData", imgBytes);
        }

        updateMoodEventById(moodEvent.getDbFileId(), updates, callback);
    }

    /**
     * Adds the comment to the mood event and save the result to database.
     * @param moodEvent Mood event
     * @param comment The comment to add
     * @param callback Success/failure callback
     * @throws RuntimeException Exception thrown on invalid data / IO exception
     */
    public static void addComment(MoodEvent moodEvent, String comment, Consumer<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("comments", FieldValue.arrayUnion(comment));

        updateMoodEventById(moodEvent.getDbFileId(), updates, callback);
    }

    /**
     * Validates the mood type and reason length for a mood event.
     * @param moodType Mood type; exception thrown if mood type is null
     * @param reason Reason text; exception thrown if too long.
     * @throws RuntimeException Throws exception when the data is invalid.
     */
    public static void validateMoodEvent(MoodEvent.MoodType moodType, String reason) throws RuntimeException {
        // Mood type must be defined
        if (moodType == null) {
            throw new RuntimeException("Please select a mood");
        }
        // Check if reason text is within the limit
        if (reason.length() > REASON_LIMIT) {
            throw new RuntimeException("Reason must be " + REASON_LIMIT + " characters or less");
        }
    }

    /**
     * Encodes image from Uri to bit array.
     * @param ctx Android context
     * @param imgUri Image uri
     * @return The bit array ready to be saved
     * @throws RuntimeException Exception thrown if IOException met;
     *                          wrapped with RuntimeException to directly show error message to android UI
     */
    private static List<Integer> encodeImg(Context ctx, @Nullable Uri imgUri) throws RuntimeException {
        if (imgUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(ctx.getContentResolver(), imgUri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] imageByteArray = stream.toByteArray();

                // Check image size
                if (imageByteArray.length > 65536) { // 64 KB limit
                    throw new RuntimeException("Image size exceeds the limit of 64 KB");
                }

                // Convert byte array to List<Integer>
                List<Integer> byteList = new ArrayList<>();
                for (byte b : imageByteArray) {
                    byteList.add((int) b);
                }

                return byteList;
            } catch (IOException e) {
                throw new RuntimeException("Error reading image", e);
            }
        }
        return null;
    }

    /*
     * BELOW: "getter"/"delete" functions
     */

    /**
     * Gets all mood events satisfying the filter.
     * If user id is provided, the records are restricted to the specified user. <br>
     * On success, all mood events will be saved in the holder; otherwise, holder will not be changed.
     * @param userId User id, optional
     * @param filter Filter for the mood event
     * @param holder The holder for retrieved mood events
     * @param onComplete Callback when operation is completed
     */
    public static void getAllMoodEvents(@Nullable String userId, Function<Query, Query> filter,
                                        Collection<MoodEvent> holder, Consumer<Boolean> onComplete) {
        // Restrict to the current user & order by time, then apply further filters
        // Recall that the argument in compose is executed first.
        if (userId != null) {
            filter = filter.compose( (Query query) -> query.whereEqualTo("userId", userId) );
        }
        filter = filter.compose( (Query query) -> query.orderBy("timestamp", Query.Direction.DESCENDING) );

        DbUtils.queryObjects(DbUtils.COLL_MOOD_EVENTS,
                filter, MoodEvent.class, holder,
                new DbOpResultHandler<>(
                        success -> onComplete.accept(true),
                        error -> {
                            error.printStackTrace(System.err);
                            onComplete.accept(false);
                        }
                )
        );
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
                    e.printStackTrace(System.err);
                    callback.accept(false);
                }
        );
        DbUtils.getObjectByDocId(DbUtils.COLL_MOOD_EVENTS, moodId, MoodEvent.class, holder, handler);
    }

    /**
     * Updates a mood event with its internal document ID in the database.
     * @param fileId The id of the file to update.
     * @param updates The updates to handle.
     * @param callback The callback when the file is saved(true) or can not be saved(false).
     */
    private static void updateMoodEventById(String fileId, Map<String, Object> updates, Consumer<Boolean> callback) {
        DbOpResultHandler<Void> handler = new DbOpResultHandler<>(
                // Success
                Void -> callback.accept(true),
                // Failure
                e -> {
                    e.printStackTrace(System.err);
                    callback.accept(false);
                }
        );
        DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, fileId,
                documentReference -> documentReference.update(updates), handler);
    }

    /**
     * Deletes a mood event with its document ID in the database.
     * @param id The document id.
     * @param callback The callback when the file is deleted(true) or can not be deleted(false).
     */
    public static void deleteMoodEventById(String id, Consumer<Boolean> callback) {
        DbOpResultHandler<Void> handler = new DbOpResultHandler<>(
                // Success
                Void -> {
                    CommentManager.removeAllComments(id);
                    callback.accept(true);
                },
                // Failure
                e -> {
                    e.printStackTrace(System.err);
                    callback.accept(false);
                }
        );
        DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, id, DocumentReference::delete, handler);
    }
}
