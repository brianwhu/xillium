package org.xillium.base;


/**
 * A Functor embodies a single-argument function.
 */
public interface Functor<T, V> {
    /**
     * Invokes the function on the sole argument.
     */
    public T invoke(V argument);
}
