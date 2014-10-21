package org.ccci.idm.user;

public class UserAlreadyExistsException extends UserException {
    private static final long serialVersionUID = 4283702679063923324L;

    public UserAlreadyExistsException() {
        super();
    }

    public UserAlreadyExistsException(final String message) {
        super(message);
    }
}
