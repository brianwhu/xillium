package org.xillium.data.validation;


/**
 * Data validation exception.
 */
@SuppressWarnings("serial")
public class DataValidationException extends org.xillium.data.DataException {
    public DataValidationException() {
    }

    public DataValidationException(String message) {
        super(message);
    }

    public DataValidationException(Throwable cause) {
        super(cause);
    }

    public DataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
