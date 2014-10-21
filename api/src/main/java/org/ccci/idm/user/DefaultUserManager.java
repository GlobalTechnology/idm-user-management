package org.ccci.idm.user;

import com.github.inspektr.audit.annotation.Audit;
import org.ccci.idm.user.dao.UserDao;
import org.ccci.idm.user.util.DefaultRandomPasswordGenerator;
import org.ccci.idm.user.util.RandomPasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Locale;
import java.util.UUID;

public class DefaultUserManager implements UserManager {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserManager.class);

    private static final String AUDIT_APPLICATION_CODE = "IDM";
    private static final String AUDIT_ACTION_RESOLVER = "IDM_USER_MANAGER_ACTION_RESOLVER";

    @NotNull
    protected RandomPasswordGenerator randomPasswordGenerator = new DefaultRandomPasswordGenerator();

    @Inject
    @NotNull
    protected UserDao userDao;

    public void setRandomPasswordGenerator(final RandomPasswordGenerator randomPasswordGenerator) {
        this.randomPasswordGenerator = randomPasswordGenerator;
    }

    public void setUserDao(final UserDao dao) {
        this.userDao = dao;
    }

    protected boolean doesGuidExist(final String guid) {
        return guid != null && this.userDao.findByGuid(guid) != null;
    }

    protected boolean doesRelayGuidExist(final String guid) {
        return guid != null && this.userDao.findByRelayGuid(guid) != null;
    }

    protected boolean doesTheKeyGuidExist(final String guid) {
        return guid != null && this.userDao.findByTheKeyGuid(guid) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean doesEmailExist(final String email) {
        return email != null && this.userDao.findByEmail(email) != null;
    }

    @Override
    @Transactional(readOnly = false)
    @Audit(applicationCode = AUDIT_APPLICATION_CODE, action = "CREATE_USER", actionResolverName = AUDIT_ACTION_RESOLVER,
            resourceResolverName = "IDM_USER_MANAGER_CREATE_USER_RESOURCE_RESOLVER")
    public void createUser(final User user) throws UserException {
        // validate user being created
        this.validateNewUser(user);

        // initialize some default attributes
        this.setNewUserDefaults(user);

        // Save the user
        this.userDao.save(user);
    }

    protected void validateNewUser(final User user) throws UserException {
        // throw an error if we don't have a valid email
        if (!StringUtils.hasText(user.getEmail())) {
            throw new UserException("Invalid email specified for creating a user");
        }

        // throw an error if a user already exists for this email
        if (this.doesEmailExist(user.getEmail())) {
            LOG.debug("The specified user '{}' already exists.", user.getEmail());
            throw new UserAlreadyExistsException();
        }

        // throw an error if the raw Relay or The Key guid exists already
        if (user.getRawRelayGuid() != null && this.doesRelayGuidExist(user.getRawRelayGuid())) {
            throw new UserException("Relay guid '" + user.getRawRelayGuid() + "' already exists");
        }
        if (user.getRawTheKeyGuid() != null && this.doesRelayGuidExist(user.getRawTheKeyGuid())) {
            throw new UserException("The Key guid '" + user.getRawTheKeyGuid() + "' already exists");
        }

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
    }

    protected void setNewUserDefaults(final User user) {
        user.setEmailVerified(false);

        // Generate a random password for the new user if one wasn't already set
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(this.randomPasswordGenerator.generatePassword());
            user.setForcePasswordChange(true);
        }
    }
}
