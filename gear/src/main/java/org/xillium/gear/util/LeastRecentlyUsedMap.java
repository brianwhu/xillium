package org.xillium.gear.util;

import java.util.Map;
import java.util.LinkedHashMap;
import org.xillium.core.management.WithCache;


/**
 * A map with a maximum capacity and the LRU replacement policy.
 */
public class LeastRecentlyUsedMap<K, V> extends LinkedHashMap<K, V> {
    private final int _limit;
    private long _get, _hit, _rep;
    private int _max;

    /**
     * Constructs a LeastRecentlyUsedMap with the given capacity limit and a load factor of 0.75.
     */
    public LeastRecentlyUsedMap(int limit) {
        this(limit, 0.75f);
    }

    /**
     * Constructs a LeastRecentlyUsedMap with the given capacity limit and load factor.
     */
    public LeastRecentlyUsedMap(int limit, float load) {
        super((int)Math.ceil(limit/load), load, true);
        _limit = limit;
    }

    /**
     * Reports the cumulative statistics of this cache.
     */
    public WithCache.CacheState getCacheState() {
        return new WithCache.CacheState(size(), _max, _get, _hit, _rep);
    }

    @Override
    public V get(Object key) {
        ++_get;
        V value = super.get(key);
        if (value != null) ++_hit;
        return value;
    }

    @Override
    public V put(K key, V value) {
        V old = super.put(key, value);
        if (size() > _max) _max = size();
        return old;
    }

    @Override
    public V remove(Object key) {
        V old = super.remove(key);
        ++_rep;
        return old;
    }

    /**
     * Invalidates an entry in the cache. Compared to "remove()", this operation does not constitute a cache replacement.
     */
    public V invalidate(Object key) {
        return super.remove(key);
    }

    @Override
    public void clear() {
        super.clear();
        _get = _hit = _max = 0;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > _limit;
    }

    static final long serialVersionUID = -5226569682411174431L;
}
