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

    /**
     * Update the specified {@link User}.
     *
     * @param user {@link User} to be updated.
     * @throws UserNotFoundException The specified user cannot be found to be updated
     */
    void updateUser(User user) throws UserNotFoundException;

    /**
     * @param user the {@link User} to retrieve a fresh instance of
     * @return a fresh copy of the {@link User} object
     * @throws UserNotFoundException if the user can't be found
     */
    User getFreshUser(User user) throws UserNotFoundException;
}
