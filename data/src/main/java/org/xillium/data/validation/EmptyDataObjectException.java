package org.xillium.data.validation;


/**
 * Data validation exception: the whole data object is empty.
 */
public class EmptyDataObjectException extends MissingParameterException {
    public EmptyDataObjectException(String message) {
        super("", message);
    }

    public EmptyDataObjectException(String message, Throwable cause) {
        super("", message, cause);
    }

    private static final long serialVersionUID = 4318641522545221014L;
}
