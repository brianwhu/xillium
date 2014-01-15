package org.xillium.data.validation;


/**
 * Data validation exception.
 */
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

    private static final long serialVersionUID = 7531119436056645564L;
}
