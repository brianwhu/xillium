package org.xillium.core;


/**
 * Thrown when authorization fails and authentication should be attempted.
 */
@SuppressWarnings("serial")
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
}
