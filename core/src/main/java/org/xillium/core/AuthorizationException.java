package org.xillium.core;


/**
 * Thrown when authorization fails
 */
public class AuthorizationException extends RuntimeException {
    public AuthorizationException() {
    }

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(Throwable cause) {
        super(cause);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = -2082479184425267793L;
}
