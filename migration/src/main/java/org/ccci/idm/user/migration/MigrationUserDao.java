package org.ccci.idm.user.migration;

import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.dao.UserDao;

import java.util.List;

public interface MigrationUserDao extends UserDao {
    void moveLegacyKeyUser(User user);

    void moveLegacyKeyUser(User user, String newEmail);

    void deactivateAndMoveLegacyKeyUser(User user);

    void updateGuid(User user);

    User findLegacyKeyByTheKeyGuid(String guid, boolean includeDeactivated);

    User findLegacyKeyByEmail(String email, boolean includeDeactivated);

    List<User> findAll(boolean includeDeactivated) throws ExceededMaximumAllowedResultsException;

    List<User> findAllMissingRelayGuid() throws ExceededMaximumAllowedResultsException;

    List<User> findAllLegacyKeyUsers(boolean includeDeactivated);
}
