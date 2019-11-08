package org.ccci.idm.user.exception;

public class UserException extends RuntimeException {
    private static final long serialVersionUID = 2012533477643620017L;

    public UserException() {
        super();
    }

    public UserException(final String message) {
        super(message);
    }
}
