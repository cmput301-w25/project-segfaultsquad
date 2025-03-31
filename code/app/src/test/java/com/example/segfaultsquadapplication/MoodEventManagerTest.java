package com.example.segfaultsquadapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;

import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEvent;
import com.example.segfaultsquadapplication.impl.moodevent.MoodEventManager;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs tests for MoodEventManager content.
 *
 * USE SILENT CLASS to prevent lenient errors
 * All of MockDb's features may not be needed for all test cases in all test files. Use silent version.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@LargeTest
public class MoodEventManagerTest {
    private static final MoodEvent[] INIT_EVENTS = {
            new MoodEvent("uid1", MoodEvent.MoodType.SADNESS, "reason", null, null, true),
            new MoodEvent("uid1", MoodEvent.MoodType.HAPPINESS, "be happy", null, null, true),
            new MoodEvent("uid1", MoodEvent.MoodType.DISGUST, "can we stop making placeholder mood events", null, null, true),
            new MoodEvent("uid1", MoodEvent.MoodType.ANGER, "it's so hard to make up reasons", null, null, true),
            new MoodEvent("uid2", MoodEvent.MoodType.SADNESS, "reason", null, null, true),
    };

    @Before
    public void setup() throws InterruptedException {
        new MockDb(DbUtils.COLL_MOOD_EVENTS, DbUtils.COLL_USERS, DbUtils.COLL_COMMENTS);

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
     * Tests createMoodEvent's functionality.
     */
    @Test
    public void createMoodEventTest() throws InterruptedException {
        System.out.println("=> createMoodEventTest");

        System.out.println("  - invalid mood type");
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.createMoodEvent(null, null,
                        "reason_text", true, MoodEvent.SocialSituation.ALONE, null,
                        isSuccess -> {
                            assertFalse("Creating mood event from invalid mood type was successful", isSuccess);
                            finishCallback.run();
                        }));

        System.out.println("  - invalid reason length");
        StringBuilder longReason = new StringBuilder();
        for (int i = 0; i < 201; i ++)
            longReason.append("R");
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.createMoodEvent(null, MoodEvent.MoodType.ANGER,
                        longReason.toString(), true, MoodEvent.SocialSituation.ALONE, null,
                        isSuccess -> {
                            assertFalse("Creating mood event from invalid reason length was successful", isSuccess);
                            finishCallback.run();
                        }));

        System.out.println("  - correctly save event with just the right reason length");
        StringBuilder shorterReason = new StringBuilder();
        for (int i = 0; i < 200; i ++)
            shorterReason.append("R");
        MoodEvent.MoodType type = MoodEvent.MoodType.DISGUST;
        boolean isPublic = false;
        MoodEvent.SocialSituation situation = MoodEvent.SocialSituation.WITH_GROUP;
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.createMoodEvent(null, type,
                        shorterReason.toString(), isPublic, situation, null,
                        isSuccess -> {
                            assertTrue("Could not save a valid mood event", isSuccess);
                            finishCallback.run();
                        }));
        // Check the mood event!
        ArrayList<MoodEvent> eventHolder = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getAllMoodEvents(UserManager.getUserId(), MoodEventManager.MoodEventFilter.MOST_RECENT_1, eventHolder,
                        isSuccess -> {
                            assertTrue("Could not verify the saved mood event", isSuccess);
                            finishCallback.run();
                        }));
        MoodEvent addedMoodEvent = eventHolder.get(0);
        assertEquals(type, addedMoodEvent.getMoodType());
        assertEquals(shorterReason.toString(), addedMoodEvent.getReasonText());
        assertEquals(isPublic, addedMoodEvent.isPublic());
        assertEquals(situation, addedMoodEvent.getSocialSituation());
    }

    /**
     * Tests updateMoodEvent's functionality.
     */
    @Test
    public void updateMoodEventTest() throws InterruptedException {
        System.out.println("=> updateMoodEventTest");

        // Get the mood event
        AtomicReference<MoodEvent> holder = new AtomicReference<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getMoodEventById("1", holder,
                        isSuccess -> {
                            assertTrue("Could not get the pre-defined first mood event", isSuccess);
                            finishCallback.run();
                        }));

        System.out.println("  - invalid mood type");
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.updateMoodEvent(null, holder.get(), null,
                        "reason_text", true, MoodEvent.SocialSituation.ALONE, null,
                        isSuccess -> {
                            assertFalse("Update mood event from invalid mood type was successful", isSuccess);
                            finishCallback.run();
                        }));

        System.out.println("  - invalid reason length");
        StringBuilder longReason = new StringBuilder();
        for (int i = 0; i < 201; i ++)
            longReason.append("R");
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.updateMoodEvent(null, holder.get(), MoodEvent.MoodType.ANGER,
                        longReason.toString(), true, MoodEvent.SocialSituation.ALONE, null,
                        isSuccess -> {
                            assertFalse("Update mood event from invalid reason length was successful", isSuccess);
                            finishCallback.run();
                        }));

        System.out.println("  - correctly update event info");
        StringBuilder shorterReason = new StringBuilder();
        for (int i = 0; i < 200; i ++)
            shorterReason.append("R");
        MoodEvent.MoodType type = MoodEvent.MoodType.DISGUST;
        boolean isPublic = false;
        MoodEvent.SocialSituation situation = MoodEvent.SocialSituation.WITH_GROUP;
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.updateMoodEvent(null, holder.get(), type,
                        shorterReason.toString(), isPublic, situation, null,
                        isSuccess -> {
                            assertTrue("Could not update a valid mood event", isSuccess);
                            finishCallback.run();
                        }));
        // Check the mood event!
        AtomicReference<MoodEvent> updatedHolder = new AtomicReference<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getMoodEventById("1", updatedHolder,
                        isSuccess -> {
                            assertTrue("Could not get the modified first mood event", isSuccess);
                            finishCallback.run();
                        }));
        MoodEvent addedMoodEvent = updatedHolder.get();
        assertEquals(type, addedMoodEvent.getMoodType());
        assertEquals(shorterReason.toString(), addedMoodEvent.getReasonText());
        assertEquals(isPublic, addedMoodEvent.isPublic());
        assertEquals(situation, addedMoodEvent.getSocialSituation());
    }

    /**
     * Tests getAllMoodEvents's functionality.
     */
    @Test
    public void getAllMoodEventsTest() throws InterruptedException {
        System.out.println("=> getAllMoodEventsTest");

        System.out.println("  - returns empty list for invalid user id");
        ArrayList<MoodEvent> holderInvalidUserId = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getAllMoodEvents("1234567544678", MoodEventManager.MoodEventFilter.ALL,
                        holderInvalidUserId,
                        isSuccess -> {
                            assertTrue("Get all mood event failed", isSuccess);
                            finishCallback.run();
                        }));
        assertEquals(0, holderInvalidUserId.size());

        System.out.println("  - returns all events with proper ordering");
        ArrayList<MoodEvent> holderAll = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getAllMoodEvents("uid1", MoodEventManager.MoodEventFilter.ALL,
                        holderAll,
                        isSuccess -> {
                            assertTrue("Get all mood event failed", isSuccess);
                            finishCallback.run();
                        }));
        assertEquals(4, holderAll.size());
        for (int i = 0; i < holderAll.size(); i ++) {
            assertEquals("uid1", holderAll.get(i).getUserId());
        }
        for (int i = 1; i < holderAll.size(); i ++) {
            assertTrue( holderAll.get(i-1).getTimestamp().compareTo(holderAll.get(i).getTimestamp()) >= 0 );
        }

        System.out.println("  - returns first 3 events with proper ordering with the filter");
        ArrayList<MoodEvent> holderFirst3 = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getAllMoodEvents("uid1", MoodEventManager.MoodEventFilter.MOST_RECENT_3,
                        holderFirst3,
                        isSuccess -> {
                            assertTrue("Get first 3 mood event failed", isSuccess);
                            finishCallback.run();
                        }));
        assertEquals(holderFirst3.size(), 3);
        for (int i = 0; i < holderFirst3.size(); i ++) {
            assertEquals("uid1", holderFirst3.get(i).getUserId());
            // Make sure it is the first 3!
            assertEquals(holderAll.get(i), holderFirst3.get(i));
        }

        System.out.println("  - returns the first event with the filter");
        ArrayList<MoodEvent> holderFirst = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getAllMoodEvents("uid1", MoodEventManager.MoodEventFilter.MOST_RECENT_1,
                        holderFirst,
                        isSuccess -> {
                            assertTrue("Get first 1 mood event failed", isSuccess);
                            finishCallback.run();
                        }));
        assertEquals("uid1", holderFirst.get(0).getUserId());
        // Make sure it is the first!
        assertEquals(holderAll.get(0), holderFirst.get(0));

        System.out.println("  - run without error when only 1 event available yet filtered for first 3 events");
        ArrayList<MoodEvent> holderLessThanLimit = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getAllMoodEvents("uid2", MoodEventManager.MoodEventFilter.MOST_RECENT_3,
                        holderLessThanLimit,
                        isSuccess -> {
                            assertTrue("Get first 3 mood event when only has 1 failed", isSuccess);
                            finishCallback.run();
                        }));
        assertEquals(1, holderLessThanLimit.size());
        assertEquals("uid2", holderLessThanLimit.get(0).getUserId());
    }

    /**
     * Tests getMoodEventById's functionality.
     */
    @Test
    public void getMoodEventByIdTest() throws InterruptedException {
        System.out.println("=> getMoodEventByIdTest");

        System.out.println("  - flags error for non-existing document");
        AtomicReference<MoodEvent> holderError = new AtomicReference<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getMoodEventById("1234567544678",
                        holderError,
                        isSuccess -> {
                            assertFalse("Get non-existing mood event by Id should fail", isSuccess);
                            finishCallback.run();
                        }));

        System.out.println("  - gets the correct data for a valid doc ID");
        AtomicReference<MoodEvent> holderCorrectId = new AtomicReference<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getMoodEventById("2",
                        holderCorrectId,
                        isSuccess -> {
                            assertTrue("Get an existing event by ID should success", isSuccess);
                            finishCallback.run();
                        }));
        MoodEvent event = holderCorrectId.get();
        assertEquals("uid1", event.getUserId());
        assertEquals("be happy", event.getReasonText());
        assertEquals(MoodEvent.MoodType.HAPPINESS, event.getMoodType());
    }

    /**
     * Tests deleteMoodEventById's functionality.
     */
    @Test
    public void deleteMoodEventByIdTest() throws InterruptedException {
        System.out.println("=> deleteMoodEventByIdTest");

        System.out.println("  - does nothing for non-existing document");
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.deleteMoodEventById("1234567544678",
                        isSuccess -> {
                            assertTrue("Deleting non-existing mood event by Id should do nothing, not fail", isSuccess);
                            finishCallback.run();
                        }));

        System.out.println("  - deletes the correct document for a valid doc ID");
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.deleteMoodEventById("5",
                        isSuccess -> {
                            assertTrue("Delete an existing event by ID should success", isSuccess);
                            finishCallback.run();
                        }));
        // Find out whether the deletion affected other documents
        ArrayList<MoodEvent> holderUser1 = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getAllMoodEvents("uid1", MoodEventManager.MoodEventFilter.ALL,
                        holderUser1,
                        isSuccess -> {
                            assertTrue("Get all mood event failed", isSuccess);
                            finishCallback.run();
                        }));
        assertEquals(4, holderUser1.size());
        // Find out whether the deletion is successful!
        ArrayList<MoodEvent> holderUser2 = new ArrayList<>();
        MockDb.await( (finishCallback) ->
                () -> MoodEventManager.getAllMoodEvents("uid2", MoodEventManager.MoodEventFilter.ALL,
                        holderUser2,
                        isSuccess -> {
                            assertTrue("Get all mood event failed", isSuccess);
                            finishCallback.run();
                        }));
        assertEquals(0, holderUser2.size());
    }
}
