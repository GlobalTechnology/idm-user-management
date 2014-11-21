package org.ccci.idm.user.ldaptive.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import com.google.common.collect.Sets;
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
import java.util.Collection;
import java.util.Random;
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

        final User foundUser = this.dao.findByEmail(user.getEmail());

        Assert.assertTrue(user.equals(foundUser));
    }

    @Test
    public void testUpdateUser() throws Exception {
        assumeConfigured();

        User user = getUser();

        this.dao.save(user);

        user.setFirstName(user.getFirstName() + "modified");

        this.dao.update(user, User.Attr.NAME);

        final User foundUser = this.dao.findByEmail(user.getEmail());

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
        final User saved1 = this.dao.findByGuid(guid);
        assertTrue(saved1.getLoginTime().isEqual(user.getLoginTime()));

        // update the login time to now
        saved1.setLoginTime(new DateTime().secondOfMinute().roundFloorCopy());
        this.dao.update(saved1, User.Attr.LOGINTIME);

        // check to see if the update succeeded
        final User saved2 = this.dao.findByGuid(guid);
        assertTrue(saved2.getLoginTime().isEqual(saved1.getLoginTime()));
        assertFalse(saved2.getLoginTime().isEqual(user.getLoginTime()));
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

        User foundUser = this.dao.findByEmail(user.getEmail());

        Assert.assertTrue(user.equals(foundUser));

        user.setCity(user.getCity() + "modified");
        user.setEmployeeId(user.getEmployeeId() + "modified");

        this.dao.update(user, User.Attr.LOCATION, User.Attr.CRU_PERSON);

        foundUser = this.dao.findByEmail(user.getEmail());

        Assert.assertTrue(user.equals(foundUser));
    }

    @Test
    public void testFindUserByEmployeeId() throws Exception {
        assumeConfigured();

        final User user = getStaffUser();

        this.dao.save(user);

        final User foundUser = this.dao.findByEmployeeId(user.getEmployeeId());

        Assert.assertNotNull(foundUser);

        Assert.assertTrue(user.equals(foundUser));
    }

    private User getUser()
    {
        final User user = new User();
        user.setEmail("test.user." + RAND.nextInt(Integer.MAX_VALUE) + "@example.com");
        user.setGuid(UUID.randomUUID().toString().toUpperCase());
        user.setFirstName("Test");
        user.setLastName("User");

        return user;
    }

    private User getStaffUser()
    {
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
