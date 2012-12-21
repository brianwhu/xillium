package org.xillium.data.validation;


/**
 * Data validation exception: missing a required parameter.
 */
@SuppressWarnings("serial")
public class MissingParameterException extends DataValidationException {
    public MissingParameterException() {
    }

    public MissingParameterException(String message) {
        super(message);
    }

    public MissingParameterException(Throwable cause) {
        super(cause);
    }

    public MissingParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
