package com.example.segfaultsquadapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;

import com.example.segfaultsquadapplication.impl.comment.Comment;
import com.example.segfaultsquadapplication.impl.comment.CommentManager;
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
 * Runs tests for CommentManager content.
 *
 * USE SILENT CLASS to prevent lenient errors
 * All of MockDb's features may not be needed for all test cases in all test files. Use silent version.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@LargeTest
public class CommentManagerTest {
    private static final MoodEvent[] INIT_EVENTS = {
            new MoodEvent("uid1", MoodEvent.MoodType.SURPRISE, "reason", null, null, true),
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
        for (int i = 1; i <= 2; i ++) {
            User user = new User("uid" + i, "user" + i, "uid" + i + "@gmail.com");
            MockDb.await((finishCallback) ->
                    () -> DbUtils.addObjectToCollection(DbUtils.COLL_USERS, user,
                            new DbOpResultHandler<>(
                                    ignored -> finishCallback.run(),
                                    Assert::assertNull
                            )));
        }
    }

    /*
     * TESTS BELOW
     */

    /**
     * Tests submitComment, getCommentsForMood and removeAllComments' functionality.
     */
    @Test
    public void createGetClearCommentTest() throws InterruptedException {
        System.out.println("=> createCommentTest");

        System.out.println("  - add comment does not crash");
        Comment validEvent = new Comment("1", "uid1", "user1", "valid");
        CommentManager.submitComment(validEvent);

        System.out.println("  - gets the added comment");
        ArrayList<Comment> cmts = new ArrayList<>();
        MockDb.await((finishCallback) ->
                () ->
                        CommentManager.getCommentsForMood("1", cmts,
                                isSuccess -> {
                                    assertTrue(isSuccess);
                                    finishCallback.run();
                                }));
        assertEquals(1, cmts.size());
        assertEquals("1", cmts.get(0).getEventId());
        assertEquals("uid1", cmts.get(0).getUserId());
        assertEquals("valid", cmts.get(0).getText());

        System.out.println("  - clear the comments for the mood");
        Comment anotherValidEvent = new Comment("1", "uid1", "user1", "valid another cmt");
        CommentManager.submitComment(anotherValidEvent);
        CommentManager.removeAllComments("1");
        // Check whether they have been all removed
        ArrayList<Comment> commentsAfterRmv = new ArrayList<>();
        MockDb.await((finishCallback) ->
                () ->
                        CommentManager.getCommentsForMood("1", commentsAfterRmv,
                                isSuccess -> {
                                    assertTrue(isSuccess);
                                    finishCallback.run();
                                }));
        assertEquals(0, commentsAfterRmv.size());
    }

    /**
     * Tests submitComment does not crash on invalid info.
     */
    @Test
    public void createInvalidTest() throws InterruptedException {
        System.out.println("=> invalid comment does not crash");
        Comment invalidEvent = new Comment("-12345", "uid1", "user1", "test!");
        CommentManager.submitComment(invalidEvent);
        Comment invalidUserId = new Comment("1", "-123uid1", "user1", "test!");
        CommentManager.submitComment(invalidUserId);
    }

    /**
     * Tests getCommentsForMood does not crash on invalid info.
     */
    @Test
    public void getCommentsForMoodInvalidTest() throws InterruptedException {
        System.out.println("=> invalid get comments does not crash");
        ArrayList<Comment> cmts = new ArrayList<>();
        MockDb.await((finishCallback) ->
                () ->
                        CommentManager.getCommentsForMood("-12345", cmts,
                                isSuccess -> {
                                    assertEquals(0, cmts.size());
                                    finishCallback.run();
                                }));
    }

    /**
     * Tests removeAllComments does not crash on invalid info.
     */
    @Test
    public void removeAllCommentsInvalidTest() throws InterruptedException {
        System.out.println("=> invalid remove all comments does not crash");
        System.out.println("  - invalid mood event");
        CommentManager.removeAllComments("-12345678");
        System.out.println("  - empty comments for the mood event");
        CommentManager.removeAllComments("1");
        CommentManager.removeAllComments("1");
        CommentManager.removeAllComments("1");
        // Sleep a bit to make sure no error was thrown
        Thread.sleep(2000);
    }
}
