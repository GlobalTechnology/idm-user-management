package org.ccci.idm.user.exception;

public class TheKeyGuidAlreadyExistsException extends UserException {
    private static final long serialVersionUID = 5470942112218454006L;

    public TheKeyGuidAlreadyExistsException() {
        super();
    }

    public TheKeyGuidAlreadyExistsException(final String message) {
        super(message);
    }
}
