package org.xillium.base.util;

import java.util.*;


/**
 * A multimap implemented as a map of lists.
 */
public class Multimap<K, V> extends HashMap<K, List<V>> {
    public Multimap() {
    }

    public Multimap(int initialCapacity) {
        super(initialCapacity);
    }

    public Multimap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public void add(K key, V value) {
        List<V> list = get(key);
        if (list == null) {
            put(key, list = new ArrayList<V>());
        }
        list.add(value);
    }

    public void add(K key, Collection<V> values) {
        List<V> list = get(key);
        if (list == null) {
            put(key, list = new ArrayList<V>());
        }
        list.addAll(values);
    }
}
