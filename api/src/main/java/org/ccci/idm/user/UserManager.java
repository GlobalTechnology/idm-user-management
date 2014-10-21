package org.ccci.idm.user;

public interface UserManager {
    boolean doesEmailExist(String email);

    /**
     * Create a new {@link User}.
     *
     * @param user {@link User} object to be saved.
     * @throws UserAlreadyExistsException Thrown when the specified User conflicts with an existing user
     */
    void createUser(User user) throws UserException;
}
