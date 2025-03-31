package com.example.segfaultsquadapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;

import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.db.DbOpResultHandler;
import com.example.segfaultsquadapplication.impl.user.User;
import com.example.segfaultsquadapplication.impl.user.UserManager;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs tests for User-related and Following content.
 * Because of the nature of such topics, it would be the most appropriate to test them altogether.
 *
 * USE SILENT CLASS to prevent lenient errors
 * All of MockDb's features may not be needed for all test cases in all test files. Use silent version.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
@LargeTest
public class UserAndFollowingTest {
    @Before
    public void setup() throws InterruptedException {
        new MockDb(DbUtils.COLL_MOOD_EVENTS, DbUtils.COLL_USERS, DbUtils.COLL_COMMENTS);
        MockDb.await(finishCallback ->
                () -> UserManager.login(
                        "user1@gmail.com",
                        "Why did I still type in this password when mock auth simply accepts anything?",
                        (isSuccess, failureReason) -> finishCallback.run()));
    }

    /*
     * TESTS BELOW
     */

    /**
     * Tests miscellaneous getters in UserManager.
     */
    @Test
    public void miscUserManagerGettersTest() {
        System.out.println("=> miscUserManagerGettersTest");

        System.out.println("  - valid inputs");
        assertEquals( UserManager.getCurrUser().getUid(), UserManager.getUserId() );
        assertEquals( UserManager.getCurrUser().getUid(), UserManager.getUserId(UserManager.getCurrUser()) );
        assertEquals( "user1", UserManager.getUsername(UserManager.getCurrUser()) );

        System.out.println("  - null inputs");
        assertNull(UserManager.getUsername(null));
        assertNull(UserManager.getUserId(null));
    }

    /**
     * Tests user login; furthermore, it should create a proper user file when and only when first logging in.
     */
    @Test
    public void userLoginTest() throws InterruptedException {
        System.out.println("=> userLoginTest");

        System.out.println("  - user file created");
        MockDb.await(finishCallback ->
                () -> UserManager.login(
                        "user2@gmail.com",
                        "PWD...",
                        (isSuccess, failureReason) -> finishCallback.run()));
        // Check user file created.
        AtomicReference<User> holder = new AtomicReference<>();
        MockDb.await(finishCallback ->
                () -> UserManager.loadUserData(UserManager.getUserId(), holder,
                        isSuccess -> {
                            assertTrue("User file not created after login", isSuccess);
                            finishCallback.run();
                        }));
        User dbUser = holder.get();
        FirebaseUser firebaseUser = UserManager.getCurrUser();
        assertEquals(UserManager.getUsername(firebaseUser), dbUser.getUsername());
        assertEquals(UserManager.getUserId(firebaseUser), dbUser.getDbFileId());
        // Should not save user's email for security reasons; we won't need it anyways.
        assertEquals(null, dbUser.getEmail());

        System.out.println("  - user file kept intact upon further logins");
        // Add a follower
        MockDb.await( (finishCallback) ->
                () -> DbUtils.operateDocumentById(DbUtils.COLL_USERS, UserManager.getUserId(),
                        docRef -> docRef.update("followers",
                                FieldValue.arrayUnion("user1")),
                        new DbOpResultHandler<>(
                                Void -> finishCallback.run(),
                                e -> assertNull("Failed to add follower to user", e)
                        )));
        // Login again
        MockDb.await(finishCallback ->
                () -> UserManager.login(
                        "user2@gmail.com",
                        "Why did I type in such a long password when mock auth simply accepts anything?",
                        (isSuccess, failureReason) -> finishCallback.run()));
        // Check for follower
        MockDb.await(finishCallback ->
                () -> UserManager.loadUserData(UserManager.getUserId(), holder,
                        isSuccess -> {
                            assertTrue("User file disappeared after second login", isSuccess);
                            finishCallback.run();
                        }));
        assertEquals(List.of("user1"), holder.get().getFollowers());
    }
}
