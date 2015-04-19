package org.xillium.base;

import java.util.concurrent.Callable;


/**
 * A thread-safe, lazily-initialized singleton wrapper with double-checked locking.
 */
public class Singleton<T> {
    private volatile T _value;

    /**
     * Retrieves the singleton object, creating it by calling the provider if it is not created yet. This method uses
     * double-checked locking to ensure that only one instance will ever be created, while keeping retrieval cost at
     * minimum.
     *
     * @see http://en.wikipedia.org/wiki/Double-checked_locking
     *
     * @param provider - a Callable that can be called to create a new value
     * @return the singleton object
     * @throws Exception if the provider fails to create a new value
     */
    public T get(Callable<T> provider) throws Exception {
        T result = _value;
        if (result == null) synchronized(this) {
            result = _value;
            if (result == null) {
                _value = result = provider.call();
            }
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
