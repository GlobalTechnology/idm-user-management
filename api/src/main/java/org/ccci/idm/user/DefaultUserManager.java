package org.ccci.idm.user;

import static org.ccci.idm.user.Constants.AUDIT_ACTION_ADD_TO_GROUP;
import static org.ccci.idm.user.Constants.AUDIT_ACTION_CREATE_USER;
import static org.ccci.idm.user.Constants.AUDIT_ACTION_DEACTIVATE_USER;
import static org.ccci.idm.user.Constants.AUDIT_ACTION_MFA_RESET_INTRUDER;
import static org.ccci.idm.user.Constants.AUDIT_ACTION_MFA_TRACK_FAILED_LOGIN;
import static org.ccci.idm.user.Constants.AUDIT_ACTION_REACTIVATE_USER;
import static org.ccci.idm.user.Constants.AUDIT_ACTION_REMOVE_FROM_GROUP;
import static org.ccci.idm.user.Constants.AUDIT_ACTION_RESOLVER_USER_MANAGER;
import static org.ccci.idm.user.Constants.AUDIT_ACTION_UPDATE_USER;
import static org.ccci.idm.user.Constants.AUDIT_RESOURCE_RESOLVER_ADD_TO_GROUP;
import static org.ccci.idm.user.Constants.AUDIT_RESOURCE_RESOLVER_CREATE_USER;
import static org.ccci.idm.user.Constants.AUDIT_RESOURCE_RESOLVER_DEACTIVATE_USER;
import static org.ccci.idm.user.Constants.AUDIT_RESOURCE_RESOLVER_MFA_RESET_INTRUDER;
import static org.ccci.idm.user.Constants.AUDIT_RESOURCE_RESOLVER_MFA_TRACK_FAILED_LOGIN;
import static org.ccci.idm.user.Constants.AUDIT_RESOURCE_RESOLVER_REACTIVATE_USER;
import static org.ccci.idm.user.Constants.AUDIT_RESOURCE_RESOLVER_REMOVE_FROM_GROUP;
import static org.ccci.idm.user.Constants.AUDIT_RESOURCE_RESOLVER_UPDATE_USER;

import com.google.common.annotations.Beta;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.commons.validator.routines.EmailValidator;
import org.apereo.inspektr.audit.annotation.Audit;
import org.ccci.idm.user.dao.UserDao;
import org.ccci.idm.user.dao.exception.DaoException;
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.exception.EmailAlreadyExistsException;
import org.ccci.idm.user.exception.InvalidEmailUserException;
import org.ccci.idm.user.exception.InvalidUsDesignationUserException;
import org.ccci.idm.user.exception.InvalidUsEmployeeIdUserException;
import org.ccci.idm.user.exception.RelayGuidAlreadyExistsException;
import org.ccci.idm.user.exception.TheKeyGuidAlreadyExistsException;
import org.ccci.idm.user.exception.UserException;
import org.ccci.idm.user.exception.UserNotFoundException;
import org.ccci.idm.user.query.Expression;
import org.ccci.idm.user.util.DefaultRandomPasswordGenerator;
import org.ccci.idm.user.util.PasswordHistoryManager;
import org.ccci.idm.user.util.RandomPasswordGenerator;
import org.ccci.idm.user.util.UserUtil;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Period;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadableInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

public class DefaultUserManager implements UserManager {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserManager.class);

    private static final EmailValidator VALIDATOR_EMAIL = EmailValidator.getInstance();

    private int mfaIntruderAttempts = 10;
    @Nonnull
    private ReadableDuration mfaIntruderResetInterval = Duration.standardMinutes(10);
    @Nonnull
    private ReadableDuration mfaIntruderLockDuration = Duration.standardMinutes(15);

    @NotNull
    @Autowired(required = false)
    private List<? extends UserManagerListener> listeners = ImmutableList.of();

    @NotNull
    protected RandomPasswordGenerator randomPasswordGenerator = new DefaultRandomPasswordGenerator();

    @NotNull
    private PasswordHistoryManager passwordHistoryManager = new PasswordHistoryManager();

    @Inject
    @NotNull
    protected UserDao userDao;

    public void setMfaIntruderAttempts(final int attempts) {
        mfaIntruderAttempts = attempts;
    }

    public void setMfaIntruderResetInterval(@Nonnull final String period) {
        setMfaIntruderResetInterval(Period.parse(period).toStandardDuration());
    }

    public void setMfaIntruderResetInterval(@Nonnull final ReadableDuration interval) {
        mfaIntruderResetInterval = interval;
    }

    public void setMfaIntruderLockDuration(@Nonnull final String period) {
        setMfaIntruderLockDuration(Period.parse(period).toStandardDuration());
    }

    public void setMfaIntruderLockDuration(@Nonnull final ReadableDuration duration) {
        mfaIntruderLockDuration = duration;
    }

    public void setListeners(@Nonnull final List<? extends UserManagerListener> listeners) {
        this.listeners = listeners;
    }

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

    @Deprecated
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
    @Audit(action = AUDIT_ACTION_CREATE_USER, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_CREATE_USER)
    public void createUser(final User user) throws DaoException, UserException {
        // validate user being created
        this.validateNewUser(user);

        // initialize some default attributes
        this.setNewUserDefaults(user);

        // add password to history
        user.setCruPasswordHistory(passwordHistoryManager.add(user.getPassword(), user.getCruPasswordHistory()));

        // Save the user
        this.userDao.save(user);

        // trigger any post create listeners
        for (final UserManagerListener listener : listeners) {
            listener.onPostCreateUser(user);
        }
    }

    protected void validateNewUser(final User user) throws UserException {
        // perform user validation
        validateUser(user);
        validateEmail(user);
        validateUsDesignation(user);
        validateUsEmployeeId(user);

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
        while (!UserUtil.isValidGuid(guid) || doesGuidExist(guid) || doesRelayGuidExist(guid) ||
                doesTheKeyGuidExist(guid)) {
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
    @Audit(action = AUDIT_ACTION_UPDATE_USER, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_UPDATE_USER)
    public void updateUser(final User user, final User.Attr... attrs) throws DaoException, UserException {
        // validate user object before trying to update it
        this.validateUpdateUser(user, attrs);

        // trigger any pre update listeners
        final User original = this.getFreshUser(user);
        for (final UserManagerListener listener : listeners) {
            listener.onPreUpdateUser(original, user, attrs);
        }

        // add password to history (if you have password and caller intends to set)
        if (StringUtils.hasText(user.getPassword()) && ImmutableList.copyOf(attrs).contains(User.Attr.PASSWORD)) {
            user.setCruPasswordHistory(passwordHistoryManager.add(user.getPassword(), original.getCruPasswordHistory()));
        }

        // update the user object
        this.userDao.update(original, user, attrs);

        // trigger any post update listeners
        for (final UserManagerListener listener : listeners) {
            listener.onPostUpdateUser(original, user, attrs);
        }
    }

    protected void validateUpdateUser(final User user, final User.Attr... attrs) throws UserException {
        // perform base user validation
        validateUser(user);

        // validate user based on attributes being updated
        final List<User.Attr> attrsList = ImmutableList.copyOf(attrs);
        if (attrsList.contains(User.Attr.EMAIL)) {
            validateEmail(user);
        }
        if (attrsList.contains(User.Attr.CRU_DESIGNATION)) {
            validateUsDesignation(user);
        }
        if (attrsList.contains(User.Attr.EMPLOYEE_NUMBER)) {
            validateUsEmployeeId(user);
        }
    }

    @Override
    @Audit(action = AUDIT_ACTION_DEACTIVATE_USER, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_DEACTIVATE_USER)
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

        // trigger any post-deactivate listeners
        for (final UserManagerListener listener : listeners) {
            listener.onPostDeactivateUser(user);
        }
    }

    @Override
    @Audit(action = AUDIT_ACTION_REACTIVATE_USER, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_REACTIVATE_USER)
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

        // trigger any post reactivate listeners
        for (final UserManagerListener listener : listeners) {
            listener.onPostReactivateUser(user);
        }
    }

    @Override
    public boolean isMfaIntruderLocked(@Nonnull final User user) {
        final ReadableInstant resetTime = user.getMfaIntruderResetTime();
        return user.isMfaIntruderLocked() && resetTime != null && resetTime.isAfter(Instant.now());
    }

    @Override
    @Audit(action = AUDIT_ACTION_MFA_TRACK_FAILED_LOGIN, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_MFA_TRACK_FAILED_LOGIN)
    public void trackFailedMfaLogin(@Nonnull final User user) throws DaoException, UserException {
        // short-circuit if the user is already locked
        if (isMfaIntruderLocked(user)) {
            return;
        }

        // reset intruder state if we have passed the reset time
        final Instant now = Instant.now();
        final ReadableInstant resetTime = user.getMfaIntruderResetTime();
        if (resetTime != null && resetTime.isBefore(now)) {
            clearMfaIntruderState(user);
        }

        // update intruder detection failed login attempts
        final int attempts = MoreObjects.firstNonNull(user.getMfaIntruderAttempts(), 0) + 1;
        user.setMfaIntruderAttempts(attempts);
        if (user.getMfaIntruderResetTime() == null) {
            user.setMfaIntruderResetTime(now.plus(mfaIntruderResetInterval));
        }

        // should we lock the user?
        if (attempts >= mfaIntruderAttempts) {
            user.setMfaIntruderResetTime(now.plus(mfaIntruderLockDuration));
            user.setMfaIntruderLocked(true);
        }

        // update the user model
        updateUser(user, User.Attr.MFA_INTRUDER_DETECTION);
    }

    @Override
    @Audit(action = AUDIT_ACTION_MFA_RESET_INTRUDER, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_MFA_RESET_INTRUDER)
    public void resetMfaIntruderLock(@Nonnull final User user) throws DaoException, UserException {
        final boolean updated = clearMfaIntruderState(user);
        if (updated) {
            updateUser(user, User.Attr.MFA_INTRUDER_DETECTION);
        }
    }

    private boolean clearMfaIntruderState(@Nonnull final User user) {
        boolean changed = user.setMfaIntruderAttempts(null);
        changed = user.setMfaIntruderLocked(false) || changed;
        changed = user.setMfaIntruderResetTime(null) || changed;
        return changed;
    }

    @Nonnull
    @Override
    public User getFreshUser(@Nonnull final User user) throws UserNotFoundException {
        // attempt retrieving the fresh user object using the original users guid
        final User fresh = userDao.findByTheKeyGuid(user.getTheKeyGuid(), true);

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
    @Deprecated
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

    @Nullable
    @Override
    public User findUserByDesignation(@Nullable final String designation) {
        return findUserByDesignation(designation, false);
    }

    @Nullable
    @Override
    public User findUserByDesignation(@Nullable final String designation, final boolean includeDeactivated) {
        return userDao.findByDesignation(designation, includeDeactivated);
    }

    @Override
    public User findUserByEmployeeId(final String employeeId) {
        return this.findUserByEmployeeId(employeeId, false);
    }

    @Override
    public User findUserByEmployeeId(final String employeeId, final boolean includeDeactivated) {
        return this.userDao.findByEmployeeId(employeeId, includeDeactivated);
    }

    @Beta
    @Nonnull
    @Override
    @Deprecated
    public List<User> findAllByQuery(@Nonnull final SearchQuery query) throws DaoException {
        return this.userDao.findAllByQuery(query);
    }

    @Override
    @Deprecated
    public List<User> findAllByFirstName(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByFirstName(pattern, includeDeactivated);
    }

    @Override
    @Deprecated
    public List<User> findAllByLastName(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByLastName(pattern, includeDeactivated);
    }

    @Override
    @Deprecated
    public List<User> findAllByEmail(final String pattern, final boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return this.userDao.findAllByEmail(pattern, includeDeactivated);
    }

    @Nonnull
    @Override
    @Deprecated
    public List<User> findAllByGroup(@Nonnull final Group group, final boolean includeDeactivated) throws DaoException {
        return this.userDao.findAllByGroup(group, includeDeactivated);
    }

    @Override
    @Deprecated
    public int enqueueAll(@Nonnull final BlockingQueue<User> queue, final boolean includeDeactivated)
            throws DaoException {
        return this.userDao.enqueueAll(queue, includeDeactivated);
    }

    @Override
    public Stream<User> streamUsers(@Nullable final Expression expression, final boolean includeDeactivated) {
        return userDao.streamUsers(expression, includeDeactivated);
    }

    @Override
    public Stream<User> streamUsers(@Nullable final Expression expression, final boolean includeDeactivated,
                                    final boolean restrictMaxAllowed) {
        return userDao.streamUsers(expression, includeDeactivated, restrictMaxAllowed);
    }

    @Override
    @Audit(action = AUDIT_ACTION_ADD_TO_GROUP, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_ADD_TO_GROUP)
    public void addToGroup(@Nonnull final User user, @Nonnull final Group group) throws DaoException {
        this.userDao.addToGroup(user, group);
    }

    @Override
    @Audit(action = AUDIT_ACTION_ADD_TO_GROUP, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_ADD_TO_GROUP)
    public void addToGroup(@Nonnull final User user, @Nonnull final Group group, final boolean addSecurity)
            throws DaoException {
        userDao.addToGroup(user, group, addSecurity);
    }

    @Override
    @Audit(action = AUDIT_ACTION_REMOVE_FROM_GROUP, actionResolverName = AUDIT_ACTION_RESOLVER_USER_MANAGER,
            resourceResolverName = AUDIT_RESOURCE_RESOLVER_REMOVE_FROM_GROUP)
    public void removeFromGroup(@Nonnull final User user, @Nonnull final Group group) throws DaoException {
        this.userDao.removeFromGroup(user, group);
    }

    /**
     * Returns all available groups
     *
     * Note that this method is not particular to a user, but is temporarily made available here until a
     * more suitable framework becomes available for providing group dao.
     *
     * @param baseSearchDn
     *  null value indicates to return all groups
     *
     * @return list of all available groups under base search dn
     */
    @Nonnull
    @Override
    public List<Group> getAllGroups(@Nullable final Dn baseSearchDn) throws DaoException {
        return this.userDao.getAllGroups(baseSearchDn);
    }

    protected void validateEmail(@Nonnull final User user) throws UserException {
        // throw an error if we don't have a valid email
        final String email = user.getEmail();
        if (email == null || !VALIDATOR_EMAIL.isValid(email) || CharMatcher.whitespace().matchesAnyOf(email)) {
            throw new InvalidEmailUserException("Invalid email '" + email + "' specified for user");
        }
    }

    protected void validateUser(@Nonnull final User user) throws UserException {
        // keep as extension point
    }

    protected void validateUsEmployeeId(@Nonnull final User user) throws UserException {
        final String employeeId = user.getEmployeeId();
        if (!Strings.isNullOrEmpty(employeeId) && !UserUtil.isValidUsEmployeeId(employeeId)) {
            throw new InvalidUsEmployeeIdUserException("Invalid Employee ID '" + employeeId + "' specified for user");
        }
    }

    protected void validateUsDesignation(@Nonnull final User user) throws UserException {
        final String designation = user.getCruDesignation();
        if (!Strings.isNullOrEmpty(designation) && !UserUtil.isValidUsDesignation(designation)) {
            throw new InvalidUsDesignationUserException("Invalid Designation '" + designation + "' specified for user");
        }
    }

    public interface UserManagerListener {
        void onPostCreateUser(@Nonnull User user);

        void onPreUpdateUser(@Nonnull User original, @Nonnull User user, @Nonnull User.Attr... attrs)
                throws UserException;

        void onPostUpdateUser(@Nonnull User original, @Nonnull User user, @Nonnull User.Attr... attrs);

        void onPostDeactivateUser(@Nonnull User user);

        void onPostReactivateUser(@Nonnull User user);
    }

    public abstract static class SimpleUserManagerListener implements UserManagerListener {
        @Override
        public void onPostCreateUser(@Nonnull final User user) {}

        @Override
        public void onPreUpdateUser(@Nonnull final User original, @Nonnull final User user,
                                    @Nonnull final User.Attr... attrs) throws UserException {}

        @Override
        public void onPostUpdateUser(@Nonnull User original, @Nonnull final User user,
                                     @Nonnull final User.Attr... attrs) {}

        @Override
        public void onPostDeactivateUser(@Nonnull final User user) {}

        @Override
        public void onPostReactivateUser(@Nonnull final User user) {}
    }
}
