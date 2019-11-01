package org.ccci.idm.user.dao;

import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.exception.ReadOnlyDaoException;
import org.springframework.util.Assert;

public abstract class AbstractUserDao implements UserDao {
    private boolean readOnly = false;

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly;
    }

    protected void assertWritable() throws ReadOnlyDaoException {
        if (this.readOnly) {
            throw new ReadOnlyDaoException();
        }
    }

    protected void assertValidUser(final User user) {
        Assert.notNull(user, "No user was provided");
        Assert.hasText(user.getEmail(), "E-mail address cannot be blank.");
        Assert.hasText(user.getTheKeyGuid(), "GUID cannot be blank");
        Assert.hasText(user.getRelayGuid(), "GUID cannot be blank");
    }
}
