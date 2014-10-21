package org.ccci.idm.user;

public class UserNotFoundException extends UserException {
    private static final long serialVersionUID = 7539829450378233497L;

    public UserNotFoundException() {
        super();
    }

    public UserNotFoundException(final String message) {
        super(message);
    }
}
