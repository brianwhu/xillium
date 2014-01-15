package org.xillium.base.beans;


/**
 * Common data exception.
 */
public class BeanAssemblyException extends RuntimeException {
    public BeanAssemblyException() {
    }

    public BeanAssemblyException(String message) {
        super(message);
    }

    public BeanAssemblyException(Throwable cause) {
        super(cause);
    }

    public BeanAssemblyException(String message, Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 8208763758349113075L;
}
