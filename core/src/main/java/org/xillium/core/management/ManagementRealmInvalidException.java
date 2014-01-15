package org.xillium.core.management;


/**
 * Exception to represent a management exception
 */
public class ManagementRealmInvalidException extends ManagementRealmException {
    public ManagementRealmInvalidException() {
    }

    public ManagementRealmInvalidException(String message) {
        super(message);
    }

    public ManagementRealmInvalidException(Throwable cause) {
        super(cause);
    }

    public ManagementRealmInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 3903537305492126901L;
}
