package org.ccci.idm.user.migration;

import org.ccci.idm.user.User;
import org.ccci.idm.user.UserManager;

public interface MigrationUserManager extends UserManager {
    void moveLegacyKeyUser(User user);
}
