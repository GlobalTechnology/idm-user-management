package org.ccci.idm.user.dao;

import org.ccci.idm.user.Group;
import org.ccci.idm.user.User;

import java.util.List;

public interface UserDao {
    /**
     * Save the specified user.
     *
     * @param user User to be created.
     */
    void save(User user);

    /**
     * Update an existing user in the persistent user store.
     *
     * @param user User to be updated.
     */
    void update(User user, User.Attr... attrs);

    /**
     * Update an existing user in the persistent user store.
     *
     * @param original The original version of the user being updated
     * @param user     User to be updated.
     */
    void update(User original, User user, User.Attr... attrs);

    /**
     * Find the user with the specified e-mail.
     *
     * @param email              Email for lookup.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return Requested {@link User} or <tt>null</tt> if not found.
     */
    User findByEmail(String email, boolean includeDeactivated);

    /**
     * Find the user with the specified guid.
     *
     * @param guid               guid for lookup.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return Request {@link User} or <tt>null</tt> if not found.
     */
    User findByGuid(String guid, boolean includeDeactivated);

    /**
     * Find the user with the specified Relay guid.
     *
     * @param guid               guid for lookup.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return Request {@link User} or <tt>null</tt> if not found.
     */
    User findByRelayGuid(String guid, boolean includeDeactivated);

    /**
     * Find the user with the specified The Key guid.
     *
     * @param guid               guid for lookup.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return Request {@link User} or <tt>null</tt> if not found.
     */
    User findByTheKeyGuid(String guid, boolean includeDeactivated);

    /**
     * Find the user with the specified Facebook Id
     *
     * @param id                 the facebook id to search for
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return Requested {@link User} or <tt>null</tt> if not found.
     */
    User findByFacebookId(String id, boolean includeDeactivated);

    /**
     * Find all users matching the first name pattern.
     *
     * @param pattern            Pattern used for matching first name.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link java.util.List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByFirstName(String pattern, boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException;

    /**
     * Find all users matching the last name pattern.
     *
     * @param pattern            Pattern used for matching last name.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByLastName(String pattern, boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException;

    /**
     * Find all users matching the email pattern.
     *
     * @param pattern            Pattern used for matching emails.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByEmail(String pattern, boolean includeDeactivated) throws ExceededMaximumAllowedResultsException;

    /**
     * Find the user with the specified employee id.
     *
     * @param employeeId         Employee id for lookup.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return Requested {@link org.ccci.idm.user.User} or <tt>null</tt> if not found.
     */
    User findByEmployeeId(String employeeId, boolean includeDeactivated);

    /**
     * Add user to group
     *
     * @param user to add
     * @param group to group
     */
    void addToGroup(User user, Group group);

    /**
     * Remove user from group
     *
     * @param user to remove
     * @param group from group
     */
    void removeFromGroup(User user, Group group);

}
