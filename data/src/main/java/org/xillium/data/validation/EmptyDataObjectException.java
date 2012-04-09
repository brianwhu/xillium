package org.xillium.data.validation;


/**
 * Data validation exception: the whole data object is empty.
 */
public class EmptyDataObjectException extends MissingParameterException {
    public EmptyDataObjectException() {
    }

    public EmptyDataObjectException(String message) {
        super(message);
    }

    public EmptyDataObjectException(Throwable cause) {
        super(cause);
    }

    public EmptyDataObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
