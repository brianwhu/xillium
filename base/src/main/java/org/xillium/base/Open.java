package org.xillium.base;


/**
 * Open interface marks an object as being designed to hold public data members.
 */
public interface Open {
    /**
     * A wrapper to enclose a non-Open object in an Open object.
     */
    public static class Wrapper<T> implements Open {
        public final T value;

        /**
         * Constructs a Wrapper to enclose a given object.
         *
         * @param v an object to be enclosed as a public member of this Wrapper instance.
         */
        public Wrapper(T v) { value = v; }
    }
}
