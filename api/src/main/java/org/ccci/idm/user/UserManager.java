package org.ccci.idm.user;

import org.ccci.idm.user.dao.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.exception.UserAlreadyExistsException;
import org.ccci.idm.user.exception.UserException;
import org.ccci.idm.user.exception.UserNotFoundException;

import java.util.List;

public interface UserManager {
    boolean doesEmailExist(String email);

    /**
     * Create a new {@link User}.
     *
     * @param user {@link User} object to be saved.
     * @throws UserAlreadyExistsException Thrown when the specified User conflicts with an existing user
     */
    void createUser(User user) throws UserException;

    /**
     * Update the specified {@link User}.
     *
     * @param user {@link User} to be updated.
     * @param attrs The User attributes to be updated. An empty list means to update default attributes.
     * @throws UserNotFoundException The specified user cannot be found to be updated
     */
    void updateUser(User user, User.Attr... attrs) throws UserException;

    /**
     * Deactivate the user by disabling the account and changing the e-mail address.
     *
     * @param user {@link User} to deactivate
     */
    void deactivateUser(User user) throws UserException;

    /**
     * Reactivate a previously deactivated user.
     *
     * @param user {@link User} to reactivate
     * @throws UserAlreadyExistsException thrown if the user being reactivated already exists
     */
    void reactivateUser(User user) throws UserException;

    /**
     * @param user the {@link User} to retrieve a fresh instance of
     * @return a fresh copy of the {@link User} object
     * @throws UserNotFoundException if the user can't be found
     */
    User getFreshUser(User user) throws UserException;

    /**
     * Locate the user with the specified e-mail address.
     *
     * @param email              E-mail address of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    User findUserByEmail(String email, boolean includeDeactivated);

    /**
     * Locate the user (not transitional) with the specified guid.
     *
     * @param guid GUID of user to find.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByGuid(String guid);

    /**
     * Locate the user (not transitional) with the specified Relay guid.
     *
     * @param guid GUID of user to find.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByRelayGuid(String guid);

    /**
     * Locate the user (not transitional) with the specified The Key guid.
     *
     * @param guid GUID of user to find.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findUserByTheKeyGuid(String guid);

    /**
     * Locate the user with the specified facebook id.
     *
     * @param id the facebook id being search for
     * @return {@link User} with the specified facebook id, or <tt>null</tt> if not found.
     */
    User findUserByFacebookId(String id);

    /**
     * Locate the user with the specified employee id.
     *
     * @param employeeId employee id of user to find.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    User findUserByEmployeeId(String employeeId);

    /**
     * Find all users matching the first name pattern.
     *
     * @param pattern Pattern used for matching first name.
     * @return {@link java.util.List} of {@link User} objects.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByFirstName(String pattern) throws ExceededMaximumAllowedResultsException;

    /**
     * Find all users matching the last name pattern.
     *
     * @param pattern Pattern used for matching last name.
     * @return {@link List} of {@link User} objects.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByLastName(String pattern) throws ExceededMaximumAllowedResultsException;

    /**
     * Find all users matching the userid pattern.
     *
     * @param pattern            Pattern used for matching email.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByEmail(String pattern, boolean includeDeactivated) throws ExceededMaximumAllowedResultsException;
}
