package org.ccci.idm.user.migration;

import org.ccci.idm.user.DefaultUserManager;
import org.ccci.idm.user.User;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

public class DefaultMigrationUserManager extends DefaultUserManager implements MigrationUserManager {
    @Inject
    @NotNull
    protected MigrationUserDao migrationUserDao;

    public void setMigrationUserDao(final MigrationUserDao migrationUserDao) {
        this.migrationUserDao = migrationUserDao;
    }

    @Override
    public void moveLegacyKeyUser(final User user) {
        this.migrationUserDao.moveLegacyKeyUser(user);
    }
}
