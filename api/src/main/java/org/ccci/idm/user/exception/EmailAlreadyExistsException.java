package org.ccci.idm.user.exception;

public class EmailAlreadyExistsException extends UserAlreadyExistsException {
    private static final long serialVersionUID = -345912295700798200L;

    public EmailAlreadyExistsException() {
        super();
    }

    public EmailAlreadyExistsException(final String message) {
        super(message);
    }
}
