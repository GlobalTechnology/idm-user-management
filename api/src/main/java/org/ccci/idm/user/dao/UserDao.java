package org.ccci.idm.user.dao;

import com.google.common.annotations.Beta;
import org.ccci.idm.user.Group;
import org.ccci.idm.user.SearchQuery;
import org.ccci.idm.user.User;
import org.ccci.idm.user.dao.exception.DaoException;
import org.ccci.idm.user.dao.exception.ExceededMaximumAllowedResultsException;
import org.ccci.idm.user.query.Attribute;
import org.ccci.idm.user.query.Expression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface UserDao {
    /**
     * Indicates if this UserDao is readonly
     * @return true if this UserDao is readonly
     */
    boolean isReadOnly();

    /**
     * Save the specified user.
     *
     * @param user User to be created.
     */
    void save(User user) throws DaoException;

    /**
     * Update an existing user in the persistent user store.
     *
     * @param user User to be updated.
     */
    void update(User user, User.Attr... attrs) throws DaoException;

    /**
     * Update an existing user in the persistent user store.
     *
     * @param original The original version of the user being updated
     * @param user     User to be updated.
     */
    default void update(@Nonnull User original, @Nonnull User user, User.Attr... attrs) throws DaoException {
        update(user, attrs);
    }

    default void deactivate(@Nonnull final User user) throws DaoException {
        // Create a deep clone copy before proceeding
        final User original = user.clone();

        // Set a few flags to disable the account
        user.setDeactivated(true);
        user.setLoginDisabled(true);

        // remove any federated identities
        user.removeFacebookId(original.getFacebookId());

        // update the user object
        update(original, user, User.Attr.EMAIL, User.Attr.FLAGS, User.Attr.FACEBOOK);
    }

    default void reactivate(@Nonnull final User user) {
        // Create a deep clone copy before proceeding
        final User original = user.clone();

        // Restore several settings on the user object
        user.setDeactivated(false);
        user.setLoginDisabled(false);
        user.setAllowPasswordChange(true);

        // update the user object
        update(original, user, User.Attr.EMAIL, User.Attr.FLAGS);
    }

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
    @Deprecated
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
     * Find all users matching the search query.
     *
     * @param query the users to find
     * @return {@link List} of {@link User} objects.
     * @throws DaoException
     * @deprecated Since v1.0.0, use {@link UserDao#streamUsers} instead.
     */
    @Beta
    @Nonnull
    @Deprecated
    List<User> findAllByQuery(@Nonnull SearchQuery query) throws DaoException;

    /**
     * Find all users matching the first name pattern.
     *
     * @param pattern            Pattern used for matching first name.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link java.util.List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     * @deprecated Since v1.0.0, use {@link UserDao#streamUsers} instead.
     */
    @Deprecated
    default List<User> findAllByFirstName(String pattern, boolean includeDeactivated) throws
            ExceededMaximumAllowedResultsException {
        return streamUsers(Attribute.FIRST_NAME.like(pattern), includeDeactivated, true).collect(Collectors.toList());
    }

    /**
     * Find all users matching the last name pattern.
     *
     * @param pattern            Pattern used for matching last name.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects, or <tt>null</tt> if none are found.
     * @throws ExceededMaximumAllowedResultsException
     * @deprecated Since v1.0.0, use {@link UserDao#streamUsers} instead.
     */
    @Deprecated
    default List<User> findAllByLastName(String pattern, boolean includeDeactivated)
            throws ExceededMaximumAllowedResultsException {
        return streamUsers(Attribute.LAST_NAME.like(pattern), includeDeactivated, true).collect(Collectors.toList());
    }

    /**
     * Find all users matching the email pattern.
     *
     * @param pattern            Pattern used for matching emails.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects found.
     * @throws ExceededMaximumAllowedResultsException
     * @deprecated Since v1.0.0, use {@link UserDao#streamUsers} instead.
     */
    @Nonnull
    @Deprecated
    default List<User> findAllByEmail(String pattern, boolean includeDeactivated) throws ExceededMaximumAllowedResultsException {
        return streamUsers(Attribute.EMAIL.like(pattern), includeDeactivated, true).collect(Collectors.toList());
    }

    /**
     * Find all users in the specified group
     *
     * @param group              The {@link Group} to return membership for.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link List} of {@link User} objects found.
     * @throws ExceededMaximumAllowedResultsException if there are too many users found
     * @throws DaoException
     * @deprecated Since v1.0.0, use {@link UserDao#streamUsers} instead.
     */
    @Nonnull
    @Deprecated
    List<User> findAllByGroup(@Nonnull Group group, boolean includeDeactivated) throws DaoException;

    /**
     * Find the user with the specified Designation
     *
     * @param designation        designation being searched for.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return Requested {@link User} or <tt>null</tt> if not found.
     */
    @Nullable
    User findByDesignation(@Nullable String designation, boolean includeDeactivated);

    /**
     * Find the user with the specified employee id.
     *
     * @param employeeId         Employee id for lookup.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return Requested {@link org.ccci.idm.user.User} or <tt>null</tt> if not found.
     */
    User findByEmployeeId(String employeeId, boolean includeDeactivated);

    /**
     * Add all users to the specified {@link BlockingQueue}. This method will use {@link BlockingQueue#put(Object)} to
     * enqueue users.
     *
     * @param queue              The {@link BlockingQueue} to add all users to.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return number of users enqueued
     * @deprecated Since 1.0.0, use {@link UserDao#streamUsers} instead.
     */
    @Deprecated
    int enqueueAll(@Nonnull BlockingQueue<User> queue, boolean includeDeactivated) throws DaoException;

    /**
     * Provide a Java 8 Stream over all the users that match the specified expression. This stream needs to be closed
     * after use.
     *
     * @param expression         The search expression
     * @param includeDeactivated Whether deactivated users should be included in the Stream
     * @return a Stream of all users
     */
    @Nonnull
    default Stream<User> streamUsers(@Nullable Expression expression, boolean includeDeactivated) {
        return streamUsers(expression, includeDeactivated, false);
    }

    /**
     * Provide a Java 8 Stream over all the users that match the specified expression. This stream needs to be closed
     * after use.
     *
     * @param expression         The search expression
     * @param includeDeactivated Whether deactivated users should be included in the Stream
     * @param restrictMaxAllowed A boolean indicating that the stream should be restricted to an upper search limit
     * @return a Stream of all users
     */
    @Nonnull
    Stream<User> streamUsers(@Nullable Expression expression, boolean includeDeactivated, boolean restrictMaxAllowed);

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
     * @param baseSearch null value indicates to return all groups
     * @return list of all available groups under base search
     */
    @Nonnull
    default List<Group> getAllGroups(@Nullable String baseSearch) throws DaoException {
        return Collections.emptyList();
    }
}
