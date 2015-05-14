package org.xillium.gear.util;

import java.util.concurrent.Callable;
import org.xillium.base.*;
import org.xillium.core.management.WithCache;


/**
 * LeastRecentlyUsedCache is a thread-safe cache with a maximum capacity and the LRU replacement policy.
 */
public class LeastRecentlyUsedCache<K, V> {
    private final LeastRecentlyUsedMap<K, Singleton<V>> _cache;

    /**
     * Constructs a LeastRecentlyUsedCache with the given capacity limit and a load factor of 0.75.
     *
     * @param limit the cache limit
     */
    public LeastRecentlyUsedCache(int limit) {
        _cache = new LeastRecentlyUsedMap<K, Singleton<V>>(limit);
    }

    /**
     * Constructs a LeastRecentlyUsedCache with the given capacity limit and load factor.
     *
     * @param limit the cache limit
     * @param load the load factor
     */
    public LeastRecentlyUsedCache(int limit, float load) {
        _cache = new LeastRecentlyUsedMap<K, Singleton<V>>(limit, load);
    }

    /**
     * Reports the cumulative statistics of this cache.
     *
     * @return the current cache state encapsulated in a WithCache.CacheState object
     */
    public WithCache.CacheState getCacheState() {
        return _cache.getCacheState();
    }

    /**
     * Fetches an object from the cache. Synchronization at cache level is kept to minimum. The provider is called upon cache miss.
     *
     * @param key the identity of the object
     * @param callable a callable to provide the object upon cache miss
     * @return the object requested
     * @throws Exception if the provider fails to create a new object
     */
    public V fetch(K key, Callable<V> callable) throws Exception {
        return locate(key).get(callable);
    }

    /**
     * Fetches an object from the cache. Synchronization at cache level is kept to minimum. The provider is called upon cache miss.
     *
     * @param key the identity of the object
     * @param functor a functor to provide the object upon cache miss
     * @param argument the sole argument to pass to the functor
     * @return the object requested
     * @throws Exception if the provider fails to create a new object
     */
    public <T> V fetch(K key, Functor<V, T> functor, T argument) throws Exception {
        return locate(key).get(functor, argument);
    }

    /**
     * Fetches an object from the cache. Synchronization at cache level is kept to minimum. The provider is called upon cache miss.
     *
     * @param key the identity of the object
     * @param factory a factory to provide the object upon cache miss
     * @param args arguments to pass to the factory
     * @return the object requested
     * @throws Exception if the provider fails to create a new object
     */
    public V fetch(K key, Factory<V> factory, Object... args) throws Exception {
        return locate(key).get(factory, args);
    }

    /**
     * Invalidates an entry in the cache. This operation does not constitute a cache replacement.
     *
     * @param key the identity of the object
     * @return the matching object that is being discarded, if any
     */
    public synchronized V invalidate(K key) {
        Singleton<V> singleton = _cache.get(key);
        if (singleton != null) {
            return singleton.clear();
        } else {
            return null;
        }
    }

    private synchronized final Singleton<V> locate(K key) {
        Singleton<V> singleton = _cache.get(key);
        if (singleton == null) {
            _cache.put(key, singleton = new Singleton<V>());
        }
        return singleton;
    }
}
