package org.xillium.core;


/**
 * Use of HTTP status code
 */
public class ServiceException extends RuntimeException {
    public ServiceException() {
    }

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(Throwable cause) {
        super(cause);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = -6544596638507709427L;
}
