package org.xillium.core.management;


/**
 * Exception to represent a management exception
 */
public class ManagementRealmException extends ManagementDataException {
    public ManagementRealmException() {
    }

    public ManagementRealmException(String message) {
        super(message);
    }

    public ManagementRealmException(Throwable cause) {
        super(cause);
    }

    public ManagementRealmException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = -8863112020338519304L;
}
