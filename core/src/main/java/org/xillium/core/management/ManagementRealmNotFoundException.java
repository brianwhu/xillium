package org.xillium.core.management;


/**
 * Exception to represent a management exception
 */
public class ManagementRealmNotFoundException extends ManagementRealmInvalidException {
    public ManagementRealmNotFoundException() {
    }

    public ManagementRealmNotFoundException(String message) {
        super(message);
    }

    public ManagementRealmNotFoundException(Throwable cause) {
        super(cause);
    }

    public ManagementRealmNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = -6144998210807368840L;
}
