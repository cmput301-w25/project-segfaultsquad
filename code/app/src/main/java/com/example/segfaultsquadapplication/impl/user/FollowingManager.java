package com.example.segfaultsquadapplication.impl.user;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Transaction;

import java.util.function.Consumer;

/**
 * Following-specific helper functions.
 */
public class FollowingManager {
    /**
     * Sends a follow request to another user.
     * The request is added to the other user's "followRequests" field in Firebase.
     * @param otherUserId The user ID of the user to whom follow request is sent.
     * @param callback consumer boolean for custom callback
     */
    public static void sendFollowRequest(String otherUserId, Consumer<Boolean> callback) {
        String currentUserId = UserManager.getUserId();
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
    /**
     * Handles a follow request from another user.
     * If accepted, a follow relationship is created between the current user and the other user.
     * If declined, the request is removed from the current user's "followRequests" list.
     * @param otherUserId The user ID of the user who sent the follow request.
     * @param accept True to accept the follow request, false to decline.
     */
    public static void handleFollowRequest(String otherUserId, boolean accept) {
        String currentUserId = UserManager.getUserId();
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

    /**
     * Creates a follow relationship between two users.
     * Removes the follow request from the followed user's "followRequests" list.
     * Adds the follower to the followed user's "followers" list.
     * Adds the followed user to the follower's "following" list.
     * @param followerId The user ID of the follower.
     * @param followedId The user ID of the followed.
     */
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

    /**
     * Removes a follower from the current user's list of followers updated in firebase
     * The relationship is removed from both the follower's and the followed user's lists
     * @param followerId The user ID of the follower to be removed.
     */
    public static void removeFollower(String followerId) {
        String currentUserId = UserManager.getUserId();
        makeUnfollow(followerId, currentUserId);
    }

    /**
     * Removes a follow relationship between two users.
     * Removes the follower from the followed user's "followers" list.
     * Removes the followed user from the follower's "following" list.
     * @param followerId The user ID of the follower.
     * @param followedId The user ID of the followed.
     */
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
