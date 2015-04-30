package org.xillium.base;

import org.xillium.base.beans.Beans;


/**
 * A Factory embodies an n-nary method that produces a value.
 */
public interface Factory<T> {
    /**
     * Makes an object.
     *
     * @param arguments - arguments provided to the factory
     */
    public T make(Object... arguments);

    /**
     * An implementation of Factory that uses Class construction to create new objects.
     */
    public static class New<T> implements Factory<T> {
        private final Class<? extends T> _type;

        public New(Class<? extends T> type) {
            _type = type;
        }

        public T make(Object... args) {
            try {
                return Beans.create(_type, args);
            } catch (Exception x) {
                throw new RuntimeException(x.getMessage(), x);
            }
        }
    }
}
