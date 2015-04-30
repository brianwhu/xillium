package org.xillium.base;


/**
 * A Functor embodies a single-argument function.
 */
public interface Functor<T, V> {
    /**
     * Invokes the function on the sole argument.
     *
     * @param argument the sole argument to pass to the functor
     * @return a return value
     */
    public T invoke(V argument);
}
