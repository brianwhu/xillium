package org.xillium.data.validation;


/**
 * Data validation exception: missing a required parameter.
 */
public class MissingParameterException extends DataValidationException {
    public MissingParameterException(String name, Object value) {
        super("MISSING", name, value);
    }

    public MissingParameterException(String name, Object value, Throwable cause) {
        super("MISSING", name, value, cause);
    }

    private static final long serialVersionUID = -6821700092185232348L;
}
