package org.xillium.base;

import java.util.concurrent.Callable;


/**
 * A thread-safe, lazily-initialized singleton wrapper with double-checked locking. The performance gain from DCL becomes more
 * significant as the number of contending threads grows.
 */
public class Singleton<T> {
    private volatile T _value;

    /**
     * Tests whether the referenced object is missing, where the meaning of "missing" is defined by implementation.
     * This implementation simply tests reference nullity.
     *
     * @param object the object to check
     * @return whether the object is missing
     */
    protected boolean isMissing(T object) {
        return object == null;
    }

    /**
     * Retrieves the singleton object, creating it by calling a provider if it is not created yet. This method uses
     * double-checked locking to ensure that only one instance will ever be created, while keeping retrieval cost at
     * minimum.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Double-checked_locking">Double-checked locking</a>
     *
     * @param callable a {@link java.util.concurrent.Callable Callable} that can be called to create a new value without arguments
     * @return the singleton object
     * @throws Exception if the callable fails to create a new value
     */
    public T get(Callable<T> callable) throws Exception {
        T result = _value;
        if (isMissing(result)) synchronized(this) {
            result = _value;
            if (isMissing(result)) {
                _value = result = callable.call();
            }
        }
        return result;
    }

    /**
     * Retrieves the singleton object, creating it by calling a provider if it is not created yet. This method uses
     * double-checked locking to ensure that only one instance will ever be created, while keeping retrieval cost at
     * minimum.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Double-checked_locking">Double-checked locking</a>
     *
     * @param <V> the argument type to the functor
     * @param functor a {@link org.xillium.base.Functor Functor} that can be called to create a new value with 1 argument
     * @param argument the argument to pass to the functor
     * @return the singleton object
     * @throws Exception if the functor fails to create a new value
     */
    public <V> T get(Functor<T, V> functor, V argument) throws Exception {
        T result = _value;
        if (isMissing(result)) synchronized(this) {
            result = _value;
            if (isMissing(result)) {
                _value = result = functor.invoke(argument);
            }
        }
        return result;
    }

    /**
     * Retrieves the singleton object, creating it by calling a provider if it is not created yet. This method uses
     * double-checked locking to ensure that only one instance will ever be created, while keeping retrieval cost at
     * minimum.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Double-checked_locking">Double-checked locking</a>
     *
     * @param factory a {@link org.xillium.base.Factory Factory} that can be called to create a new value with more than 1 arguments
     * @param args the arguments to pass to the factory
     * @return the singleton object
     * @throws Exception if the factory fails to create a new value
     */
    public T get(Factory<T> factory, Object... args) throws Exception {
        T result = _value;
        if (isMissing(result)) synchronized(this) {
            result = _value;
            if (isMissing(result)) {
                _value = result = factory.make(args);
            }
        }
        return result;
    }

    /**
     * For performance evaluation only - do not use.
     *
     * @param p a {@link org.xillium.base.Factory Factory} that can be called to create a new value with more than 1 arguments
     * @param args the arguments to pass to the factory
     * @return the singleton object
     * @throws Exception if the factory fails to create a new value
     */
    public synchronized T slow(Factory<T> p, Object... args) throws Exception {
        T result = _value;
        if (isMissing(result)) {
            _value = result = p.make(args);
        }
        return result;
    }

    /**
     * Clears the wrapper. Taking advantage of atomic reference assignment this method requires no thread synchronization.
     *
     * @return the singleton object before the wrapper is cleared
     */
    public T clear() {
        T result = _value;
        _value = null;
        return result;
    }
}
