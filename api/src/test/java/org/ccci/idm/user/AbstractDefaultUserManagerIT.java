package org.ccci.idm.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

import org.ccci.idm.user.exception.InvalidEmailUserException;
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
    public void testCreateUser() throws Exception {
        assumeConfigured();

        // test simple creation
        {
            final User user = this.getNewUser();
            this.userManager.createUser(user);
            assertTrue(this.userManager.doesEmailExist(user.getEmail()));
        }

        // test an invalid email address
        {
            final User user = this.getNewUser();
            user.setEmail("invalid.email." + RAND.nextInt(Integer.MAX_VALUE));
            try {
                this.userManager.createUser(user);
                fail("no exception for an invalid email");
            } catch(final InvalidEmailUserException expected) {
                // This exception is expected
            }
            assertFalse(this.userManager.doesEmailExist(user.getEmail()));
        }
    }

    @Test
    public void testUpdateUser() throws Exception {
        assumeConfigured();

        // create base user
        final User user = this.getNewUser();
        this.userManager.createUser(user);
        assertTrue(this.userManager.doesEmailExist(user.getEmail()));

        // update email of user
        {
            final String oldEmail = user.getEmail();
            user.setEmail("test.user." + RAND.nextInt(Integer.MAX_VALUE) + "@example.com");
            this.userManager.updateUser(user, User.Attr.EMAIL);

            assertFalse(this.userManager.doesEmailExist(oldEmail));
            assertTrue(this.userManager.doesEmailExist(user.getEmail()));
        }

        // update to invalid email
        {
            final String oldEmail = user.getEmail();
            user.setEmail("invalid.email." + RAND.nextInt(Integer.MAX_VALUE));

            try {
                this.userManager.updateUser(user, User.Attr.EMAIL);
                fail("no error when updating to invalid email");
            } catch(final InvalidEmailUserException expected) {
                // This exception is expected
            }

            assertTrue(this.userManager.doesEmailExist(oldEmail));
            assertFalse(this.userManager.doesEmailExist(user.getEmail()));
        }
    }

    private User getNewUser() {
        final User user = new User();
        user.setEmail("test.user." + RAND.nextInt(Integer.MAX_VALUE) + "@example.com");
        user.setGuid(UUID.randomUUID().toString().toUpperCase());
        user.setRelayGuid(UUID.randomUUID().toString().toUpperCase());
        user.setPassword("testPassword");
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }
}
