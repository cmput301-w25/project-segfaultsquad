package com.example.segfaultsquadapplication.impl.user;

import androidx.annotation.Nullable;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This file contains authentication and functionality for loading an user's data from DB.
 * For following-related functions, see FollowingManager.
 */
public class UserManager {
    private static FirebaseAuth mAuth = null;
    private static final boolean SAVE_USER_GMAIL = false;

    /**
     * UNIT TEST WIRING FUNCTION
     */
    public static void wireMockAuth(FirebaseAuth auth) {
        mAuth = auth;
    }

    /**
     * If the auth has not been mocked, lazy-initialize it as the default.
     */
    private static void requireAuth() {
        if (mAuth == null) mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Gets the current user; returns null if not logged in.
     * @return The Firebase User. MIGHT BE NULL if not logged in.
     */
    @Nullable
    public static FirebaseUser getCurrUser() {
        requireAuth();
        return mAuth.getCurrentUser();
    }

    /**
     * Gets the user's username.
     * @return The user name for the user.
     */
    @Nullable
    public static String getUsername(@Nullable FirebaseUser user) {
        return user == null ? null : user.getEmail().split("@")[0];
    }

    /**
     * Gets the CURRENT user's id.
     * @return The user id for the current user.
     */
    @Nullable
    public static String getUserId() {
        return getUserId(getCurrUser());
    }

    /**
     * Gets the SPECIFIED user's id. Use this instead of the raw getUid for consistency & null handling.
     * @param user The user to retrieve the ID from.
     * @return The user id for the specified user.
     */
    @Nullable
    public static String getUserId(@Nullable FirebaseUser user) {
        return user == null ? null : user.getUid();
    }

    /**
     * Sign in to a user account in the firebase authentication system.
     * @param email The user's email
     * @param pwd The user's password
     * @param callback The handler for success (true, /) or failure (false, failure reason)
     */
    public static void login(String email, String pwd, BiConsumer<Boolean, String> callback) {
        requireAuth();
        mAuth.signInWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Check if user document exists in Firestore
                        FirebaseUser user = getCurrUser();
                        if (user != null) {
                            System.out.println("User ID: " + user.getUid());
                            checkUserDocument(user, callback);
                        } else {
                            callback.accept(false, "Error during login - please try again");
                        }
                    } else {
                        task.getException().printStackTrace(System.err);
                        callback.accept(false, "Authentication failed");
                    }
                });
    }

    /**
     * Method to check for existance otherwise create a user in the db
     * @param firebaseUser The user we are checking for
     * @param callback The handler for the result
     */
    private static void checkUserDocument(FirebaseUser firebaseUser, BiConsumer<Boolean, String> callback) {
        DbOpResultHandler<DocumentSnapshot> opHandler = new DbOpResultHandler<>(
                // Success
                documentSnapshot -> {
                    System.out.println("SUCCESS, EXIST? " + documentSnapshot.exists());
                    if (!documentSnapshot.exists()) {
                        createUserDocument(firebaseUser, callback);
                    } else {
                        // user document was already there
                        callback.accept(true, null);
                    }
                },
                // Failure
                e -> {
                    e.printStackTrace(System.err);
                    callback.accept(false, "Error checking user profile");
                }
        );
        DbUtils.operateDocumentById(DbUtils.COLL_USERS, getUserId(firebaseUser),
                DocumentReference::get, opHandler);
    }

    /**
     * Method to create a user in the db
     * @param firebaseUser The user we are checking for
     * @param callback The handler for the result
     */
    private static void createUserDocument(FirebaseUser firebaseUser, BiConsumer<Boolean, String> callback) {
        String uid = getUserId(firebaseUser);
        System.out.println("NEW UID" + uid);
        User newUser = new User(uid, getUsername(firebaseUser),
                SAVE_USER_GMAIL ? firebaseUser.getEmail() : null);

        DbOpResultHandler<Void> opHandler = new DbOpResultHandler<>(
                // Success
                aVoid -> callback.accept(true, null),
                // Failure
                e -> {
                    e.printStackTrace(System.err);
                    callback.accept(false, "Error creating user profile");
                }
        );
        DbUtils.operateDocumentById(DbUtils.COLL_USERS, uid,
                docRef -> docRef.set(newUser), opHandler);
    }

//    // This function is the previous impl. It is more hacky than it should've been.
//    // If things work well, this can be safely deleted a bit later.
//    private static void createUserDocument(FirebaseUser firebaseUser, Task<AuthResult> task, TaskResultHandler<AuthResult> handler) {
//        Map<String, Object> userData = new HashMap<>();
//        userData.put("username", getUsername(firebaseUser));
//        userData.put("followers", new ArrayList<String>()); // Initialize as empty
//        userData.put("following", new ArrayList<String>()); // Initialize as empty
//
//        TaskResultHandler<Void> opHandler = new TaskResultHandler<>(
//                // Success
//                aVoid -> handler.onSuccess(task.getResult()),
//                // Failure
//                e -> handler.onFailure(
//                        new RuntimeException("Error creating user profile") )
//        );
//        DbUtils.operateDocumentById(DbUtils.COLL_USERS, firebaseUser.getUid(),
//                dr -> dr.set(userData), opHandler);
//    }

    /**
     * Gets the user information from the database.
     * @param userId The user id.
     * @param holder The Atomic Reference that is used to hold the result on success.
     * @param callback Callback function when the function concludes with success(true) or failure(false).
     */
    public static void loadUserData(String userId, AtomicReference<User> holder, Consumer<Boolean> callback) {
        DbUtils.getObjectByDocId(DbUtils.COLL_USERS, userId, User.class, holder,
                new DbOpResultHandler<>(
                        result -> callback.accept(true),
                        e -> {
                            e.printStackTrace(System.err);
                            callback.accept(false);
                        }));
    }
}
