package org.xillium.base.etc;


/**
 * A pair of members packed in a single object.
 */
@Deprecated
public class Pair<T, V> {
    public T first;
    public V second;

    public Pair(T f, V s) {
        first = f;
        second = s;
    }
}
