package org.ccci.idm.user.ldaptive.dao;

import static org.ccci.idm.user.TestUtil.guid;
import static org.ccci.idm.user.TestUtil.newUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNotNull;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.SearchQuery;
import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.util.HashUtility;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"ldap.xml", "config.xml", "dao-default.xml"})
public class LdaptiveUserDaoIT {
    private static final Random RAND = new SecureRandom();

    private static final Function<User, String> FUNCTION_GUID = new Function<User, String>() {
        @Nullable
        @Override
        public String apply(final User user) {
            return user != null ? user.getGuid() : null;
        }
    };

    @Inject
    private LdaptiveUserDao dao;

    @Inject
    @Named("group1")
    private Group group1;
    @Inject
    @Named("group2")
    private Group group2;

    // required config values for tests to pass successfully
    @Value("${ldap.url:#{null}}")
    private String url = null;
    @Value("${ldap.base:#{null}}")
    private String base = null;
    @Value("${ldap.userdn:#{null}}")
    private String username = null;
    @Value("${ldap.password:#{null}}")
    private String password = null;
    @Value("${ldap.dn.user:#{null}}")
    private String dn = null;
    @Value("${ldap.dn.group:#{null}}")
    private String groupDn = null;

    private void assumeConfigured() throws Exception {
        assumeNotNull(url, base, username, password, dn);
        assumeNotNull(dao);
    }

    private void assumeGroupsConfigured() throws Exception {
        assumeNotNull(groupDn);
        assumeNotNull(group1, group2);
        assumeFalse(group1.equals(group2));
    }

    @Test
    public void testCreateUser() throws Exception {
        assumeConfigured();

        final User user = newUser();

        this.dao.save(user);
    }

    @Test
    public void testFindByEmail() throws Exception {
        assumeConfigured();

        // create a new user
        final User user = newUser();
        this.dao.save(user);

        // fetch the user using findByEmail
        final User foundUser = this.dao.findByEmail(user.getEmail(), false);

        assertNotNull(foundUser);
        assertEquals(user.getGuid(), foundUser.getGuid());
        assertEquals(user.getEmail(), foundUser.getEmail());
    }

    @Test
    public void testUpdateUser() throws Exception {
        assumeConfigured();

        // create user
        User user = newUser();
        this.dao.save(user);

        // update user's name
        user.setFirstName(user.getFirstName() + " modified");
        user.setLastName(user.getLastName() + " modified");
        this.dao.update(user, User.Attr.NAME);

        // load user from database
        final User foundUser = this.dao.findByGuid(user.getGuid(), false);

        // make sure the updates persisted correctly
        assertNotNull(foundUser);
        assertEquals(user.getGuid(), foundUser.getGuid());
        assertEquals(user.getEmail(), foundUser.getEmail());
        assertEquals(user.getFirstName(), foundUser.getFirstName());
        assertEquals(user.getLastName(), foundUser.getLastName());
    }

    @Test
    public void testLoginDate() throws Exception {
        assumeConfigured();

        final User user = newUser();
        final String guid = user.getGuid();
        user.setLoginTime(new DateTime().minusDays(30).secondOfMinute().roundFloorCopy());
        this.dao.save(user);

        // see if we load the same value from ldap
        final User saved1 = this.dao.findByGuid(guid, true);
        assertTrue(saved1.getLoginTime().isEqual(user.getLoginTime()));

        // update the login time to now
        saved1.setLoginTime(new DateTime().secondOfMinute().roundFloorCopy());
        this.dao.update(saved1, User.Attr.LOGINTIME);

        // check to see if the update succeeded
        final User saved2 = this.dao.findByGuid(guid, true);
        assertTrue(saved2.getLoginTime().isEqual(saved1.getLoginTime()));
        assertFalse(saved2.getLoginTime().isEqual(user.getLoginTime()));
    }

    @Test
    public void testPasswordChangedTime() throws Exception {
        assumeConfigured();

        // create user
        final User user = newUser();
        user.setPassword(guid());
        final String guid = user.getGuid();
        this.dao.save(user);

        // check for an initial password changeTime
        final User user1 = this.dao.findByGuid(guid, true);
        assertNotNull(user1);
        ReadableInstant changeTime = user1.getPasswordChangedTime();
        assertNotNull(changeTime);

        String password = guid();

        // change password
        Thread.sleep(1000); // sleep for a second to make sure the new change time is different
        user1.setPassword(password);
        this.dao.update(user1, User.Attr.PASSWORD);

        // check pwdChangedTime
        final User user2 = this.dao.findByGuid(guid, true);
        assertNotNull(user2);
        assertNotNull(user2.getPasswordChangedTime());
        assertNotEquals(changeTime, user2.getPasswordChangedTime());

        changeTime = user2.getPasswordChangedTime();

        // attempt update with same password to verify that pwdChangedTime does, in fact, still change
        Thread.sleep(1000); // sleep for a second to make sure the new change time is different
        user2.setPassword(password);
        this.dao.update(user2, User.Attr.PASSWORD);

        // check pwdChangedTime
        final User user3 = this.dao.findByGuid(guid, true);
        assertNotNull(user3);
        assertNotNull(user3.getPasswordChangedTime());
        assertNotEquals(changeTime, user3.getPasswordChangedTime());
    }

    @Test
    public void testDeactivatedFiltering() throws Exception {
        assumeConfigured();

        // create a base user
        final User user1 = getStaffUser();
        user1.setFirstName("first_" +RAND.nextInt(Integer.MAX_VALUE));
        user1.setLastName("last_" +RAND.nextInt(Integer.MAX_VALUE));

        // create a duplicate deactivated user
        final User user2 = getStaffUser();
        user2.setEmail(user1.getEmail());
        user2.setFirstName(user1.getFirstName());
        user2.setLastName(user1.getLastName());
        user2.setDeactivated(true);

        // save both users
        this.dao.save(user1);
        this.dao.save(user2);

        // test findAllByFirstName
        {
            final Set<String> active = FluentIterable.from(this.dao.findAllByFirstName(user1.getFirstName(), false))
                    .transform(FUNCTION_GUID).toSet();
            final Set<String> all = FluentIterable.from(this.dao.findAllByFirstName(user1.getFirstName(), true))
                    .transform(FUNCTION_GUID).toSet();

            assertEquals(1, active.size());
            assertEquals(2, all.size());

            assertTrue(active.contains(user1.getGuid()));
            assertFalse(active.contains(user2.getGuid()));
            assertTrue(all.contains(user1.getGuid()));
            assertTrue(all.contains(user2.getGuid()));
        }

        // test findAllByLastName
        {
            final Set<String> active = FluentIterable.from(this.dao.findAllByLastName(user1.getLastName(), false))
                    .transform(FUNCTION_GUID).toSet();
            final Set<String> all = FluentIterable.from(this.dao.findAllByLastName(user1.getLastName(), true))
                    .transform(FUNCTION_GUID).toSet();

            assertEquals(1, active.size());
            assertEquals(2, all.size());

            assertTrue(active.contains(user1.getGuid()));
            assertFalse(active.contains(user2.getGuid()));
            assertTrue(all.contains(user1.getGuid()));
            assertTrue(all.contains(user2.getGuid()));
        }

        // test findAllByEmail
        {
            final Set<String> active = FluentIterable.from(this.dao.findAllByEmail(user1.getEmail(), false))
                    .transform(FUNCTION_GUID).toSet();
            final Set<String> all = FluentIterable.from(this.dao.findAllByEmail(user1.getEmail(), true)).transform
                    (FUNCTION_GUID).toSet();

            assertEquals(1, active.size());
            assertEquals(2, all.size());

            assertTrue(active.contains(user1.getGuid()));
            assertFalse(active.contains(user2.getGuid()));
            assertTrue(all.contains(user1.getGuid()));
            assertTrue(all.contains(user2.getGuid()));
        }

        // create a deactivated user with unique attributes to test individual findBy* support
        final User user3 = getStaffUser();
        user3.setFirstName("first_" +RAND.nextInt(Integer.MAX_VALUE));
        user3.setLastName("last_" +RAND.nextInt(Integer.MAX_VALUE));
        user3.setDeactivated(true);

        // save new user
        this.dao.save(user3);

        // test findByEmail
        {
            final User activeUser = this.dao.findByEmail(user3.getEmail(), false);
            final User anyUser = this.dao.findByEmail(user3.getEmail(), true);

            assertNull(activeUser);
            assertNotNull(anyUser);

            assertEquals(user3.getGuid(), anyUser.getGuid());
        }
    }

    @Test
    public void testExceededMaximumResults() throws Exception {
        assumeConfigured();

        // create multiple users with the same name and email, one is deactivated
        final User user1 = getStaffUser();
        user1.setFirstName("first_" + RAND.nextInt(Integer.MAX_VALUE));
        user1.setLastName("last_" + RAND.nextInt(Integer.MAX_VALUE));
        final User user2 = getStaffUser();
        user2.setEmail(user1.getEmail());
        user2.setFirstName(user1.getFirstName());
        user2.setLastName(user1.getLastName());
        user2.setDeactivated(true);

        // save both users
        this.dao.save(user1);
        this.dao.save(user2);

        // test no limit
        {
            this.dao.setMaxSearchResults(0);

            final List<User> firstNameUsers = this.dao.findAllByFirstName(user1.getFirstName(), true);
            assertEquals(2, firstNameUsers.size());

            final List<User> lastNameUsers = this.dao.findAllByLastName(user1.getLastName(), true);
            assertEquals(2, lastNameUsers.size());

            final List<User> emailUsers = this.dao.findAllByEmail(user1.getEmail(), true);
            assertEquals(2, emailUsers.size());
        }

        // test with maxSearchResults = 1
        try {
            this.dao.setMaxSearchResults(1);

            // test first name
            try {
                this.dao.findAllByFirstName(user1.getFirstName(), true);
                fail("ExceededMaximumAllowedResultsException was not thrown by findAllByFirstName");
            } catch (final ExceededMaximumAllowedResultsException e) {
                // this exception was expected
            }

            // test last name
            try {
                this.dao.findAllByLastName(user1.getLastName(), true);
                fail("ExceededMaximumAllowedResultsException was not thrown by findAllByLastName");
            } catch (final ExceededMaximumAllowedResultsException e) {
                // this exception was expected
            }

            // test last name
            try {
                this.dao.findAllByEmail(user1.getEmail(), true);
                fail("ExceededMaximumAllowedResultsException was not thrown by findAllByEmail");
            } catch (final ExceededMaximumAllowedResultsException e) {
                // this exception was expected
            }
        } finally {
            // disable limit to prevent interference in other tests
            this.dao.setMaxSearchResults(0);
        }
    }

    @Test
    public void testEnqueueAll() throws Exception {
        assumeConfigured();

        final BlockingQueue<User> queue = new LinkedBlockingQueue<User>(5);
        final AtomicInteger seen = new AtomicInteger(0);
        final User STOP = new User();
        try {
            // increase the max page size to reduce I/O delays
            this.dao.setMaxPageSize(50);

            // start background processing thread
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            // quit processing if we encounter the stop user
                            if (queue.take() == STOP) {
                                return;
                            }

                            // increment seen counter
                            seen.incrementAndGet();
                        }
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            thread.start();

            // enqueue all users
            final int count = this.dao.enqueueAll(queue, true);

            // stop processing and join thread
            queue.add(STOP);
            thread.join();

            // check final state
            assertTrue("No users were queued, something might be wrong", count > 0);
            assertTrue(queue.isEmpty());
            assertEquals(seen.get(), count);
        } finally {
            // reset maxPageSize to force paging for other tests
            this.dao.setMaxPageSize(1);
        }
    }

    /**
     * Test findAllByQuery. we use bit math to generate and test all possible search combinations
     *
     * @throws Exception
     */
    @Test
    public void testFindAllByQuery() throws Exception {
        assumeConfigured();
        assumeGroupsConfigured();

        // generate all possible intersections of query values on accounts
        final String prefix1 = Integer.toString(RAND.nextInt(Integer.MAX_VALUE));
        final String prefix2 = Integer.toString(RAND.nextInt(Integer.MAX_VALUE));
        final int EMAIL = 1 << 0;
        final int FIRST = 1 << 1;
        final int LAST = 1 << 2;
        final int EMPLOYEEID = 1 << 3;
        final int GROUP = 1 << 4;
        final int DEACTIVATED = 1 << 5;
        final int MAX = 1 << 6;
        for (int i = 0; i < MAX; i++) {
            final User user = getStaffUser();
            user.setEmail(((EMAIL & i) == EMAIL ? prefix1 : prefix2) + "-" + user.getEmail());
            user.setFirstName(((FIRST & i) == FIRST ? prefix1 : prefix2) + "-" + user.getFirstName());
            user.setLastName(((LAST & i) == LAST ? prefix1 : prefix2) + "-" + user.getLastName());
            user.setEmployeeId(((EMPLOYEEID & i) == EMPLOYEEID ? prefix1 : prefix2) + "-" + user.getEmployeeId());
            user.setDeactivated((DEACTIVATED & i) == DEACTIVATED);
            this.dao.save(user);

            // add user to group1
            if ((GROUP & i) == GROUP) {
                this.dao.addToGroup(user, group1);
            }
        }

        // perform all possible query combinations, we only search for prefix1.
        for (int i = 0; i < MAX; i++) {
            int count = MAX;
            boolean hasFilter = false;
            final SearchQuery query = new SearchQuery();
            if ((EMAIL & i) == EMAIL) {
                query.email(prefix1 + "*");
                count /= 2;
                hasFilter = true;
            }
            if ((FIRST & i) == FIRST) {
                query.firstName(prefix1 + "*");
                count /= 2;
                hasFilter = true;
            }
            if ((LAST & i) == LAST) {
                query.lastName(prefix1 + "*");
                count /= 2;
                hasFilter = true;
            }
            if ((EMPLOYEEID & i) == EMPLOYEEID) {
                query.employeeId(prefix1 + "*");
                count /= 2;
                hasFilter = true;
            }
            // groups aren't unique to this test, so we don't consider it as hasFilter
            if ((GROUP & i) == GROUP) {
                query.group(group1);
                count /= 2;
            }

            query.includeDeactivated((DEACTIVATED & i) == DEACTIVATED);
            if (!query.isIncludeDeactivated()) {
                count /= 2;
            }

            // skip any tests that aren't filtering anything
            if (!hasFilter) {
                continue;
            }

            // perform query and test results
            final List<User> results = this.dao.findAllByQuery(query);
            assertEquals("invalid number of results for i = " + i, count, results.size());
        }
    }

    @Test
    public void testCreateStaffUser() throws Exception {
        assumeConfigured();

        // create a new user
        final User user = getStaffUser();

        final String securityAnswer = guid();
        user.setSecurityQuestion(guid());
        user.setSecurityAnswer(securityAnswer);

        this.dao.save(user);

        // make sure we can find it and it's valid
        final User foundUser = this.dao.findByGuid(user.getGuid(), false);
        assertNotNull(foundUser);
        assertEquals(user.getGuid(), foundUser.getGuid());
        assertEquals(user.getEmail(), foundUser.getEmail());
        assertEquals(user.getEmployeeId(), foundUser.getEmployeeId());
        assertEquals(user.getCity(), foundUser.getCity());
        assertEquals(user.getCruDesignation(), foundUser.getCruDesignation());

        assertEquals(user.getSecurityQuestion(), foundUser.getSecurityQuestion());

        assertTrue(foundUser.checkSecurityAnswer(securityAnswer));
    }

    @Test
    public void testUpdateStaffUser() throws Exception {
        assumeConfigured();

        // create a new user
        final User user = getStaffUser();
        this.dao.save(user);

        // check that it saved correctly
        User foundUser = this.dao.findByGuid(user.getGuid(), false);
        assertNotNull(foundUser);
        assertEquals(user.getGuid(), foundUser.getGuid());
        assertEquals(user.getEmail(), foundUser.getEmail());
        assertEquals(user.getCity(), foundUser.getCity());
        assertEquals(user.getEmployeeId(), foundUser.getEmployeeId());

        // update city & employee id
        user.setCity(user.getCity() + " modified");
        user.setEmployeeId(user.getEmployeeId() + " modified");

        this.dao.update(user, User.Attr.LOCATION, User.Attr.EMPLOYEE_NUMBER);

        // check for valid update
        foundUser = this.dao.findByGuid(user.getGuid(), false);
        assertNotNull(foundUser);
        assertEquals(user.getGuid(), foundUser.getGuid());
        assertEquals(user.getEmail(), foundUser.getEmail());
        assertEquals(user.getCity(), foundUser.getCity());
        assertEquals(user.getEmployeeId(), foundUser.getEmployeeId());
    }

    @Test
    public void testSecurityQuestionAnswer() throws Exception {
        assumeConfigured();

        // create a new user
        final User user = getStaffUser();
        this.dao.save(user);

        String securityAnswer = "";

        user.setSecurityQuestion("");
        user.setSecurityAnswer(securityAnswer);

        this.dao.update(user, User.Attr.SECURITYQA);

        User foundUser = this.dao.findByGuid(user.getGuid(), false);
        assertEquals(user.getSecurityQuestion(), foundUser.getSecurityQuestion());
        assertFalse(foundUser.checkSecurityAnswer(securityAnswer));

        user.setSecurityQuestion(null);
        user.setSecurityAnswer(null);
        this.dao.update(user, User.Attr.SECURITYQA);

        foundUser = this.dao.findByGuid(user.getGuid(), false);
        assertEquals(user.getSecurityQuestion(), foundUser.getSecurityQuestion());
        assertFalse(foundUser.checkSecurityAnswer(null));
        assertFalse(foundUser.checkSecurityAnswer("null"));

        securityAnswer = guid();
        user.setSecurityQuestion(guid());
        user.setSecurityAnswer(securityAnswer);

        this.dao.update(user, User.Attr.LOCATION);

        assertNotEquals(user.getSecurityQuestion(), foundUser.getSecurityQuestion());
        assertFalse(foundUser.checkSecurityAnswer(securityAnswer));

        this.dao.update(user, User.Attr.SECURITYQA);

        // check for valid update
        foundUser = this.dao.findByGuid(user.getGuid(), false);
        assertEquals(user.getSecurityQuestion(), foundUser.getSecurityQuestion());
        assertTrue(foundUser.checkSecurityAnswer(securityAnswer));

        // ensure valid check of (non-normalized) literal string hash
        securityAnswer = "   A  b   C    d   E  f       G   h   I   j   K   l   M n O p Q r S t U v W x Y z    ";
        foundUser.setSecurityAnswer(HashUtility.getHash(securityAnswer), false);
        assertTrue(foundUser.checkSecurityAnswer(securityAnswer));
    }

    @Test
    public void testFindByDesignation() throws Exception {
        assumeConfigured();

        // create test user
        final User user = getStaffUser();
        dao.save(user);

        // find user using designation
        final User foundUser = dao.findByDesignation(user.getCruDesignation(), false);
        assertNotNull(foundUser);
        assertEquals(user.getGuid(), foundUser.getGuid());
        assertEquals(user.getCruDesignation(), foundUser.getCruDesignation());
    }

    @Test
    public void testFindByEmployeeId() throws Exception {
        assumeConfigured();

        // create staff user
        final User user = getStaffUser();
        this.dao.save(user);

        // find staff user using employee id
        final User foundUser = this.dao.findByEmployeeId(user.getEmployeeId(), false);
        assertNotNull(foundUser);
        assertEquals(user.getGuid(), foundUser.getGuid());
        assertEquals(user.getEmployeeId(), foundUser.getEmployeeId());
    }

    @Test
    public void testAddToGroupAndRemoveFromGroup() throws Exception {
        assumeConfigured();
        assumeGroupsConfigured();

        final User user = newUser();

        this.dao.save(user);

        // test adding this user to group1
        {
            this.dao.addToGroup(user, group1);
            final User foundUser = this.dao.findByEmail(user.getEmail(), false);
            assertEquals(ImmutableSet.of(group1), foundUser.getGroups());
        }

        // test adding this user to group1
        {
            this.dao.addToGroup(user, group1);
            final User foundUser = this.dao.findByEmail(user.getEmail(), false);
            assertEquals(ImmutableSet.of(group1), foundUser.getGroups());
        }

        // test adding this user to group2
        {
            this.dao.addToGroup(user, group2);
            final User foundUser = this.dao.findByEmail(user.getEmail(), false);
            assertEquals(ImmutableSet.of(group1, group2), foundUser.getGroups());
        }

        // test removing this user from group 1
        {
            this.dao.removeFromGroup(user, group1);
            final User foundUser = this.dao.findByEmail(user.getEmail(), false);
            assertEquals(ImmutableSet.of(group2), foundUser.getGroups());
        }

        // test removing this user from group 1
        {
            this.dao.removeFromGroup(user, group1);
            final User foundUser = this.dao.findByEmail(user.getEmail(), false);
            assertEquals(ImmutableSet.of(group2), foundUser.getGroups());
        }

        // test removing this user from group 2
        {
            this.dao.removeFromGroup(user, group2);
            final User foundUser = this.dao.findByEmail(user.getEmail(), false);
            assertEquals(ImmutableSet.of(), foundUser.getGroups());
        }
    }

    @Test
    public void testFindAllByGroup() throws Exception {
        assumeConfigured();
        assumeGroupsConfigured();

        // create a couple users for testing
        final User user1 = newUser();
        final User user2 = newUser();
        user2.setDeactivated(true);
        this.dao.save(user1);
        this.dao.save(user2);

        // assert the 2 users are not in the group
        {
            final Set<String> guids = FluentIterable.from(this.dao.findAllByGroup(group1, true)).transform
                    (FUNCTION_GUID).toSet();
            assertFalse(guids.contains(user1.getGuid()));
            assertFalse(guids.contains(user2.getGuid()));
        }

        // add user1 to the group
        {
            this.dao.addToGroup(user1, group1);
            final Set<String> guids = FluentIterable.from(this.dao.findAllByGroup(group1, true)).transform
                    (FUNCTION_GUID).toSet();
            assertTrue(guids.contains(user1.getGuid()));
            assertFalse(guids.contains(user2.getGuid()));
        }

        // add user2 to the group
        {
            this.dao.addToGroup(user2, group1);
            final Set<String> guids = FluentIterable.from(this.dao.findAllByGroup(group1, true)).transform
                    (FUNCTION_GUID).toSet();
            assertTrue(guids.contains(user1.getGuid()));
            assertTrue(guids.contains(user2.getGuid()));
        }

        // test includeDeactivated flag
        {
            final Set<String> guids = FluentIterable.from(this.dao.findAllByGroup(group1, false)).transform
                    (FUNCTION_GUID).toSet();
            assertTrue(guids.contains(user1.getGuid()));
            assertFalse(guids.contains(user2.getGuid()));
        }

        // remove the users from the group
        this.dao.removeFromGroup(user1, group1);
        this.dao.removeFromGroup(user2, group1);
    }

    private User getStaffUser() {
        final User user = newUser();

        user.setEmployeeId(guid());
        user.setDepartmentNumber("USDSABC");
        user.setCruDesignation(guid());
        user.setCruGender("M");
        user.setCity("Orlando");
        user.setState("FL");
        user.setPostal("32832");
        Collection<String> cruProxyAddresses = Sets.newHashSet("smtp:test.user@cru.org", "smtp:test.user@ccci.org");
        user.setCruProxyAddresses(cruProxyAddresses);
        Collection<String> cruPasswordHistory = Sets.newHashSet("09a87fa0987sdf7sdf", "897asdf987asdf789asfd");
        user.setCruPasswordHistory(cruPasswordHistory);

        return user;
    }
}
