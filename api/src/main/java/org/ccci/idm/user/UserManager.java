package org.ccci.idm.user;

public interface UserManager {
    /**
     * Create a new {@link User}.
     *
     * @param user {@link User} object to be saved.
     * @throws UserAlreadyExistsException
     */
    void createUser(User user) throws UserAlreadyExistsException;
}
