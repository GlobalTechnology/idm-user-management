package org.ccci.idm.user.dao;

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
    void update(User user);

    /**
     * Update an existing user in the persistent user store.
     *
     * @param original The original version of the user being updated
     * @param user     User to be updated.
     */
    void update(User original, User user);

    /**
     * Find the user with the specified e-mail.
     *
     * @param email Email for lookup.
     * @return Requested {@link User} or <tt>null</tt> if not found.
     */
    User findByEmail(String email);

    /**
     * Find the user with the specified guid.
     *
     * @param guid guid for lookup.
     * @return Request {@link User} or <tt>null</tt> if not found.
     */
    User findByGuid(String guid);

    /**
     * Find the user with the specified Relay guid.
     *
     * @param guid guid for lookup.
     * @return Request {@link User} or <tt>null</tt> if not found.
     */
    User findByRelayGuid(String guid);

    /**
     * Find the user with the specified The Key guid.
     *
     * @param guid guid for lookup.
     * @return Request {@link User} or <tt>null</tt> if not found.
     */
    User findByTheKeyGuid(String guid);

    /**
     * Find the user with the specified Facebook Id
     *
     * @param id the facebook id to search for
     * @return Requested {@link User} or <tt>null</tt> if not found.
     */
    User findByFacebookId(String id);

    /**
     * Find all users matching the first name pattern.
     *
     * @param pattern Pattern used for matching first name.
     * @return {@link java.util.List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByFirstName(String pattern) throws ExceededMaximumAllowedResultsException;

    /**
     * Find all users matching the last name pattern.
     *
     * @param pattern Pattern used for matching last name.
     * @return {@link List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByLastName(String pattern) throws ExceededMaximumAllowedResultsException;

    /**
     * Find all users matching the email pattern.
     *
     * @param pattern            Pattern used for matching emails.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     */
    List<User> findAllByEmail(String pattern, boolean includeDeactivated) throws ExceededMaximumAllowedResultsException;
}
