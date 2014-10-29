package org.xillium.data.validation;

import java.util.*;


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
                        throw new DataValidationException("NONNEGATIVE", "", s);
                    }
                } else {
                    throw new DataValidationException("NONNEGATIVE", "", value);
                }
            }
        };

        /**
         * Assertion claiming that a given object is in a predefined set
         */
        public static class In implements Assertion {
            private final Set<Object> _set = new HashSet<Object>();

            public <T> In(T[] values) {
                for (T value: values) _set.add(value);
            }

            public void apply(Object value) throws DataValidationException {
                if (!_set.contains(value)) {
                    throw new DataValidationException("IN", "", value);
                }
            }
        }

        /**
         * Convenience method to apply an assertion to a value. Do nothing if the assertion is null.
         */
        public static void apply(Assertion assertion, Object value) throws DataValidationException {
            if (assertion != null) assertion.apply(value);
        }
    }
}
