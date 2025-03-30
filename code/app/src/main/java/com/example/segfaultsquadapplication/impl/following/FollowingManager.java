package com.example.segfaultsquadapplication.impl.following;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Transaction;

import java.util.function.Consumer;

/**
 * Following-specific helper functions.
 */
public class FollowingManager {
    public static void sendFollowRequest(String otherUserId, Consumer<Boolean> callback) {
        String currentUserId = DbUtils.getUserId();
        DbUtils.operateDocumentById(DbUtils.COLL_USERS, otherUserId,
                docRef -> docRef.update("followRequests",
                        FieldValue.arrayUnion(currentUserId)),
                new DbOpResultHandler<>(
                        Void -> {
                            System.out.println("Successfully added follow request");
                            callback.accept(true);
                        },
                        e -> {
                            e.printStackTrace(System.err);
                            callback.accept(false);
                        }
                ));
    }

    public static void handleFollowRequest(String otherUserId, boolean accept) {
        String currentUserId = DbUtils.getUserId();
        if (accept) {
            makeFollow(otherUserId, currentUserId);
        } else {
            // Remove the follow request
            DbUtils.operateDocumentById(DbUtils.COLL_USERS, currentUserId,
                    docRef -> docRef.update("followRequests",
                            FieldValue.arrayRemove(otherUserId)),
                    new DbOpResultHandler<>(
                            Void -> System.out.println("Successfully declined follow request"),
                            e -> e.printStackTrace(System.err)
                    ));
        }
    }

    public static void makeFollow(String followerId, String followedId) {
        DocumentReference docRefFollower = DbUtils.getDocRef(DbUtils.COLL_USERS, followerId);
        DocumentReference docRefFollowed = DbUtils.getDocRef(DbUtils.COLL_USERS, followedId);
        Transaction.Function<Void> logic = transaction -> {
            transaction.update(docRefFollowed, "followRequests",
                    FieldValue.arrayRemove(followerId));
            transaction.update(docRefFollowed, "followers",
                    FieldValue.arrayUnion(followerId));
            transaction.update(docRefFollower, "following",
                    FieldValue.arrayUnion(followedId));
            return null;
        };
        DbUtils.operateTransaction(logic, new DbOpResultHandler<>(
                Void -> System.out.println("Successfully handled follow request"),
                e -> e.printStackTrace(System.err)
        ));
    }

    public static void removeFollower(String followerId) {
        String currentUserId = UserManager.getUserId();
        makeUnfollow(followerId, currentUserId);
    }

    public static void makeUnfollow(String followerId, String followedId) {
        // Add to followers and following lists
        DocumentReference docRefFollowed = DbUtils.getDocRef(DbUtils.COLL_USERS, followedId);
        DocumentReference docRefFollower = DbUtils.getDocRef(DbUtils.COLL_USERS, followerId);
        Transaction.Function<Void> logic = transaction -> {
            transaction.update(docRefFollowed, "followers",
                    FieldValue.arrayRemove(followerId));
            transaction.update(docRefFollower, "following",
                    FieldValue.arrayRemove(followedId));
            return null;
        };
        DbUtils.operateTransaction(logic, new DbOpResultHandler<>(
                Void -> System.out.println("Successfully removed follower"),
                e -> e.printStackTrace(System.err)
        ));
    }
}
