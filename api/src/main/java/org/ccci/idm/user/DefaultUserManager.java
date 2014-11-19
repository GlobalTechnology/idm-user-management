package org.ccci.idm.user;

import com.github.inspektr.audit.annotation.Audit;
import org.ccci.idm.user.dao.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.dao.UserDao;
import org.ccci.idm.user.exception.RelayGuidAlreadyExistsException;
import org.ccci.idm.user.exception.TheKeyGuidAlreadyExistsException;
import org.ccci.idm.user.exception.UserAlreadyExistsException;
import org.ccci.idm.user.exception.UserException;
import org.ccci.idm.user.exception.UserNotFoundException;
import org.ccci.idm.user.util.DefaultRandomPasswordGenerator;
import org.ccci.idm.user.util.RandomPasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
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
            throw new RelayGuidAlreadyExistsException("Relay guid '" + user.getRawRelayGuid() + "' already exists");
        }
        if (user.getRawTheKeyGuid() != null && this.doesTheKeyGuidExist(user.getRawTheKeyGuid())) {
            throw new TheKeyGuidAlreadyExistsException("The Key guid '" + user.getRawTheKeyGuid() + "' already exists");
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

    @Override
    @Transactional(readOnly = false)
    @Audit(applicationCode = AUDIT_APPLICATION_CODE, action = "UPDATE_USER", actionResolverName = AUDIT_ACTION_RESOLVER,
            resourceResolverName = "IDM_USER_MANAGER_UPDATE_USER_RESOURCE_RESOLVER")
    public void updateUser(final User user, final User.Attr... attrs) throws UserNotFoundException {
        final User original = this.getFreshUser(user);
        this.userDao.update(original, user, attrs);
    }

    @Override
    @Transactional(readOnly = false)
    @Audit(applicationCode = AUDIT_APPLICATION_CODE, action = "DEACTIVATE_USER",
            actionResolverName = AUDIT_ACTION_RESOLVER, resourceResolverName =
            "IDM_USER_MANAGER_DEACTIVATE_USER_RESOURCE_RESOLVER")
    public void deactivateUser(final User user) throws UserException {
        // Create a deep clone copy before proceeding
        final User original = user.clone();

        // Set a few flags to disable the account
        user.setDeactivated(true);
        user.setLoginDisabled(true);

        // remove any federated identities
        user.removeFacebookId(original.getFacebookId());

        // update the user object
        this.userDao.update(original, user);
    }

    @Override
    @Transactional(readOnly = false)
    @Audit(applicationCode = AUDIT_APPLICATION_CODE, action = "REACTIVATE_USER",
            actionResolverName = AUDIT_ACTION_RESOLVER, resourceResolverName =
            "IDM_USER_MANAGER_REACTIVATE_USER_RESOURCE_RESOLVER")
    public void reactivateUser(final User user) throws UserException {
        // Determine if the user already exists, and can't be reactivated
        if (this.doesEmailExist(user.getEmail())) {
            final String error = "Unable to reactivate user because an account with the email address '" + user
                    .getEmail() + "' currently exists";
            LOG.error(error);
            throw new UserAlreadyExistsException(error);
        }

        // Create a deep clone copy before proceeding
        final User original = user.clone();

        // Restore several settings on the user object
        user.setDeactivated(false);
        user.setLoginDisabled(false);
        user.setAllowPasswordChange(true);

        // update the user object
        this.userDao.update(original, user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getFreshUser(final User user) throws UserNotFoundException {
        // attempt retrieving the fresh user object using the original users guid
        final User fresh = userDao.findByGuid(user.getGuid());

        // throw an error if the guid wasn't found
        if (fresh == null) {
            throw new UserNotFoundException("Cannot find a fresh instance of the specified user");
        }

        // return the fresh user object
        return fresh;
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByEmail(final String email, final boolean includeDeactivated) {
        //TODO: implement includeDeactivated functionality
        return this.userDao.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByGuid(final String guid) {
        return this.userDao.findByGuid(guid);
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByRelayGuid(final String guid) {
        return this.userDao.findByRelayGuid(guid);
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByTheKeyGuid(final String guid) {
        return this.userDao.findByTheKeyGuid(guid);
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByFacebookId(final String id) {
        return this.userDao.findByFacebookId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByEmployeeId(final String employeeId) {
        return this.userDao.findByEmployeeId(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllByFirstName(final String pattern) throws ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByFirstName(pattern);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllByLastName(final String pattern) throws ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByLastName(pattern);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllByEmail(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByEmail(pattern, includeDeactivated);
    }
}
