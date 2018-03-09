package org.ccci.idm.user;

import com.google.common.annotations.Beta;
import org.ccci.idm.user.dao.exception.DaoException;
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.exception.EmailAlreadyExistsException;
import org.ccci.idm.user.exception.UserAlreadyExistsException;
import org.ccci.idm.user.exception.UserException;
import org.ccci.idm.user.exception.UserNotFoundException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public interface UserManager {
    /**
     * Returns whether the underlying UserDao is readonly
     *
     * @return true if the underlying UserDao is readonly
     */
    boolean isReadOnly();

    /**
     * Check to see if the specified email already exists
     *
     * @param email Email to check for
     * @return true if the specified email already exists
     */
    boolean doesEmailExist(String email);

    /**
     * Create a new {@link User}.
     *
     * @param user {@link User} object to be saved.
     * @throws UserAlreadyExistsException Thrown when the specified User conflicts with an existing user
     */
    void createUser(User user) throws DaoException, UserException;

    /**
     * Update the specified {@link User}.
     *
     * @param user {@link User} to be updated.
     * @param attrs The User attributes to be updated. An empty list means to update default attributes.
     * @throws UserNotFoundException The specified user cannot be found to be updated
     */
    void updateUser(User user, User.Attr... attrs) throws DaoException, UserException;

    /**
     * Deactivate the user by disabling the account and changing the e-mail address.
     *
     * @param user {@link User} to deactivate
     */
    void deactivateUser(User user) throws DaoException, UserException;

    /**
     * Reactivate a previously deactivated user.
     *
     * @param user {@link User} to reactivate
     * @throws EmailAlreadyExistsException thrown if the user being reactivated conflicts with another user that
     * already exists
     */
    void reactivateUser(User user) throws DaoException, UserException;

    /**
     * Returns if the specified user is currently locked for MFA intruder detection.
     *
     * @param user the User to check for an MFA intruder lock
     * @return true if the specified User is locked, false otherwise.
     */
    boolean isMfaIntruderLocked(@Nonnull final User user);

    /**
     * Track a failed MFA login attempt.
     *
     * @param user the User to track a failed MFA login attempt for.
     * @throws UserNotFoundException The specified user cannot be found to be updated
     */
    void trackFailedMfaLogin(@Nonnull final User user) throws DaoException, UserException;

    /**
     * Reset the MFA intruder lock state for the specified User.
     *
     * @param user the User to reset the MFA intruder lock on
     */
    void resetMfaIntruderLock(@Nonnull final User user) throws DaoException, UserException;

    /**
     * @param user the {@link User} to retrieve a fresh instance of
     * @return a fresh copy of the {@link User} object
     * @throws UserNotFoundException if the user can't be found
     */
    @Nonnull
    User getFreshUser(@Nonnull User user) throws UserException;

    /**
     * Locate the user with the specified e-mail address. Does not return deactivated accounts.
     *
     * @param email              E-mail address of user to find.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    User findUserByEmail(String email);

    /**
     * Locate the user with the specified e-mail address.
     *
     * @param email              E-mail address of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    User findUserByEmail(String email, boolean includeDeactivated);

    /**
     * Locate the user with the specified guid. Deactivated accounts are included in the search.
     *
     * @param guid GUID of user to find.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByGuid(String guid);

    /**
     * Locate the user with the specified guid.
     *
     * @param guid               GUID of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByGuid(String guid, boolean includeDeactivated);

    /**
     * Locate the user with the specified Relay guid. Deactivated accounts are included in the search.
     *
     * @param guid GUID of user to find.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByRelayGuid(String guid);

    /**
     * Locate the user with the specified Relay guid.
     *
     * @param guid               GUID of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByRelayGuid(String guid, boolean includeDeactivated);

    /**
     * Locate the user with the specified The Key guid. Deactivated accounts are included in the search.
     *
     * @param guid GUID of user to find.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByTheKeyGuid(String guid);

    /**
     * Locate the user with the specified The Key guid.
     *
     * @param guid               GUID of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByTheKeyGuid(String guid, boolean includeDeactivated);

    /**
     * Locate the user with the specified facebook id. Does not return deactivated accounts.
     *
     * @param id the facebook id being search for
     * @return {@link User} with the specified facebook id, or <tt>null</tt> if not found.
     */
    User findUserByFacebookId(String id);

    /**
     * Locate the user with the specified facebook id.
     *
     * @param id                 the facebook id being search for
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified facebook id, or <tt>null</tt> if not found.
     */
    User findUserByFacebookId(String id, boolean includeDeactivated);

    /**
     * Locate the user with the specified designation. Does not return deactivated accounts.
     *
     * @param designation designation of user to find.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    @Nullable
    User findUserByDesignation(@Nullable String designation);

    /**
     * Locate the user with the specified employee id.
     *
     * @param designation        designation of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    @Nullable
    User findUserByDesignation(@Nullable String designation, boolean includeDeactivated);

    /**
     * Locate the user with the specified employee id. Does not return deactivated accounts.
     *
     * @param employeeId employee id of user to find.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    User findUserByEmployeeId(String employeeId);

    /**
     * Locate the user with the specified employee id.
     *
     * @param employeeId         employee id of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    User findUserByEmployeeId(String employeeId, boolean includeDeactivated);

    /**
     * Find all users matching the search query.
     *
     * @param query the users to find
     * @return {@link List} of {@link User} objects.
     * @throws DaoException
     */
    @Beta
    @Nonnull
    List<User> findAllByQuery(@Nonnull SearchQuery query) throws DaoException;

    /**
     * Find all users matching the first name pattern.
     *
     * @param pattern            Pattern used for matching first name.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link java.util.List} of {@link User} objects.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByFirstName(String pattern, boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException;

    /**
     * Find all users matching the last name pattern.
     *
     * @param pattern            Pattern used for matching last name.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByLastName(String pattern, boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException;

    /**
     * Find all users matching the email pattern.
     *
     * @param pattern            Pattern used for matching email.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByEmail(String pattern, boolean includeDeactivated) throws ExceededMaximumAllowedResultsException;

    /**
     * Find all users in the specified group
     *
     * @param group              The {@link Group} to return membership for.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects found.
     * @throws ExceededMaximumAllowedResultsException if there are too many users found
     * @throws DaoException
     */
    @Nonnull
    List<User> findAllByGroup(@Nonnull Group group, boolean includeDeactivated) throws DaoException;

    /**
     * Add all users to the specified {@link BlockingQueue}. This method will use {@link BlockingQueue#put(Object)} to
     * enqueue users.
     *
     * @param queue              The {@link BlockingQueue} to add all users to.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return number of users enqueued
     */
    int enqueueAll(@Nonnull BlockingQueue<User> queue, boolean includeDeactivated) throws DaoException;

    /**
     * Add user to group
     *
     * @param user to add
     * @param group to group
     */
    void addToGroup(@Nonnull User user, @Nonnull Group group) throws DaoException;

    /**
     * Add user to group
     *
     * @param user        to add to a group
     * @param group       to add the user to
     * @param addSecurity specifies if the Group security should be shared with the user being added.
     */
    void addToGroup(@Nonnull User user, @Nonnull Group group, boolean addSecurity) throws DaoException;

    /**
     * Remove user from group
     *
     * @param user to remove
     * @param group from group
     */
    void removeFromGroup(@Nonnull User user, @Nonnull Group group) throws DaoException;

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
    List<Group> getAllGroups(@Nullable Dn baseSearchDn) throws DaoException;
}
