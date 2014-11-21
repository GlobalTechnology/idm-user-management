package org.ccci.idm.user.ldaptive.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"ldap.xml", "config.xml", "dao-default.xml"})
public class LdaptiveUserDaoIT {
    private static final Random RAND = new SecureRandom();

    @Inject
    private LdaptiveUserDao dao;

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

    private void assumeConfigured() throws Exception {
        assumeNotNull(url, base, username, password, dn);
        assumeNotNull(dao);
    }

    @Test
    public void testCreateUser() throws Exception {
        assumeConfigured();

        final User user = getUser();

        this.dao.save(user);
    }

    @Test
    public void testFindUser() throws Exception {
        assumeConfigured();

        final User user = getUser();

        this.dao.save(user);

        final User foundUser = this.dao.findByEmail(user.getEmail(), false);

        Assert.assertTrue(user.equals(foundUser));
    }

    @Test
    public void testUpdateUser() throws Exception {
        assumeConfigured();

        User user = getUser();

        this.dao.save(user);

        user.setFirstName(user.getFirstName() + "modified");

        this.dao.update(user, User.Attr.NAME);

        final User foundUser = this.dao.findByEmail(user.getEmail(), false);

        Assert.assertTrue(user.equals(foundUser));
    }

    @Test
    public void testLoginDate() throws Exception {
        assumeConfigured();

        final User user = getUser();
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
            final List<User> active = this.dao.findAllByFirstName(user1.getFirstName(), false);
            final List<User> all = this.dao.findAllByFirstName(user1.getFirstName(), true);

            assertEquals(1, active.size());
            assertEquals(2, all.size());

            assertTrue(active.contains(user1));
            assertFalse(active.contains(user2));
            assertTrue(all.contains(user1));
            assertTrue(all.contains(user2));
        }

        // test findAllByLastName
        {
            final List<User> active = this.dao.findAllByLastName(user1.getLastName(), false);
            final List<User> all = this.dao.findAllByLastName(user1.getLastName(), true);

            assertEquals(1, active.size());
            assertEquals(2, all.size());

            assertTrue(active.contains(user1));
            assertFalse(active.contains(user2));
            assertTrue(all.contains(user1));
            assertTrue(all.contains(user2));
        }

        // test findAllByEmail
        {
            final List<User> active = this.dao.findAllByEmail(user1.getEmail(), false);
            final List<User> all = this.dao.findAllByEmail(user1.getEmail(), true);

            assertEquals(1, active.size());
            assertEquals(2, all.size());

            assertTrue(active.contains(user1));
            assertFalse(active.contains(user2));
            assertTrue(all.contains(user1));
            assertTrue(all.contains(user2));
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

            assertEquals(user3, anyUser);
        }
    }

    @Test
    public void testCreateStaffUser() throws Exception {
        assumeConfigured();

        final User user = getStaffUser();

        this.dao.save(user);
    }

    @Test
    public void testUpdateStaffUser() throws Exception {
        assumeConfigured();

        final User user = getStaffUser();

        this.dao.save(user);

        User foundUser = this.dao.findByEmail(user.getEmail(), false);

        Assert.assertTrue(user.equals(foundUser));

        user.setCity(user.getCity() + "modified");
        user.setEmployeeId(user.getEmployeeId() + "modified");

        this.dao.update(user, User.Attr.LOCATION, User.Attr.CRU_PERSON);

        foundUser = this.dao.findByEmail(user.getEmail(), false);

        Assert.assertTrue(user.equals(foundUser));
    }

    @Test
    public void testFindUserByEmployeeId() throws Exception {
        assumeConfigured();

        final User user = getStaffUser();

        this.dao.save(user);

        final User foundUser = this.dao.findByEmployeeId(user.getEmployeeId(), false);

        Assert.assertNotNull(foundUser);

        Assert.assertTrue(user.equals(foundUser));
    }

    @Test
    public void testAddToGroup() throws Exception {
        assumeConfigured();

        final User user = getUser();

        this.dao.save(user);

        List<String[]> paths = Arrays.asList(
                new String[] {"GoogleApps", "Cru", "Cru"},
                new String[] {"GoogleApps", "Cru", "AIA"}
        );
        String name = "Mail";

        List<Group> groups = Lists.newArrayList();
        for(String[] path : paths) {
            Group group = new Group(path, name);
            groups.add(group);
            this.dao.addToGroup(user, group);
        }

        final User foundUser = this.dao.findByEmail(user.getEmail(), false);

        final Set<Group> foundUserGroups = Sets.newHashSet(foundUser.getGroups());

        Collection<Group> empty = Sets.newHashSet();
        foundUser.setGroups(empty);
        Assert.assertEquals(user, foundUser);

        Assert.assertTrue(foundUserGroups.size() == groups.size());

        for(Group foundGroup : foundUserGroups) {
            Boolean match = false;
            for(Group group : groups) {
                if(group.equals(foundGroup)) {
                    match = true;
                    break;
                }
            }
            Assert.assertTrue(match);
        }
    }

    @Test
    public void testRemoveFromGroup() throws Exception {
        assumeConfigured();

        final User user = getUser();

        this.dao.save(user);

        List<String[]> paths = Arrays.asList(
                new String[] {"GoogleApps", "Cru", "Cru"},
                new String[] {"GoogleApps", "Cru", "AIA"}
        );
        String name = "Mail";

        List<Group> groups = Lists.newArrayList();
        for(String[] path : paths) {
            Group group = new Group(path, name);
            groups.add(group);
            this.dao.addToGroup(user, group);
        }

        for(Group group : groups) {
            this.dao.removeFromGroup(user, group);
        }

        final User foundUser = this.dao.findByEmail(user.getEmail(), false);

        Assert.assertEquals(user, foundUser);

        Assert.assertTrue(foundUser.getGroups().size() == 0);
    }

    private User getUser() {
        final User user = new User();
        user.setEmail("test.user." + RAND.nextInt(Integer.MAX_VALUE) + "@example.com");
        user.setTheKeyGuid(UUID.randomUUID().toString().toUpperCase());
        user.setFirstName("Test");
        user.setLastName("User");

        return user;
    }

    private User getStaffUser() {
        final User user = getUser();

        user.setEmployeeId("000123457");
        user.setDepartmentNumber("USDSABC");
        user.setCruDesignation("123457");
        user.setCruGender("M");
        user.setCity("Orlando");
        user.setState("FL");
        user.setPostal("32832");
        Collection<String> collection = Sets.newHashSet("smtp:test.user@cru.org", "smtp:test.user@ccci.org");
        user.setCruProxyAddresses(collection);

        return user;
    }
}
