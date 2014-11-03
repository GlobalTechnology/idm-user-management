package org.ccci.idm.user.ldaptive.dao;

import static org.junit.Assume.assumeNotNull;

import org.ccci.idm.user.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.security.SecureRandom;
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
    public void testCreateStaffUser() throws Exception {
        assumeConfigured();

        final User user = getStaffUser();

        this.dao.save(user);
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

        return user;
    }
}
