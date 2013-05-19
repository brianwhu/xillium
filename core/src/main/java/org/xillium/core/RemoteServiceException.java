package org.xillium.core;


/**
 * Exception to represent a remote ServiceException.
 */
@SuppressWarnings("serial")
public class RemoteServiceException extends ServiceException {
    public RemoteServiceException() {
    }

    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(Throwable cause) {
        super(cause);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
