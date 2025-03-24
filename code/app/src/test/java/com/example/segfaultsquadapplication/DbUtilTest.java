package com.example.segfaultsquadapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import androidx.test.filters.LargeTest;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.user.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs tests for DbUtil content.
 *
 * USE SILENT CLASS to prevent lenient errors
 * All of MockDb's features may not be needed for all test cases in all test files. Use silent version.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@LargeTest
public class DbUtilTest {
    private static final MoodEvent[] INIT_EVENTS = {
            new MoodEvent("uid1", MoodEvent.MoodType.SADNESS, "reason", null, null, true),
            new MoodEvent("uid1", MoodEvent.MoodType.HAPPINESS, "be happy", null, null, true),
            new MoodEvent("uid1", MoodEvent.MoodType.SADNESS, "last", null, null, true),
            new MoodEvent("uid2", MoodEvent.MoodType.SADNESS, "reason", null, null, true),
    };

    @Before
    public void setup() throws InterruptedException {
        new MockDb(DbUtils.COLL_MOOD_EVENTS, DbUtils.COLL_USERS);

        System.out.println("\n\n Test setup - adding event objects to collections");

        for (MoodEvent evt : INIT_EVENTS) {
            MockDb.await( (finishCallback) ->
                    () -> DbUtils.addObjectToCollection(DbUtils.COLL_MOOD_EVENTS, evt,
                            new DbOpResultHandler<>(
                                    ignored -> finishCallback.run(),
                                    Assert::assertNull
                            )));

            System.out.println("Saved db file id: " + evt.getDbFileId());
            assertNotNull(evt.getDbFileId());
        }

        System.out.println("\n\n Test setup - adding user objects to collections");
        User user1 = new User("uid1", "user1", "uid1@gmail.com");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.addObjectToCollection(DbUtils.COLL_USERS, user1,
                        new DbOpResultHandler<>(
                                ignored -> finishCallback.run(),
                                Assert::assertNull
                        )));
    }

    /*
     * TESTS BELOW
     */

    /**
     * Tests queryObjects's functionality.
     */
    @Test
    public void queryDocumentsTest() throws InterruptedException {
        System.out.println("=> queryDocumentsTest");
        System.out.println("  - check uid1");
        ArrayList<MoodEvent> holder1 = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> DbUtils.queryObjects(DbUtils.COLL_MOOD_EVENTS,
                        query -> query
                                .whereEqualTo("userId", "uid1")
                                .orderBy("timestamp", Query.Direction.DESCENDING),
                        MoodEvent.class, holder1,
                        new DbOpResultHandler<>(
                                ignored -> finishCallback.run(),
                                ignored -> Assert.assertNull("Failed to get query result on UID 1", true)
                        )));
        assertEquals(holder1.size(), 3);
        for (int i = 0; i < holder1.size(); i ++) {
            System.out.println(holder1.get(i).getTimestamp());
        }
        for (int i = 0; i < holder1.size() - 1; i ++) {
            assertTrue(holder1.get(i).getTimestamp().compareTo(holder1.get(i+1).getTimestamp()) >= 0);
        }

        System.out.println("  - check do not exist");
        ArrayList<MoodEvent> holder2 = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> DbUtils.queryObjects(DbUtils.COLL_MOOD_EVENTS,
                        query -> query
                                .whereEqualTo("userId", "that's right, the target uid of the query do not exist"),
                        MoodEvent.class, holder2,
                        new DbOpResultHandler<>(
                                ignored -> finishCallback.run(),
                                ignored -> finishCallback.run()
                        )));
        assertTrue(holder2.isEmpty());
    }

    /**
     * Tests getDocRef's functionality.
     */
    @Test
    public void getDocRefTest() throws InterruptedException {
        System.out.println("=> getDocRefTest");
        System.out.println("  - existing document");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.getDocRef(DbUtils.COLL_MOOD_EVENTS, "1")
                        .get()
                        .addOnCompleteListener(
                                (result) -> {
                                    assertTrue(result.isSuccessful());
                                    MoodEvent evt = result.getResult().toObject(MoodEvent.class);
                                    assertEquals(evt.getMoodType(), MoodEvent.MoodType.SADNESS);
                                    assertEquals(evt.getReasonText(), "reason");
                                    finishCallback.run();
                                }));

        System.out.println("  - non-existing document");
        assertNull( DbUtils.getDocRef(DbUtils.COLL_MOOD_EVENTS, "-123") );
    }

    /**
     * Tests getObjectByDocId's functionality.
     */
    @Test
    public void getObjectByDocIdTest() throws InterruptedException {
        System.out.println("=> getObjectByDocIdTest");
        System.out.println("  - existing");
        AtomicReference<MoodEvent> holder1 = new AtomicReference<>();
        MockDb.await( (finishCallback) ->
                () -> DbUtils.getObjectByDocId(DbUtils.COLL_MOOD_EVENTS,
                        "2", MoodEvent.class, holder1,
                        new DbOpResultHandler<>(
                                ignored -> finishCallback.run(),
                                e -> Assert.assertNull("Failed to retrieve an existing document", e)
                        )));
        assertEquals(holder1.get().getReasonText(), "be happy");
        assertEquals(holder1.get().getMoodType(), MoodEvent.MoodType.HAPPINESS);
        assertEquals(holder1.get().getUserId(), "uid1");

        System.out.println("  - non-existing");
        AtomicReference<MoodEvent> holder2 = new AtomicReference<>();
        MockDb.await( (finishCallback) ->
                () -> DbUtils.getObjectByDocId(DbUtils.COLL_MOOD_EVENTS,
                        "-114514", MoodEvent.class, holder2,
                        new DbOpResultHandler<>(
                                ignored -> Assert.assertNull("Success on retrieving non-existing document", true),
                                ignored -> finishCallback.run()
                        )));
    }

    /**
     * Tests operateDocumentById's functionality;
     * Tested use cases (the ones used in this project):
     * Get, set, and delete
     * For update, see the test below!
     */
    @Test
    public void operateDocumentByIdTest() throws InterruptedException {
        System.out.println("=> operateDocumentByIdTest");
        System.out.println("  - Set event #2");
        MoodEvent moodEvt2 = new MoodEvent("uid1", MoodEvent.MoodType.ANGER, "AWWW MAN", null, null, true);
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, "2",
                        docRef -> docRef.set(moodEvt2),
                        new DbOpResultHandler<>(
                                Void -> finishCallback.run(),
                                e -> assertNull("Failed to set an existing document to a new value", e)
                        )));
        System.out.println("  - Set nonexisting event");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, "-12345",
                        docRef -> docRef.set(moodEvt2),
                        new DbOpResultHandler<>(
                                Void -> assertNull("Should not succeed to set a non-existing document to a new value", false),
                                e -> finishCallback.run()
                        )));

        System.out.println("  - Get event #2");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, "2",
                        DocumentReference::get,
                        new DbOpResultHandler<>(
                                (result) -> {
                                    MoodEvent evt = result.toObject(MoodEvent.class);
                                    assertEquals(evt.getMoodType(), MoodEvent.MoodType.ANGER);
                                    assertEquals(evt.getReasonText(), "AWWW MAN");
                                    finishCallback.run();
                                },
                                (e) -> assertNull("Error getting existing document", e)
                        )));
        System.out.println("  - Get non existing event");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, "-100",
                        DocumentReference::get,
                        new DbOpResultHandler<>(
                                (result) -> assertNull("Should not succeed in getting an non-existing document", result),
                                ignored -> finishCallback.run()
                        )));

        System.out.println("  - Delete event #3");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, "3",
                        DocumentReference::delete,
                        new DbOpResultHandler<>(
                                Void -> finishCallback.run(),
                                e -> assertNull("Error deleting existing document", e)
                        )));
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, "3",
                        DocumentReference::get,
                        new DbOpResultHandler<>(
                                (result) -> assertNull("Should not succeed in getting the deleted document", result),
                                ignored -> finishCallback.run()
                        )));
        System.out.println("  - Delete non existing event");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_MOOD_EVENTS, "-999999",
                        DocumentReference::delete,
                        new DbOpResultHandler<>(
                                Void -> assertNull("Should not succeed in deleting an non-existing document", true),
                                e -> finishCallback.run()
                        )));
    }

    /**
     * Tests operateDocumentById's update functionality
     */
    @Test
    public void operateDocumentByIdUpdateTest() throws InterruptedException {
        System.out.println("=> operateDocumentByIdUpdateTest");
        System.out.println("  - Update User #1 - union user 2 & user 3");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_USERS, "1",
                        docRef -> docRef.update("followers",
                                FieldValue.arrayUnion("user2", "user3")),
                        new DbOpResultHandler<>(
                                Void -> finishCallback.run(),
                                e -> assertNull("Failed to update an existing document with a new value", e)
                        )));
        System.out.println("  - Check union result");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_USERS, "1",
                        DocumentReference::get,
                        new DbOpResultHandler<>(
                                (result) -> {
                                    User evt = result.toObject(User.class);
                                    assertEquals(evt.getFollowers(), List.of("user2", "user3"));
                                    finishCallback.run();
                                },
                                (e) -> assertNull("Error getting existing document", e)
                        )));
        System.out.println("  - Update User #1 - remove user 3");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_USERS, "1",
                        docRef -> docRef.update("followers",
                                FieldValue.arrayRemove("user3")),
                        new DbOpResultHandler<>(
                                Void -> finishCallback.run(),
                                e -> assertNull("Failed to update an existing document with a new value", e)
                        )));
        System.out.println("  - Check remove result");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_USERS, "1",
                        DocumentReference::get,
                        new DbOpResultHandler<>(
                                (result) -> {
                                    User evt = result.toObject(User.class);
                                    assertEquals(evt.getFollowers(), List.of("user2"));
                                    finishCallback.run();
                                },
                                (e) -> assertNull("Error getting existing document", e)
                        )));

        System.out.println("  - Update non-existing document");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_USERS, "-112233",
                        docRef -> docRef.update("followers",
                                FieldValue.arrayRemove("some element")),
                        new DbOpResultHandler<>(
                                Void -> assertNull("The update on a non-existing document was successful", true),
                                e -> finishCallback.run()
                        )));
    }

    /**
     * Tests operateTransaction's functionality
     */
    @Test
    public void operateTransactionTest() throws InterruptedException {
        System.out.println("=> operateTransactionTest");
        System.out.println("  - transaction start");
        Transaction.Function<Void> logic;
        DocumentReference docRef = DbUtils.getDocRef(DbUtils.COLL_USERS, "1");
        logic = transaction -> {
            transaction.update(docRef, "followers",
                    FieldValue.arrayUnion("user2", "user3", "user4"));
            transaction.update(docRef, "following",
                    FieldValue.arrayUnion("user3"));
            transaction.update(docRef, "followers",
                    FieldValue.arrayRemove("user3"));
            return null;
        };
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateTransaction(logic,
                        new DbOpResultHandler<>(
                                Void -> finishCallback.run(),
                                e -> assertNull("Failed to process transaction", e)
                        )));
        System.out.println("  - Check transaction result");
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_USERS, "1",
                        DocumentReference::get,
                        new DbOpResultHandler<>(
                                (result) -> {
                                    User evt = result.toObject(User.class);
                                    assertEquals(evt.getFollowers(), List.of("user2", "user4"));
                                    assertEquals(evt.getFollowing(), List.of("user3"));
                                    finishCallback.run();
                                },
                                (e) -> assertNull("Error getting existing document", e)
                        )));
    }
}
