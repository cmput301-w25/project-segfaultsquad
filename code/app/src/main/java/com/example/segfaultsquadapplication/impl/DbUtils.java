package com.example.segfaultsquadapplication.impl;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The utility class for database connection & operations. </br>
 * DbOpResultHandler is utilized to organize the "success" / "failure" callback.
 * Here, whether to call "success" or "failure" is determined by both the operation result
 * and additional logic (e.g. additional success checks), if necessary. </br>
 * </br>
 * For failures, it is possible to identify the source of failure: </br>
 * Failure caused by the task itself would throw
 * an exception with the cause being the original error. </br>
 * Failure caused by additional operation logic would throw
 * an exception with an unspecified cause, i.e. itself.
 */
public class DbUtils {
    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Gets the current user; returns null if not logged in.
     * @return The Firebase User. MIGHT BE NULL if not logged in.
     */
    @Nullable
    public static FirebaseUser getUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Sign in to a user account in the firebase authentication system.
     * @param email The user's email
     * @param pwd The user's password
     * @param handler The handler for success/failure
     */
    public static void signIn(String email, String pwd, DbOpResultHandler<AuthResult> handler) {
        mAuth.signInWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Check if user document exists in Firestore
                        FirebaseUser user = getUser();
                        if (user != null) {
                            checkAndCreateUserDocument(user, task, handler);
                        } else {
                            handler.onFailure(new Exception("Error during login - please try again"));
                        }
                    } else {
                        handler.onFailure(new Exception(
                                "Authentication failed", task.getException() ));
                    }
                });
    }

    /**
     * Method to check for existance otherwise create a user in the db
     * @param firebaseUser The user we are checking for
     * @param task The task for which we were checking this document for
     * @param handler The handler for the result
     */
    private static void checkAndCreateUserDocument(FirebaseUser firebaseUser, Task<AuthResult> task, DbOpResultHandler<AuthResult> handler) {
        db.collection("users")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // Create new user document
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("username", firebaseUser.getEmail().split("@")[0]);
                        userData.put("followers", new ArrayList<String>()); // Initialize as empty
                        userData.put("following", new ArrayList<String>()); // Initialize as empty

                        db.collection("users")
                                .document(firebaseUser.getUid())
                                .set(userData)
                                .addOnSuccessListener( aVoid-> handler.onSuccess(task.getResult()) )
                                .addOnFailureListener(e -> {
                                    handler.onFailure( new RuntimeException("Error creating user profile") );
                                });
                    } else {
                        // user document was already there
                        handler.onSuccess(task.getResult());
                    }
                })
                .addOnFailureListener(e -> {
                    handler.onFailure( new RuntimeException("Error checking user profile") );
                });
    }

    /**
     * Organizes the logic used to handle database operation results.
     */
    public static class DbOpResultHandler<TResult> {
        private final OnSuccessListener<TResult> successListener;
        private final OnFailureListener failListener;

        /**
         * Creates an instance of result handler. </br>
         * To find out more detail about how success / failure would be triggered, see documentation for DbUtils.
         * @param successListener The success callback, put null if want to be omitted
         * @param failListener The failure callback, put null if want to be omitted
         */
        public DbOpResultHandler(@Nullable OnSuccessListener<TResult> successListener,
                                 @Nullable OnFailureListener failListener) {
            this.successListener = successListener;
            this.failListener = failListener;
        }

        void onSuccess(TResult result) {
            if (successListener != null) {
                successListener.onSuccess(result);
            }
        }

        void onFailure(Exception exception) {
            if (failListener != null) {
                failListener.onFailure(exception);
            }
        }
    }
}
