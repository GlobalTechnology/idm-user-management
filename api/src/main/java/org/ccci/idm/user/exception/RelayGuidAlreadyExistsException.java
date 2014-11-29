package org.ccci.idm.user.exception;

public class RelayGuidAlreadyExistsException extends UserAlreadyExistsException {
    private static final long serialVersionUID = 6899292295712606400L;

    public RelayGuidAlreadyExistsException() {
        super();
    }

    public RelayGuidAlreadyExistsException(final String message) {
        super(message);
    }
}
