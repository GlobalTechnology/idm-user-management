package org.ccci.idm.user.dao.exception;

public class InterruptedDaoException extends DaoException {
    private static final long serialVersionUID = 8334483745750069287L;

    public InterruptedDaoException() {
        super();
    }

    public InterruptedDaoException(final Throwable cause) {
        super(cause);
    }
}
