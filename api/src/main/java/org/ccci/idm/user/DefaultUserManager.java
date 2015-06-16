package org.ccci.idm.user;

import com.github.inspektr.audit.annotation.Audit;
import com.google.common.base.CharMatcher;
import org.apache.commons.validator.routines.EmailValidator;
import org.ccci.idm.user.dao.UserDao;
import org.ccci.idm.user.dao.exception.DaoException;
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.exception.EmailAlreadyExistsException;
import org.ccci.idm.user.exception.InvalidEmailUserException;
import org.ccci.idm.user.exception.RelayGuidAlreadyExistsException;
import org.ccci.idm.user.exception.TheKeyGuidAlreadyExistsException;
import org.ccci.idm.user.exception.UserException;
import org.ccci.idm.user.exception.UserNotFoundException;
import org.ccci.idm.user.util.DefaultRandomPasswordGenerator;
import org.ccci.idm.user.util.RandomPasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

public class DefaultUserManager implements UserManager {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserManager.class);

    private static final String AUDIT_ACTION_RESOLVER = "IDM_USER_MANAGER_ACTION_RESOLVER";

    private static final EmailValidator VALIDATOR_EMAIL = EmailValidator.getInstance();

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

    @Override
    public boolean isReadOnly() {
        return this.userDao.isReadOnly();
    }

    protected boolean doesGuidExist(final String guid) {
        return guid != null && this.userDao.findByGuid(guid, true) != null;
    }

    protected boolean doesRelayGuidExist(final String guid) {
        return guid != null && this.userDao.findByRelayGuid(guid, true) != null;
    }

    protected boolean doesTheKeyGuidExist(final String guid) {
        return guid != null && this.userDao.findByTheKeyGuid(guid, true) != null;
    }

    @Override
    public boolean doesEmailExist(final String email) {
        return email != null && this.userDao.findByEmail(email, false) != null;
    }

    @Override
    @Audit(action = "IDM_CREATE_USER", actionResolverName = AUDIT_ACTION_RESOLVER, resourceResolverName =
            "IDM_USER_MANAGER_CREATE_USER_RESOURCE_RESOLVER")
    public void createUser(final User user) throws DaoException, UserException {
        // validate user being created
        this.validateNewUser(user);

        // initialize some default attributes
        this.setNewUserDefaults(user);

        // Save the user
        this.userDao.save(user);
    }

    protected void validateNewUser(final User user) throws UserException {
        // perform base user validation
        this.validateUser(user);

        // throw an error if a user already exists for this email
        if (this.doesEmailExist(user.getEmail())) {
            LOG.debug("The specified email '{}' already exists.", user.getEmail());
            throw new EmailAlreadyExistsException();
        }

        // throw an error if the raw Relay or The Key guid exists already
        if (user.getRawRelayGuid() != null && this.doesRelayGuidExist(user.getRawRelayGuid())) {
            throw new RelayGuidAlreadyExistsException("Relay guid '" + user.getRawRelayGuid() + "' already exists");
        }
        if (user.getRawTheKeyGuid() != null && this.doesTheKeyGuidExist(user.getRawTheKeyGuid())) {
            throw new TheKeyGuidAlreadyExistsException("The Key guid '" + user.getRawTheKeyGuid() + "' already exists");
        }
    }

    protected void setNewUserDefaults(final User user) throws UserException {
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

        // Generate a random password for the new user if one wasn't already set
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(this.randomPasswordGenerator.generatePassword(), true);
        }
    }

    @Override
    @Audit(action = "IDM_UPDATE_USER", actionResolverName = AUDIT_ACTION_RESOLVER, resourceResolverName =
            "IDM_USER_MANAGER_UPDATE_USER_RESOURCE_RESOLVER")
    public void updateUser(final User user, final User.Attr... attrs) throws DaoException, UserException {
        // validate user object before trying to update it
        this.validateUpdateUser(user, attrs);

        // update the user object
        final User original = this.getFreshUser(user);
        this.userDao.update(original, user, attrs);
    }

    protected void validateUpdateUser(final User user, final User.Attr... attrs) throws UserException {
        // perform base user validation
        this.validateUser(user);
    }

    @Override
    @Audit(action = "IDM_DEACTIVATE_USER", actionResolverName = AUDIT_ACTION_RESOLVER, resourceResolverName =
            "IDM_USER_MANAGER_DEACTIVATE_USER_RESOURCE_RESOLVER")
    public void deactivateUser(final User user) throws DaoException, UserException {
        // Create a deep clone copy before proceeding
        final User original = user.clone();

        // Set a few flags to disable the account
        user.setDeactivated(true);
        user.setLoginDisabled(true);

        // remove any federated identities
        user.removeFacebookId(original.getFacebookId());

        // update the user object
        this.userDao.update(original, user, User.Attr.EMAIL, User.Attr.FLAGS, User.Attr.FACEBOOK);
    }

    @Override
    @Audit(action = "IDM_REACTIVATE_USER", actionResolverName = AUDIT_ACTION_RESOLVER, resourceResolverName =
            "IDM_USER_MANAGER_REACTIVATE_USER_RESOURCE_RESOLVER")
    public void reactivateUser(final User user) throws DaoException, UserException {
        // Determine if the user already exists, and can't be reactivated
        if (this.doesEmailExist(user.getEmail())) {
            final String error = "Unable to reactivate user because an account with the email address '" + user
                    .getEmail() + "' currently exists";
            LOG.error(error);
            throw new EmailAlreadyExistsException(error);
        }

        // Create a deep clone copy before proceeding
        final User original = user.clone();

        // Restore several settings on the user object
        user.setDeactivated(false);
        user.setLoginDisabled(false);
        user.setAllowPasswordChange(true);

        // update the user object
        this.userDao.update(original, user, User.Attr.EMAIL, User.Attr.FLAGS);
    }

    @Override
    public User getFreshUser(final User user) throws UserNotFoundException {
        // attempt retrieving the fresh user object using the original users guid
        final User fresh = userDao.findByGuid(user.getGuid(), true);

        // throw an error if the guid wasn't found
        if (fresh == null) {
            throw new UserNotFoundException("Cannot find a fresh instance of the specified user");
        }

        // return the fresh user object
        return fresh;
    }

    @Override
    public User findUserByEmail(final String email) {
        return this.findUserByEmail(email, false);
    }

    @Override
    public User findUserByEmail(final String email, final boolean includeDeactivated) {
        return this.userDao.findByEmail(email, includeDeactivated);
    }

    @Override
    public User findUserByGuid(final String guid) {
        return this.findUserByGuid(guid, true);
    }

    @Override
    public User findUserByGuid(final String guid, final boolean includeDeactivated) {
        return this.userDao.findByGuid(guid, includeDeactivated);
    }

    @Override
    public User findUserByRelayGuid(final String guid) {
        return this.findUserByRelayGuid(guid, true);
    }

    @Override
    public User findUserByRelayGuid(final String guid, final boolean includeDeactivated) {
        return this.userDao.findByRelayGuid(guid, includeDeactivated);
    }

    @Override
    public User findUserByTheKeyGuid(final String guid) {
        return this.findUserByTheKeyGuid(guid, true);
    }

    @Override
    public User findUserByTheKeyGuid(final String guid, final boolean includeDeactivated) {
        return this.userDao.findByTheKeyGuid(guid, includeDeactivated);
    }

    @Override
    public User findUserByFacebookId(final String id) {
        return this.findUserByFacebookId(id, false);
    }

    @Override
    public User findUserByFacebookId(final String id, final boolean includeDeactivated) {
        return this.userDao.findByFacebookId(id, includeDeactivated);
    }

    @Override
    public User findUserByEmployeeId(final String employeeId) {
        return this.findUserByEmployeeId(employeeId, false);
    }

    @Override
    public User findUserByEmployeeId(final String employeeId, final boolean includeDeactivated) {
        return this.userDao.findByEmployeeId(employeeId, includeDeactivated);
    }

    @Override
    public List<User> findAllByFirstName(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByFirstName(pattern, includeDeactivated);
    }

    @Override
    public List<User> findAllByLastName(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByLastName(pattern, includeDeactivated);
    }

    @Override
    public List<User> findAllByEmail(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByEmail(pattern, includeDeactivated);
    }

    @Nonnull
    @Override
    public List<User> findAllByGroup(@Nonnull final Group group, final boolean includeDeactivated) throws DaoException {
        return this.userDao.findAllByGroup(group, includeDeactivated);
    }

    @Override
    public int enqueueAll(@Nonnull final BlockingQueue<User> queue, final boolean includeDeactivated)
            throws DaoException {
        return this.userDao.enqueueAll(queue, includeDeactivated);
    }

    @Override
    public void addToGroup(final User user, final Group group) throws DaoException {
        this.userDao.addToGroup(user, group);
    }

    @Override
    public void removeFromGroup(final User user, final Group group) throws DaoException {
        this.userDao.removeFromGroup(user, group);
    }

    protected void validateUser(final User user) throws UserException {
        // throw an error if we don't have a valid email
        if (!VALIDATOR_EMAIL.isValid(user.getEmail()) || CharMatcher.WHITESPACE.matchesAnyOf(user.getEmail())) {
            throw new InvalidEmailUserException("Invalid email specified for user");
        }
    }
}
