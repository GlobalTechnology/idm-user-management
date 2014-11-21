package org.ccci.idm.user.dao;

import org.ccci.idm.user.User;
import org.springframework.util.Assert;

public abstract class AbstractUserDao implements UserDao {
    protected void assertValidUser(final User user) {
        Assert.notNull(user, "No user was provided");
        Assert.hasText(user.getEmail(), "E-mail address cannot be blank.");
        Assert.hasText(user.getTheKeyGuid(), "GUID cannot be blank");
    }
}
