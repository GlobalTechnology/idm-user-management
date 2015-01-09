package org.ccci.idm.user.migration;

import org.ccci.idm.user.User;
import org.ccci.idm.user.UserManager;
import org.ccci.idm.user.exception.UserException;

public interface MigrationUserManager extends UserManager {
    void moveLegacyKeyUser(User user);

    void generateNewGuid(User user) throws UserException;

    /**
     * Locate the user with the specified The Key guid. Deactivated accounts are included in the search.
     *
     * @param guid GUID of user to find.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findLegacyKeyUserByTheKeyGuid(String guid);

    /**
     * Locate the user with the specified The Key guid.
     *
     * @param guid               GUID of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified guid, or <tt>null</tt> if not found.
     */
    User findLegacyKeyUserByTheKeyGuid(String guid, boolean includeDeactivated);

    /**
     * Locate the user with the specified e-mail address. Does not return deactivated accounts.
     *
     * @param email              E-mail address of user to find.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    User findLegacyKeyUserByEmail(String email);

    /**
     * Locate the user with the specified e-mail address.
     *
     * @param email              E-mail address of user to find.
     * @param includeDeactivated If <tt>true</tt> then deactivated accounts are included.
     * @return {@link User} with the specified e-mail address, or <tt>null</tt> if not found.
     */
    User findLegacyKeyUserByEmail(String email, boolean includeDeactivated);
}
