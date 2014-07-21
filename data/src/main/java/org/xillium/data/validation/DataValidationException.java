package org.xillium.data.validation;


/**
 * Data validation exception.
 */
public class DataValidationException extends org.xillium.data.DataException {
    public DataValidationException(String type, String name, Object value) {
        super("DataValidationFailure(" + type + ")Of(" + name + ")On{" + value + '}');
    }

    public DataValidationException(String type, String name, Object value, Throwable cause) {
        super("DataValidationFailure(" + type + ")Of(" + name + ")On{" + value + '}', cause);
    }

    private static final long serialVersionUID = 7531119436056645564L;
}
