package org.ccci.idm.user.migration;

import org.ccci.idm.user.DefaultUserManager;
import org.ccci.idm.user.User;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

public class DefaultMigrationUserManager extends DefaultUserManager implements MigrationUserManager {
    @Inject
    @NotNull
    protected MigrationUserDao userDao;

    public void setUserDao(final MigrationUserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void moveLegacyKeyUser(final User user) {
        this.userDao.moveLegacyKeyUser(user);
    }
}
