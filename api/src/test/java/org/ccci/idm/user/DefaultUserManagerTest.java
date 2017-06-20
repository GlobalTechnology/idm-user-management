package org.ccci.idm.user;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.ccci.idm.user.exception.InvalidEmailUserException;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Nonnull;

@RunWith(JUnitParamsRunner.class)
public class DefaultUserManagerTest {
    @Test
    @Parameters({
            "test@example.com",
            "user@theharbor.life",
            "Dörte@Sörensen.example.com",
            "researcher@goodperson.cancerresearch"
    })
    public void verifyValidateEmailValid(final String email) throws Exception {
        final DefaultUserManager manager = new DefaultUserManager();
        manager.validateEmail(newUser(email));
    }

    @Test(expected = InvalidEmailUserException.class)
    @Parameters({"test@example.asdf", "test@com", "asdf", "email@localhost"})
    public void verifyValidateEmailInvalid(final String email) throws Exception {
        final DefaultUserManager manager = new DefaultUserManager();
        manager.validateEmail(newUser(email));
    }

    @Nonnull
    private static User newUser(@Nonnull final String email) {
        final User user = new User();
        user.setEmail(email);
        return user;
    }
}
