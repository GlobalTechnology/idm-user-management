package org.ccci.idm.user.dao;

public class ExceededMaximumAllowedResultsException extends Exception {
    private static final long serialVersionUID = -7702132392786827559L;

    public ExceededMaximumAllowedResultsException(final String message) {
        super(message);
    }

    public ExceededMaximumAllowedResultsException() {
        super();
    }
}
