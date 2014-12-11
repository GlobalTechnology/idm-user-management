package org.ccci.idm.user.exception;

public class InvalidEmailUserException extends UserException {
    private static final long serialVersionUID = -591402303593429376L;

    public InvalidEmailUserException(final String message) {
        super(message);
    }
}
