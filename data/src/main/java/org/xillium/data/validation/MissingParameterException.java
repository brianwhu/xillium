package org.xillium.data.validation;


/**
 * Data validation exception: missing a required parameter.
 */
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

    private static final long serialVersionUID = -6821700092185232348L;
}
