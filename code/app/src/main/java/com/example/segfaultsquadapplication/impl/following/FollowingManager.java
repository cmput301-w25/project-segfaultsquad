package com.example.segfaultsquadapplication.impl.following;

import android.util.Log;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Transaction;

import java.util.Collection;
import java.util.function.Consumer;

public class FollowingManager {
    private static final String LOG_TITLE = "FollowingManager";

    public static void checkIfCurrentUserFollowing(User followed, Consumer<Boolean> callback) {
        checkIfFollowing(DbUtils.getUserId(), followed.getDbFileId(), callback);
    }
    public static void checkIfFollowing(String followerId, String followedId, Consumer<Boolean> callback) {
        DbUtils.queryObjects(DbUtils.COLL_FOLLOWERS,
                // Specification
                query -> query
                        .whereEqualTo("followerId", followerId)
                        .whereEqualTo("followedId", followedId),
                Following.class, null, new DbOpResultHandler<>(
                        // Success
                        result -> callback.accept( result.isEmpty() ),
                        // Failure
                        e -> {
                            Log.e("FollowingManager", "Error checking following status: ", e);
                            callback.accept(false);
                        }
                ));
    }

    public static void getAllFollowed(String followerId, Collection<Following> holder, Consumer<Boolean> callback) {
        DbUtils.queryObjects(DbUtils.COLL_FOLLOWERS,
                // Specification
                query -> query
                        .whereEqualTo("followerId", followerId),
                Following.class, holder, new DbOpResultHandler<>(
                        success -> callback.accept(true),
                        error -> {
                            Log.e(LOG_TITLE, "Error retrieving followed users: ", error);
                            callback.accept(false);
                        })
                );
    }

    public static void getAllFollowing(String followedId, Collection<Following> holder, Consumer<Boolean> callback) {
        DbUtils.queryObjects(DbUtils.COLL_FOLLOWERS,
                // Specification
                query -> query
                        .whereEqualTo("followedId", followedId),
                Following.class, holder, new DbOpResultHandler<>(
                        success -> callback.accept(true),
                        error -> {
                            Log.e(LOG_TITLE, "Error retrieving following users: ", error);
                            callback.accept(false);
                        })
                );
    }

    public static void handleFollowRequest(User user, boolean accept) {
        String currentUserId = DbUtils.getUserId();
        if (accept) {
            // Add to followers and following lists
            DocumentReference docRefCurr = DbUtils.getDocRef(DbUtils.COLL_USERS, currentUserId);
            DocumentReference docRefUser = DbUtils.getDocRef(DbUtils.COLL_USERS, user.getDbFileId());
            Transaction.Function<Void> logic;
            logic = transaction -> {
                transaction.update(docRefCurr, "followers",
                        FieldValue.arrayUnion(user.getDbFileId()));
                transaction.update(docRefUser, "following",
                        FieldValue.arrayUnion(user.getDbFileId()));
                return null;
            };
            DbUtils.operateTransaction(logic, new DbOpResultHandler<>(
                    Void -> Log.d(LOG_TITLE, "Successfully allowed follow request"),
                    e -> Log.e(LOG_TITLE, "Error allowing follow request", e)
            ));
        } else {
            // Remove the follow request
            DbUtils.operateDocumentById(DbUtils.COLL_USERS, currentUserId,
                    docRef -> docRef.update("followRequests",
                            FieldValue.arrayRemove(user.getDbFileId())),
                    new DbOpResultHandler<>(
                            Void -> Log.d(LOG_TITLE, "Successfully declined follow request"),
                            e -> Log.e(LOG_TITLE, "Error declining follow request", e)
                    ));
        }
    }
}
