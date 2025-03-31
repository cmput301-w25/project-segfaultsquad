package com.example.segfaultsquadapplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.content.Context;

import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * This class will help setup mock database before unit tests. <br>
 * Initialize an instance of this class before doing tests to wire things appropriately. <br>
 * WARNING: This DB impl is simple and mimics the actual firebase for mock unit tests only. <br>
 * Objects are stored as-is in the internal environment; DO NOT modify instances after adding them. <br>
 * <a href="https://www.baeldung.com/mockito-behavior">Reference</a>
 */
public class MockDb {
    // Gave up wiring emulator for mock unit tests.
    private static final boolean USE_EMULATOR = false;
    // Whether to log wiring events etc.
    private static final boolean DEBUG_LOG = false;

    // Records the current user
    FirebaseUser currMockUser = null;

    @Mock
    FirebaseFirestore mockFirestore = Mockito.mock(FirebaseFirestore.class);
    // Registers collection reference mocks
    HashMap<String, CollectionReference> collRefs = new HashMap<>();
    // Registers the "next file index" for each collection
    HashMap<String, AtomicInteger> collNextIdx = new HashMap<>();
    // Document reference mocks
    HashMap<String, HashMap<String, DocumentReference>> docRefs = new HashMap<>();
    // Document content holder
    HashMap<String, HashMap<String, Object>> docContents = new HashMap<>();
    // Query results holder; for simplicity we store the documents here.
    // It is an inefficient use of memory, but mock tests are meant to be simple.
    HashMap<Query, List<DocumentReference>> queryResults = new HashMap<>();

    /**
     * Constructor; creates mock db with relevant features as well.
     * @param colls The list of collections to mock for.
     */
    public MockDb(String... colls) {
        // Can not really use emulator in a reliable way. Mocking context is not reasonable either.
        if (USE_EMULATOR) {
            String androidLocalhost = "10.0.2.2";
            int portNumber = 8080;

            Context mockCtx = Mockito.mock(Context.class);
//            Mockito.when(mockCtx.()).thenReturn(coll);

            FirebaseApp.initializeApp(mockCtx);

            FirebaseFirestore firestore = FirebaseFirestore.getInstance("db");
            firestore.useEmulator(androidLocalhost, portNumber);

            DbUtils.wireMockDb(firestore);
            return;
        }

        // Wire up google auth
        {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            doAnswer(invoc -> {
                return currMockUser;
            }).when( mockAuth ).getCurrentUser();

            Task<AuthResult> mockTask = wrapTask(null);
            doAnswer(invocLogin -> {
                String mail = invocLogin.getArgument(0);
                currMockUser = mock(FirebaseUser.class);
                doAnswer(invoc -> mail).when( currMockUser ).getEmail();
                doAnswer(invoc -> "id" + mail).when( currMockUser ).getUid();
                return mockTask;
            }).when(mockAuth).signInWithEmailAndPassword(anyString(), anyString());
            UserManager.wireMockAuth(mockAuth);
        }
        // Wire up firestore
        DbUtils.wireMockDb(mockFirestore);
        wireTransaction(mockFirestore);
        // Add mock collections
        for (String coll : colls) {
            if (DEBUG_LOG) System.out.println("Coll " + coll);
            // Mock collection
            CollectionReference mockColl = Mockito.mock(CollectionReference.class);
            collRefs.put(coll, mockColl);
            Mockito.when(mockColl.getId()).thenReturn(coll);
            // Wire the mock collection
            Mockito.when(mockFirestore.collection(coll)).thenReturn(mockColl);
            // Mock document index
            collNextIdx.put(coll, new AtomicInteger(1));
            // Mock document reference
            docRefs.put(coll, new HashMap<>());
            // Actual data holder
            docContents.put(coll, new HashMap<>());

            wireCollectionSingleDocOp(mockColl);
            wireQueryOp(mockColl);
        }
    }

    /**
     * Helper method that helps wait for a DB operation in unit test to finish, then continue with next logics. <br>
     * method will behave as a proxy to inject shouldProceed logic as additional logic in the desired callback.
     * @param method The proxy for extra behavior to mark for continue
     */
    public static void await(Function<Runnable, Runnable> method) throws InterruptedException {
        AtomicReference<Boolean> shouldProceed = new AtomicReference<>(false);

        method.apply( () -> shouldProceed.set(true) ).run();

        while (!shouldProceed.get()) {
            Thread.sleep(100);
        }
    }

    /*
     * BELOW: WIRING FUNCTIONS
     */

    /**
     * Mocks the transaction method for the entire database.
     * ONLY WIRED UP UPDATE METHODS as they are the ones being used in the project.
     * @param mockDb The database to wire.
     */
    private void wireTransaction(FirebaseFirestore mockDb) {
        doAnswer(invocation -> {
            // Mock transaction logic
            Transaction mockTransaction = Mockito.mock(Transaction.class);
            doAnswer(updateInfo -> {
                // Mock transaction logic
                DocumentReference docRef = updateInfo.getArgument(0);
                String field = updateInfo.getArgument(1);
                Object value = updateInfo.getArgument(2);
                docRef.update(field, value);
                // Construct the task to return
                return mockTransaction;
            }).when(mockTransaction).update(any(), anyString(), any());

            Transaction.Function<?> function = invocation.getArgument(0);
            Object result = function.apply(mockTransaction);
            // Construct the task to return
            return wrapTask(result);
        }).when(mockDb).runTransaction(any());
    }

    /**
     * Mocks the single document operation for a collection
     * That is, add and get documents
     * @param mockColl The mock collection
     */
    private void wireCollectionSingleDocOp(CollectionReference mockColl) {
        if (DEBUG_LOG) System.out.println("wireNewDocOp");
        // Add new element to collection
        doAnswer(invocation -> {
            String nextId = collNextIdx.get(mockColl.getId()).getAndIncrement() + "";
            Object param = invocation.getArgument(0);
            // Save the document to storage
            docContents.get(mockColl.getId()).put(nextId, param);
            // Init doc ref
            DocumentReference docRef = initMockDocRef(mockColl, nextId);
            // Construct the task to return
            return wrapTask(docRef);
        }).when(mockColl).add(any());
        // Get document from collection
        doAnswer(invocation -> {
            String param = invocation.getArgument(0);
            // Return the document reference
            return docRefs.get(mockColl.getId()).getOrDefault(param,
                    initMockDocRef(mockColl, param));
        }).when(mockColl).document(anyString());
    }

    /**
     * Wraps the result into a task.
     * @param result The result to wrap up with
     * @return The wrapped tasks
     */
    private <T> Task<T> wrapTask(T result) {
        Task<T> task = Mockito.mock(Task.class);
        Mockito.when(task.isSuccessful()).thenReturn(true);
        Mockito.when(task.getResult()).thenReturn(result);
        // Support the on complete listener
        doAnswer(invoc -> {
            OnCompleteListener<T> resultListener = invoc.getArgument(0);
            resultListener.onComplete(task);
            return task;
        }).when(task).addOnCompleteListener(any());
        return task;
    }

    /**
     * Helper function for wireNewDocOp - creates a mock document reference. <br>
     * NOTE: The document content is saved in wireNewDocOp - this only creates the reference.
     * @param mockColl The collection to mock
     * @param docId Document id
     * @return The new document reference
     */
    private DocumentReference initMockDocRef(CollectionReference mockColl, String docId) {
        if (DEBUG_LOG) System.out.println("initMockDocRef");
        DocumentReference docRef = Mockito.mock(DocumentReference.class);
        docRefs.get(mockColl.getId()).put(docId, docRef);
        // In our mock db, path and id are basically the same.
        Mockito.when(docRef.getPath()).thenReturn(docId);
        Mockito.when(docRef.getId()).thenReturn(docId);
        // DocRef operations - get, set, update, delete
        doAnswer(invoc -> {
            return wrapTask(getDocSnapshot(mockColl, docId));
        }).when(docRef).get();

        doAnswer(invoc -> {
            docContents.get(mockColl.getId()).put(docId, invoc.getArgument(0));
            return wrapTask(null);
        }).when(docRef).set(any());

        // Normal field updates
        doAnswer(invoc -> {
            Map<String, Object> updateMap = invoc.getArgument(0);
            Object dataObj = getObjFromDocRef(mockColl, docId);
            for (String key : updateMap.keySet()) {
                try {
                    Field fieldDataArr = dataObj.getClass().getDeclaredField(key);
                    fieldDataArr.setAccessible(true);
                    fieldDataArr.set(dataObj, updateMap.get(key));
                } catch (Exception ignored) {}
            }
            return wrapTask(null);
        }).when(docRef).update(any());
        // Array union / remove updates
        doAnswer(invoc -> {
            String fieldVal = invoc.getArgument(0);
            Object update = invoc.getArgument(1);
            Class<?> arrayRemoveCls = Class.forName("com.google.firebase.firestore.FieldValue$ArrayRemoveFieldValue");
            Class<?> arrayUnionCls = Class.forName("com.google.firebase.firestore.FieldValue$ArrayUnionFieldValue");
            // If the update is to remove from array
            if (arrayRemoveCls.isInstance(update)) {
                Field fieldToRemove = arrayRemoveCls.getDeclaredField("elements");
                fieldToRemove.setAccessible(true);
                Collection<Object> removeElements = (List<Object>) fieldToRemove.get(update);
                // Remove the elements from the data
                Object dataObj = getObjFromDocRef(mockColl, docId);
                Field fieldDataArr = dataObj.getClass().getDeclaredField(fieldVal);
                fieldDataArr.setAccessible(true);
                ((Collection<Object>) fieldDataArr.get(dataObj)).removeAll(removeElements);
            }
            // If the update is to insert into array
            else if (arrayUnionCls.isInstance(update)) {
                Field fieldToAdd = arrayUnionCls.getDeclaredField("elements");
                fieldToAdd.setAccessible(true);
                Collection<Object> addElements = (List<Object>) fieldToAdd.get(update);
                // Remove the elements from the data
                Object dataObj = getObjFromDocRef(mockColl, docId);
                Field fieldDataArr = dataObj.getClass().getDeclaredField(fieldVal);
                fieldDataArr.setAccessible(true);
                ((Collection<Object>) fieldDataArr.get(dataObj)).addAll(addElements);
            }
            // Other updates is not relevant in this project.
            else {
                throw new RuntimeException("Unsupported update type");
            }
            return wrapTask(null);
        }).when(docRef).update(anyString(), any());

        doAnswer(invoc -> {
            docRefs.get(mockColl.getId()).remove(docId);
            docContents.get(mockColl.getId()).remove(docId);
            return wrapTask(null);
        }).when(docRef).delete();
        return docRef;
    }

    /**
     * Gets the mocked document snapshot for the document in the collection.
     * @param mockColl The collection
     * @param docId Document id
     * @return Mock document snapshot
     */
    private DocumentSnapshot getDocSnapshot(CollectionReference mockColl, String docId) {
        DocumentSnapshot snapshot = Mockito.mock(DocumentSnapshot.class);
        doAnswer(getIdInvoc -> docId).when(snapshot).getId();
        doAnswer(getIdInvoc -> docContents.get(mockColl.getId()).containsKey(docId))
                .when(snapshot).exists();
        doAnswer(toObjectInvoc -> {
            Class<?> param = toObjectInvoc.getArgument(0);
            return param.cast( getObjFromDocRef(mockColl, docId) );
        }).when(snapshot).toObject(any());
        return snapshot;
    }

    /**
     * Mocks the query document operations for a collection
     * @param mockColl The mock collection
     */
    private void wireQueryOp(CollectionReference mockColl) {
        if (DEBUG_LOG) System.out.println("wireQueryOp");
        // NOTE: init mockQry in separate "when"'s, it should NOT share the same instance!
        // The same instance WILL BE MODIFIED IN PLACE by other mechanisms.

        // Gets all documents within the reference
        doAnswer(invocation -> {
            Query mockQry = formulateQuery(mockColl);
            return mockQry.get();
        }).when(mockColl).get();
        // Gets all documents within the reference
        doAnswer(invocation -> {
            Query mockQry = formulateQuery(mockColl);
            return mockQry.whereEqualTo((String) invocation.getArgument(0), invocation.getArgument(1));
        }).when(mockColl).whereEqualTo(anyString(), any());
        // Orders results
        doAnswer(invocation -> {
            Query mockQry = formulateQuery(mockColl);
            return mockQry.orderBy((String) invocation.getArgument(0), invocation.getArgument(1));
        }).when(mockColl).orderBy(anyString(), any());
    }

    /**
     * Forms a query object from the collection. <br>
     * Further operations modify the query content IN-PLACE
     * As the results are held internally via hashmap.
     * @param mockColl The mock collection.
     * @return The query object that returns the entire collection.
     */
    private Query formulateQuery(CollectionReference mockColl) {
        Query mockQry = Mockito.mock(Query.class);
        // Put a new array list into its filters
        queryResults.put(mockQry, docRefs.get(mockColl.getId()).values().stream().toList() );

        // Mock the query's get mechanism
        doAnswer(invocation -> {
            QuerySnapshot queryResult = Mockito.mock(QuerySnapshot.class);
            // The query result's logics
            doAnswer(invoc -> {
                List<DocumentSnapshot> result = new ArrayList<>();
                for (DocumentReference ref : queryResults.getOrDefault(mockQry, new ArrayList<>()) ) {
                    // Formulate the corresponding snapshot item
                    result.add(getDocSnapshot(mockColl, ref.getId()));
                }
                return result;
            }).when(queryResult).getDocuments();

            // Also wire isEmpty, it is used in following etc.
            doAnswer(invoc -> queryResult.getDocuments().isEmpty()).when(queryResult).isEmpty();

            // Construct the query result to return
            Task<QuerySnapshot> task = Mockito.mock(Task.class);
            Mockito.when(task.isSuccessful()).thenReturn(true);
            Mockito.when(task.getResult()).thenReturn(queryResult);
            // Support the on complete listener
            doAnswer(invoc -> {
                OnCompleteListener<QuerySnapshot> docRefListener = invoc.getArgument(0);
                docRefListener.onComplete(task);
                return task;
            }).when(task).addOnCompleteListener(any());
            // Return the constructed task
            return task;
        }).when(mockQry).get();
        // Mocks the query's filter mechanisms
        doAnswer(invocation -> {
            List<DocumentReference> oldRefs = queryResults.get(mockQry);
            if (! oldRefs.isEmpty()) {
                // Apply filtering
                String fieldName = invocation.getArgument(0);
                Object val = invocation.getArgument(1);
                ArrayList<DocumentReference> newRefs = new ArrayList<>();
                // Assume: all objects in the collection are of the same class
                Object firstElem = getObjFromDocRef(mockColl, oldRefs.get(0));
                Field f = firstElem.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                for (DocumentReference ref : oldRefs) {
                    // Validate the object with filter
                    Object currElem = getObjFromDocRef(mockColl, ref);
                    if (f.get(currElem).equals(val)) {
                        newRefs.add(ref);
                    }
                }
                // Update items
                queryResults.put(mockQry, newRefs);
                if (DEBUG_LOG) System.out.println("QUERY REFS: " + newRefs + " with size " + newRefs.size());
            }
            // Return the qry itself
            return mockQry;
        }).when(mockQry).whereEqualTo(anyString(), any());
        // Mocks the query's order-by mechanisms
        doAnswer(invocation -> {
            ArrayList<DocumentReference> docRefs = new ArrayList<>();
            docRefs.addAll( queryResults.get(mockQry) );
            if (! docRefs.isEmpty()) {
                // Apply filtering
                String fieldName = invocation.getArgument(0);
                Query.Direction dir = invocation.getArgument(1);
                // Assume: all objects in the collection are of the same class
                Field f = getObjFromDocRef(mockColl, docRefs.get(0))
                        .getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                // Sort in place
                quickSort(docRefs,
                        (ref) -> {
                            try {
                                return (Comparable) f.get( getObjFromDocRef(mockColl, ref) );
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        dir, 0, docRefs.size());
                // Update
                queryResults.put(mockQry, docRefs);
            }
            // Return the qry itself
            return mockQry;
        }).when(mockQry).orderBy(anyString(), any());
        // Mocks the query's limit mechanisms
        doAnswer(invocation -> {
            List<DocumentReference> docRefs = queryResults.get(mockQry);
            List<DocumentReference> newDocRefs = new ArrayList<>();
            long limit = invocation.getArgument(0);
            for (int i = 0; i < Math.min(docRefs.size(), limit); i ++) {
                newDocRefs.add(docRefs.get(i));
            }
            queryResults.put(mockQry, newDocRefs);
            // Return the qry itself
            return mockQry;
        }).when(mockQry).limit(anyLong());

        return mockQry;
    }

    /**
     * Quick sort used in order-by of formulateQuery. Sorts the document references in-place.
     * @param data The list of raw data.
     * @param cast The adapter to cast data to a comparable object.
     * @param dir The direction - ascending or descending.
     * @param start The start index, inclusive.
     * @param postEnd The end index, exclusive.
     */
    private <T, D extends Comparable<D>> void quickSort(List<T> data, Function<T, D> cast,
                                                        Query.Direction dir, int start, int postEnd) {
        // Base case
        if (start + 1 >=  postEnd) return;

        D pvt = cast.apply( data.get(postEnd - 1) );
        // Early init for temp data holder for swapping later on
        T tmp;
        int pvtIdx = start;
        // Ignore looping the pivot element
        for (int i = start; i < postEnd - 1; i ++) {
            D elem = cast.apply( data.get(i) );
            // Ascending: go before pvt if elem < pvt, elem - pvt < 0
            boolean goBeforePvt = ((dir == Query.Direction.ASCENDING) ?
                    elem.compareTo(pvt) : pvt.compareTo(elem)) < 0;
            // Swap
            if (goBeforePvt) {
                tmp = data.get(pvtIdx);
                data.set(pvtIdx, data.get(i));
                data.set(i, tmp);
                pvtIdx ++;
            }
        }
        // Move pivot element to the appropriate position
        tmp = data.get(pvtIdx);
        data.set(pvtIdx, data.get(postEnd-1));
        data.set(postEnd-1, tmp);
        // Recursion calls
        quickSort(data, cast, dir, start, pvtIdx);
        quickSort(data, cast, dir, pvtIdx+1, postEnd);
    }

    /**
     * Gets an object with its corresponding collection and document reference.
     * @param collRef The collection reference.
     * @param docRef The document reference.
     * @return The object corresponding to the document reference in the collection reference.
     */
    private Object getObjFromDocRef(CollectionReference collRef, DocumentReference docRef) {
        return getObjFromDocRef(collRef, docRef.getId());
    }

    /**
     * Gets an object with its corresponding collection and document reference.
     * @param collRef The collection reference.
     * @param docId The document id.
     * @return The object corresponding to the document reference in the collection reference.
     */
    private Object getObjFromDocRef(CollectionReference collRef, String docId) {
        return docContents.get(collRef.getId()).getOrDefault(docId, null);
    }
}
