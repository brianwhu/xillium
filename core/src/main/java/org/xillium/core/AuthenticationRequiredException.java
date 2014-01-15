package org.xillium.core;


/**
 * Thrown when authorization fails and authentication should be attempted.
 */
public class AuthenticationRequiredException extends AuthorizationException {
    public AuthenticationRequiredException() {
    }

    public AuthenticationRequiredException(String message) {
        super(message);
    }

    public AuthenticationRequiredException(Throwable cause) {
        super(cause);
    }

    public AuthenticationRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = -7665738579668795108L;
}
