package org.xillium.core.management;

import org.xillium.core.RemoteServiceException;


/**
 * Exception to represent a management exception
 */
public class ManagementException extends RemoteServiceException {
    public ManagementException() {
    }

    public ManagementException(String message) {
        super(message);
    }

    public ManagementException(Throwable cause) {
        super(cause);
    }

    public ManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = -219072747091874298L;
}
