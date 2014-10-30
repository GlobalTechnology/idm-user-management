package org.ccci.idm.user;

public class RelayGuidAlreadyExistsException extends UserException {
    private static final long serialVersionUID = 6899292295712606400L;

    public RelayGuidAlreadyExistsException() {
        super();
    }

    public RelayGuidAlreadyExistsException(final String message) {
        super(message);
    }
}
