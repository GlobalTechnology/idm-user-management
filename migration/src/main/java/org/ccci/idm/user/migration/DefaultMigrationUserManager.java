package org.ccci.idm.user.migration;

import org.ccci.idm.user.DefaultUserManager;
import org.ccci.idm.user.User;
import org.ccci.idm.user.exception.UserException;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DefaultMigrationUserManager extends DefaultUserManager implements MigrationUserManager {
    @Inject
    @NotNull
    protected MigrationUserDao userDao;

    public void setUserDao(final MigrationUserDao userDao) {
        super.setUserDao(userDao);
        this.userDao = userDao;
    }

    @Override
    public void moveLegacyKeyUser(final User user) {
        this.userDao.moveLegacyKeyUser(user);
    }

    @Override
    public void generateNewGuid(final User user) throws UserException {
        // generate a guid for the user if there isn't a valid one already set
        int count = 0;
        String guid = user.getGuid();
        while (!StringUtils.hasText(guid) || this.doesGuidExist(guid) || this.doesRelayGuidExist(guid) || this
                .doesTheKeyGuidExist(guid)) {
            guid = UUID.randomUUID().toString().toUpperCase(Locale.US);
            user.setGuid(guid);

            // prevent an infinite loop, I doubt this exception will ever be thrown
            if (count++ > 200) {
                throw new UserException("Unable to create a guid for the new user");
            }
        }

        // update the guid for the user
        this.userDao.updateGuid(user);
    }

    @Override
    public User findLegacyKeyUserByTheKeyGuid(final String guid) {
        return this.findLegacyKeyUserByTheKeyGuid(guid, true);
    }

    @Override
    public User findLegacyKeyUserByTheKeyGuid(final String guid, final boolean includeDeactivated) {
        return this.userDao.findLegacyKeyByTheKeyGuid(guid, includeDeactivated);
    }

    @Override
    public User findLegacyKeyUserByEmail(final String email) {
        return this.findLegacyKeyUserByEmail(email, false);
    }

    @Override
    public User findLegacyKeyUserByEmail(final String email, final boolean includeDeactivated) {
        return this.userDao.findLegacyKeyByEmail(email, includeDeactivated);
    }

    @Override
    public List<User> getAllLegacyUsers(final boolean includeDeactivated) {
        return this.userDao.findAllLegacyKeyUsers(includeDeactivated);
    }
}
