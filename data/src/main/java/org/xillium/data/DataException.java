package org.xillium.data;


/**
 * Common data exception.
 */
@SuppressWarnings("serial")
public class DataException extends Exception {
    public DataException() {
    }

    public DataException(String message) {
        super(message);
    }

    public DataException(Throwable cause) {
        super(cause);
    }

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
