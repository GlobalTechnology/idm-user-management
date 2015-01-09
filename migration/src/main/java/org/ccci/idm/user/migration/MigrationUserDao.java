package org.ccci.idm.user.migration;

import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.UserDao;

public interface MigrationUserDao extends UserDao {
    void moveLegacyKeyUser(User user);

    void updateGuid(User user);
}
