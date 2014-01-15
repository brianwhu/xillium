package org.xillium.core.management;


/**
 * Exception to represent a management exception
 */
public class ManagementRealmEncodingException extends ManagementRealmNotFoundException {
    public ManagementRealmEncodingException() {
    }

    public ManagementRealmEncodingException(String message) {
        super(message);
    }

    public ManagementRealmEncodingException(Throwable cause) {
        super(cause);
    }

    public ManagementRealmEncodingException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = -7767365901443512577L;
}
