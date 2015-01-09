package org.ccci.idm.user.migration;

import org.ccci.idm.user.User;
import org.ccci.idm.user.UserManager;
import org.ccci.idm.user.exception.UserException;

public interface MigrationUserManager extends UserManager {
    void moveLegacyKeyUser(User user);

    void generateNewGuid(final User user) throws UserException;
}
