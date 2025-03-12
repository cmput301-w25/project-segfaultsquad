package com.example.segfaultsquadapplication.impl.moodevent;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    /*
     * BELOW: mood event "creation"/"modification" functions
     */

    public static void createMoodEvent(Context ctx, MoodEvent.MoodType moodType,
                                       String reason, String trigger,
                                       MoodEvent.SocialSituation situation, Uri imgUri,
                                       Consumer<Boolean> callback) throws RuntimeException {
        validateMoodEvent(moodType, reason);

        List<Integer> imgBytes = encodeImg(ctx, imgUri);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx);
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            addMoodEvent(moodType, reason, imgBytes, null, situation, trigger, callback);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    GeoPoint geoPoint = null;
                    if (location != null) {
                        geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                    }
                    addMoodEvent(moodType, reason, imgBytes, geoPoint, situation, trigger, callback);
                })
                .addOnFailureListener(e -> {
                    // Failed to get location, save mood without it
                    addMoodEvent(moodType, reason, imgBytes, null, situation, trigger, callback);
                });
    }

    private static void addMoodEvent(MoodEvent.MoodType moodType,
                                    String reason, List<Integer> imgBytes, GeoPoint geoPoint,
                                    MoodEvent.SocialSituation situation, String trigger,
                                    Consumer<Boolean> callback) {
        MoodEvent moodEvent = new MoodEvent(DbUtils.getUserId(), moodType, reason, imgBytes, geoPoint);
        moodEvent.setSocialSituation(situation);
        moodEvent.setTrigger(trigger);
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

    public static void updateMoodEvent(Context ctx, MoodEvent moodEvent, MoodEvent.MoodType moodType,
                                       String reason, String trigger, MoodEvent.SocialSituation situation,
                                       Uri imgUri, Consumer<Boolean> callback) throws RuntimeException {
        validateMoodEvent(moodType, reason);

        List<Integer> imgBytes = encodeImg(ctx, imgUri);

        moodEvent.setMoodType(moodType);
        moodEvent.setReasonText(reason);
        moodEvent.setTrigger(trigger);
        moodEvent.setSocialSituation(situation);
        moodEvent.setImageData(imgBytes);

        updateMoodEventById(moodEvent, callback);
    }

    private static void validateMoodEvent(MoodEvent.MoodType moodType, String reason) throws RuntimeException {
        // Mood type must be defined
        if (moodType == null) {
            throw new RuntimeException("Please select a mood");
        }
        // Check if reason text is within the limit
        if (reason.length() > 20) {
            throw new RuntimeException("Reason must be 20 characters or less");
        }
    }

    private static List<Integer> encodeImg(Context ctx, Uri imgUri) throws RuntimeException {
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
