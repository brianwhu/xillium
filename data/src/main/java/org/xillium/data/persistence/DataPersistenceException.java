package org.xillium.data.persistence;


/**
 * Data persistence exception.
 */
public class DataPersistenceException extends org.xillium.data.DataException {
    public DataPersistenceException() {
    }

    public DataPersistenceException(String message) {
        super(message);
    }

    public DataPersistenceException(Throwable cause) {
        super(cause);
    }

    public DataPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = -4791404464647658951L;
}
