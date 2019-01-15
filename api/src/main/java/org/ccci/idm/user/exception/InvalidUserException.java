package org.ccci.idm.user.exception;

public abstract class InvalidUserException extends UserException {
    private static final long serialVersionUID = -961504306583670021L;

    public InvalidUserException() { }

    public InvalidUserException(final String message) {
        super(message);
    }
}
