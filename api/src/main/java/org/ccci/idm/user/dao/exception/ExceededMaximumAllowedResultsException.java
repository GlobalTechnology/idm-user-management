package org.ccci.idm.user.dao.exception;

public class ExceededMaximumAllowedResultsException extends DaoException {
    private static final long serialVersionUID = -7702132392786827559L;

    public ExceededMaximumAllowedResultsException() {
        super();
    }

    public ExceededMaximumAllowedResultsException(final String message) {
        super(message);
    }
}
