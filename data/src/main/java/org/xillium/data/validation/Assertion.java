package org.xillium.data.validation;


/**
 * Assertions to enforce data integrity.
 */
public interface Assertion {
    /**
     * Applies the assertion on the given object.
     */
    public void apply(Object value) throws DataValidationException;

    /**
     * Assertion support utilities.
     */
    public static class S {
        /**
         * Assertion claiming that a given object is a nonnegative number
         */
        public static final Assertion NonnegativeNumber = new Assertion() {
            public void apply(Object value) throws DataValidationException {
                if (value instanceof Number) {
                    String s = value.toString();
                    if (s.charAt(0) == '-') {
                        throw new DataValidationException("NonnegativeNumber{" + s + '}');
                    }
                } else {
                    throw new DataValidationException("NonnegativeNumber{" + value + '}');
                }
            }
        };

        /**
         * Convenience method to apply an assertion to a value. Do nothing if the assertion is null.
         */
        public static void apply(Assertion assertion, Object value) throws DataValidationException {
            if (assertion != null) assertion.apply(value);
        }
    }
}
