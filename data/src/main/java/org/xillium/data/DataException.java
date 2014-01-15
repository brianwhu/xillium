package org.xillium.data;


/**
 * Common data exception.
 */
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

    private static final long serialVersionUID = -2108493108912848283L;
}
