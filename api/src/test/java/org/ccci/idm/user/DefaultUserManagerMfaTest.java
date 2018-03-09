package org.ccci.idm.user;

import static org.ccci.idm.user.TestUtil.newUser;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.ccci.idm.user.dao.UserDao;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.ReadableInstant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DefaultUserManagerMfaTest {
    private static final long TIME_INTERVAL = 1000;
    private static final int MFA_INTRUDER_ATTEMPTS = 20;
    private static final Duration MFA_RESET_INTERVAL = Duration.standardMinutes(10);
    private static final Duration MFA_LOCK_DURATION = Duration.standardMinutes(15);

    private DefaultUserManager userManager;
    @Mock
    private UserDao userDao;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        userManager = new DefaultUserManager();
        userManager.setUserDao(userDao);
        userManager.setMfaIntruderAttempts(MFA_INTRUDER_ATTEMPTS);
        userManager.setMfaIntruderLockDuration(MFA_LOCK_DURATION);
    }

    @Test
    public void verifyIsMfaIntruderLocked() {
        final User user = createUser();
        user.setMfaIntruderLocked(true);
        user.setMfaIntruderResetTime(Instant.now().plus(TIME_INTERVAL));
        assertTrue(userManager.isMfaIntruderLocked(user));
    }

    @Test
    public void verifyIsMfaIntruderLockedNotLocked() {
        final User user = createUser();
        user.setMfaIntruderLocked(false);
        assertFalse(userManager.isMfaIntruderLocked(user));
    }

    @Test
    public void verifyIsMfaIntruderLockedNoResetTime() {
        final User user = createUser();
        user.setMfaIntruderLocked(true);
        user.setMfaIntruderResetTime(null);
        assertFalse(userManager.isMfaIntruderLocked(user));
    }

    @Test
    public void verifyIsMfaIntruderLockedExpiredResetTime() {
        final User user = createUser();
        user.setMfaIntruderLocked(true);
        user.setMfaIntruderResetTime(Instant.now().minus(TIME_INTERVAL));
        assertFalse(userManager.isMfaIntruderLocked(user));
    }

    @Test
    public void verifyTrackFailedMfaLoginAlreadyLocked() throws Exception {
        final User user = createUser();
        user.setMfaIntruderLocked(true);
        user.setMfaIntruderAttempts(1);
        final ReadableInstant resetTime = Instant.now().plus(TIME_INTERVAL);
        user.setMfaIntruderResetTime(resetTime);

        userManager.trackFailedMfaLogin(user);
        assertTrue(user.isMfaIntruderLocked());
        assertEquals(1, (int) user.getMfaIntruderAttempts());
        assertEquals(resetTime, user.getMfaIntruderResetTime());
        verify(userDao, never()).update(user, user, User.Attr.MFA_INTRUDER_DETECTION);
    }

    @Test
    public void verifyTrackFailedMfaLoginExpiredLock() throws Exception {
        final User user = createUser();
        user.setMfaIntruderLocked(true);
        user.setMfaIntruderAttempts(20);
        user.setMfaIntruderResetTime(Instant.now().minus(TIME_INTERVAL));

        final ReadableInstant minExpectedTime = Instant.now().plus(MFA_RESET_INTERVAL).minus(1);
        userManager.trackFailedMfaLogin(user);
        final ReadableInstant maxExpectedTime = Instant.now().plus(MFA_RESET_INTERVAL).plus(1);
        assertFalse(user.isMfaIntruderLocked());
        assertEquals(1, (int) user.getMfaIntruderAttempts());
        assertThat(user.getMfaIntruderResetTime(), allOf(greaterThan(minExpectedTime), lessThan(maxExpectedTime)));
        verify(userDao).update(user, user, User.Attr.MFA_INTRUDER_DETECTION);
    }

    @Test
    public void verifyTrackFailedMfaLoginHasState() throws Exception {
        final User user = createUser();
        user.setMfaIntruderLocked(false);
        user.setMfaIntruderAttempts(5);
        final ReadableInstant resetTime = Instant.now().plus(TIME_INTERVAL);
        user.setMfaIntruderResetTime(resetTime);

        userManager.trackFailedMfaLogin(user);
        assertFalse(user.isMfaIntruderLocked());
        assertEquals(6, (int) user.getMfaIntruderAttempts());
        assertEquals(resetTime, user.getMfaIntruderResetTime());
        verify(userDao).update(user, user, User.Attr.MFA_INTRUDER_DETECTION);
    }

    @Test
    public void verifyTrackFailedMfaLoginFinalFailure() throws Exception {
        final User user = createUser();
        user.setMfaIntruderLocked(false);
        user.setMfaIntruderAttempts(MFA_INTRUDER_ATTEMPTS - 1);
        user.setMfaIntruderResetTime(Instant.now().plus(TIME_INTERVAL));

        final ReadableInstant minExpectedTime = Instant.now().plus(MFA_LOCK_DURATION).minus(1);
        userManager.trackFailedMfaLogin(user);
        final ReadableInstant maxExpectedTime = Instant.now().plus(MFA_LOCK_DURATION).plus(1);
        assertTrue(user.isMfaIntruderLocked());
        assertEquals(MFA_INTRUDER_ATTEMPTS, (int) user.getMfaIntruderAttempts());
        assertThat(user.getMfaIntruderResetTime(), allOf(greaterThan(minExpectedTime), lessThan(maxExpectedTime)));
        verify(userDao).update(user, user, User.Attr.MFA_INTRUDER_DETECTION);
    }

    @Test
    public void verifyResetMfaIntruderLock() throws Exception {
        final User user = createUser();
        user.setMfaIntruderLocked(true);
        user.setMfaIntruderResetTime(Instant.now().plus(TIME_INTERVAL));
        user.setMfaIntruderAttempts(100);

        userManager.resetMfaIntruderLock(user);
        assertFalse(userManager.isMfaIntruderLocked(user));
        assertFalse(user.isMfaIntruderLocked());
        assertNull(user.getMfaIntruderResetTime());
        assertNull(user.getMfaIntruderAttempts());
        verify(userDao).update(user, user, User.Attr.MFA_INTRUDER_DETECTION);
    }

    @Test
    public void verifyResetMfaIntruderLockNoOp() throws Exception {
        final User user = createUser();
        user.setMfaIntruderLocked(false);
        user.setMfaIntruderResetTime(null);
        user.setMfaIntruderAttempts(null);

        userManager.resetMfaIntruderLock(user);
        assertFalse(userManager.isMfaIntruderLocked(user));
        assertFalse(user.isMfaIntruderLocked());
        assertNull(user.getMfaIntruderResetTime());
        assertNull(user.getMfaIntruderAttempts());
        verify(userDao, never()).update(user, user, User.Attr.MFA_INTRUDER_DETECTION);
    }

    private User createUser() {
        final User user = newUser();
        when(userDao.findByGuid(eq(user.getGuid()), anyBoolean())).thenReturn(user);
        return user;
    }
}
