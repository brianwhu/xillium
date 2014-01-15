package org.xillium.core.management;


/**
 * Exception to represent a management exception
 */
public class ManagementDataException extends ManagementException {
    public ManagementDataException() {
    }

    public ManagementDataException(String message) {
        super(message);
    }

    public ManagementDataException(Throwable cause) {
        super(cause);
    }

    public ManagementDataException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 7078290769696468350L;
}
