package org.ccci.idm.user;

import static org.junit.Assume.assumeNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

public abstract class AbstractDefaultUserManagerIT {
    protected static final Random RAND = new SecureRandom();

    @Inject
    @NotNull
    protected DefaultUserManager userManager;

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

    protected void assumeConfigured() throws Exception {
        assumeNotNull(url, base, username, password, dn);
        assumeNotNull(userManager);
    }

    @Test
    @Ignore
    public void testCreateUser() throws Exception {
        assumeConfigured();

        final User user = new User();
        user.setEmail("test.user." + RAND.nextInt(Integer.MAX_VALUE) + "@example.com");
        user.setGuid(UUID.randomUUID().toString().toUpperCase());
        user.setRelayGuid(UUID.randomUUID().toString().toUpperCase());
        user.setPassword("testPassword");
        user.setFirstName("Test");
        user.setLastName("User");

        this.userManager.createUser(user);
    }
}
