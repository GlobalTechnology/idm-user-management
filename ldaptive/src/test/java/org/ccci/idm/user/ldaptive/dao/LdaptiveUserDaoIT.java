package org.ccci.idm.user.ldaptive.dao;

import static org.junit.Assume.assumeNotNull;

import org.ccci.idm.user.User;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"ldap.xml", "config.xml", "dao-default.xml"})
public class LdaptiveUserDaoIT {
    @Inject
    private ApplicationContext applicationContext;

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
        assumeNotNull(applicationContext, dao);
    }

    @Test
    @Ignore
    public void testCreate() throws Exception {
        assumeConfigured();

        final User user = new User();
        user.setEmail("test.user@example.com");
        user.setGuid(UUID.randomUUID().toString().toUpperCase());
        user.setFirstName("Test");
        user.setLastName("User");

        this.dao.save(user);
    }
}
