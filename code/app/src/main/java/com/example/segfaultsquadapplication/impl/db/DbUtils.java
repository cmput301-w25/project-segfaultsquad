package com.example.segfaultsquadapplication.impl.db;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.Nullable;

import com.example.segfaultsquadapplication.impl.comment.CommentManager;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Do not directly call query methods in here within fragments - use the related data manager instead. <br>
 * The utility class for database connection and operations. <br>
 * DbOpResultHandler is utilized to organize the "success" / "failure" callback.
 * Here, whether to call "success" or "failure" is determined by both the operation result
 * and additional logic (e.g. additional castings / success checks), if necessary. <br>
 */
public class DbUtils {
    private static FirebaseFirestore db = null;
    public static final String
            COLL_COMMENTS = "moodComments",
            COLL_MOOD_EVENTS = "moods",
            COLL_USERS = "users";

    /**
     * UNIT TEST WIRING FUNCTION
     */
    public static void wireMockDb(FirebaseFirestore firestore) {
        db = firestore;
    }

    /**
     * If the database has not been mocked, lazy-initialize it as the default.
     */
    private static void requireDb() {
        if (db == null) db = FirebaseFirestore.getInstance();
    }

    /*
     * DOCUMENT HELPER FUNCTIONS
     */

    /**
     * Query objects from a collection satisfying specifications (condition, order, limit etc.). <br>
     * If the query succeeds, all documents will be added to the holder if it is not null;
     * If holder is null, success will be invoked without parsing documents. <br>
     * If the query fails, holder will not be modified. <br>
     * @param collection The collection to query from
     * @param specifications Transformations (filter, order, limit etc.) applied to the query
     * @param tClass The class type to convert into
     * @param holder Holder object
     * @param handler The result handler
     */
    public static <T extends IDbData> void queryObjects(String collection, Function<Query, Query> specifications,
                                                        Class<T> tClass,
                                                        @Nullable Collection<T> holder,
                                                        DbOpResultHandler<QuerySnapshot> handler) {
        requireDb();
        Query qry = specifications.apply( db.collection(collection) );
        OnCompleteListener<QuerySnapshot> listener = task -> {
            Exception exception;
            if (task.isSuccessful()) {
                readDocs:
                try {
                    // Create objects from the documents if needed
                    if (holder != null) {
                        List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                        ArrayList<T> temp = new ArrayList<>(snapshots.size() + 1);
                        for (DocumentSnapshot documentSnapshot : snapshots) {
                            T obj = documentSnapshot.toObject(tClass);
                            if (obj != null) {
                                obj.setDbFileId(documentSnapshot.getId());
                                temp.add(obj);
                            }
                            // Exit on one failure
                            else {
                                exception = new RuntimeException("Failed to convert document content");
                                break readDocs;
                            }
                        }
                        // After the loop, we return the documents.
                        holder.addAll(temp);
                    }
                    handler.onSuccess(task.getResult());
                    return;
                } catch (Exception e) {
                    exception = new RuntimeException("Error parsing document", e);
                }
            } else {
                exception = new RuntimeException("Error retrieving documents", task.getException());
            }
            exception.printStackTrace(System.err);
            handler.onFailure(exception);
        };
        qry
                .get()
                .addOnCompleteListener(listener)
                .addOnFailureListener((e) ->
                        qry
                                .get(Source.CACHE)
                                .addOnCompleteListener(listener));
    }

    /**
     * Gets a document reference. RECOMMENDED FOR TRANSACTION ONLY!
     * For other usages, consider using other helper functions.
     * @param coll The collection
     * @param docPath The document path
     * @return A reference to the document.
     */
    public static DocumentReference getDocRef(String coll, String docPath) {
        requireDb();
        return db.collection(coll).document(docPath);
    }

    /**
     * Retrieves an object implementing IDbData according to the document identifier. <br>
     * When handler's success function is called, the holder will contain the retrieved object.
     * @param collection The collection name.
     * @param docId The document id.
     * @param tClass The class to initialize the object.
     * @param holder The holder that will contain the retrieved object on success.
     * @param handler Handler to encapsulate success/failure callback.
     */
    public static <T extends IDbData> void getObjectByDocId(String collection, String docId,
                                            Class<T> tClass, AtomicReference<T> holder,
                                            DbOpResultHandler<DocumentSnapshot> handler) {
        DbOpResultHandler<DocumentSnapshot> opLogicHandler = new DbOpResultHandler<>(
                // Success
                documentSnapshot -> {
                    Exception exception;
                    if (documentSnapshot.exists()) {
                        try {
                            // Create a object from the document
                            T obj = documentSnapshot.toObject(tClass);
                            if (obj != null) {
                                obj.setDbFileId(documentSnapshot.getId());
                                holder.set(obj);
                                handler.onSuccess(documentSnapshot);
                                return;
                            } else {
                                exception = new RuntimeException("Failed to convert document content");
                            }
                        } catch (Exception e) {
                            exception = new RuntimeException("Error parsing document", e);
                        }
                    } else {
                        exception = new RuntimeException("Document does not exist");
                    }
                    handler.onFailure(exception);
                },
                // Failure
                e -> handler.onFailure(new RuntimeException("Error getting document", e))
        );
        operateDocumentById(collection, docId, docRef -> docRef.get(), new DbOpResultHandler<>(
                // Success
                opLogicHandler::onSuccess,
                // Failure - try again with local cache.
                e -> {
                    opLogicHandler.onFailure(e);
                    operateDocumentById(collection, docId, docRef -> docRef.get(Source.CACHE), opLogicHandler);
                }
        ));
    }

    /**
     * Operates the document content by its collection and document ID. <br>
     * The operation could be get, update or delete. <br>
     * However, note that when getting a document, getObjectByDocId is usually easier to work with.
     * @param collection The collection of the document.
     * @param docId The document ID.
     * @param handler The handler defining success / failure behavior.
     */
    public static <T> void operateDocumentById(String collection, String docId,
                                                Function<DocumentReference, Task<T>> operation,
                                                DbOpResultHandler<T> handler) {
        requireDb();
        try {
            operation.apply(getDocRef(collection, docId))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            handler.onSuccess(task.getResult());
                        } else {
                            task.getException().printStackTrace(System.err);
                            handler.onFailure(task.getException());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace(System.err);
            handler.onFailure(e);
        }
    }

    /**
     * Operates a transaction; the operations are either all successful or all aborted.
     * @param transaction The transaction logic.
     * @param handler The handler defining success / failure behavior.
     */
    public static <T> void operateTransaction(Transaction.Function<T> transaction, DbOpResultHandler<T> handler) {
        requireDb();
        db.runTransaction(transaction)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        handler.onSuccess(task.getResult());
                    } else {
                        task.getException().printStackTrace(System.err);
                        handler.onFailure(task.getException());
                    }
                });
    }

    /**
     * Adds the data to the collection.
     * On success, the object's id is updated to the allocated file name.
     * @param collection The collection name.
     * @param data The data to add.
     * @param handler Handler to encapsulate success/failure callback.
     */
    public static <T extends IDbData> void addObjectToCollection(String collection, T data,
                                                                 DbOpResultHandler<DocumentReference> handler) {
        requireDb();
        db.collection(collection)
                .add(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        data.setDbFileId(task.getResult().getId());
                        handler.onSuccess(task.getResult());
                    }
                    else {
                        task.getException().printStackTrace(System.err);
                        handler.onFailure(task.getException());
                    }
                });
    }

    /**
     * Pulls everything that is highly relevant to the user from database
     * I.e. The user and their mood events with comments.
     * Thease features are considered important and should work offline.
     */
    public static void prepareLocalCache() {
        AtomicReference<User> userHolder = new AtomicReference<>();
        UserManager.loadUserData(UserManager.getUserId(), userHolder, getCurrUserSucc -> {
            if (! getCurrUserSucc) return;
            // Fetch events
            prepareLocalEventsCache(UserManager.getUserId());
        });
    }

    /**
     * Helper function for prepareLocalCache
     * Pulls events & comments from the database.
     */
    private static void prepareLocalEventsCache(String uid) {
        Function<Query, Query> filter = uid.equals(UserManager.getUserId()) ?
                MoodEventManager.MoodEventFilter.ALL : MoodEventManager.MoodEventFilter.PUBLIC_ONLY;
        // Load events
        ArrayList<MoodEvent> evts = new ArrayList<>();
        MoodEventManager.getAllMoodEvents(uid, filter, evts, isEvtSucc -> {
            if (!isEvtSucc) return;
            // Load comments
            for (MoodEvent evt : evts) {
                CommentManager.getCommentsForMood(evt.getDbFileId(), new ArrayList<>(), ignored -> {});
            }
        });
    }
}
