package org.ccci.idm.user.dao.exception;

public class DaoException extends RuntimeException {
    private static final long serialVersionUID = 8580677864955816717L;

    public DaoException() {
        super();
    }

    public DaoException(final String message) {
        super(message);
    }

    public DaoException(final Throwable cause) {
        super(cause);
    }
}
