package com.example.segfaultsquadapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.LargeTest;

import com.example.segfaultsquadapplication.impl.db.DbUtils;
import com.example.segfaultsquadapplication.impl.user.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

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
        new MockDb(DbUtils.COLL_MOOD_EVENTS, DbUtils.COLL_USERS);
        MockDb.await(callback ->
                () -> UserManager.login(
                        "user1@gmail.com",
                        "Why did I still type in this password when mock auth simply accepts anything?",
                        (isSuccess, failureReason) -> callback.run()));
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
        MockDb.await(callback ->
                () -> UserManager.login(
                        "user2@gmail.com",
                        "PWD...",
                        (isSuccess, failureReason) -> callback.run()));
        // TODO: check user file created.

        System.out.println("  - user file kept intact upon further logins");
        MockDb.await(callback ->
                () -> UserManager.login(
                        "user2@gmail.com",
                        "Why did I type in such a long password when mock auth simply accepts anything?",
                        (isSuccess, failureReason) -> callback.run()));
    }
}
