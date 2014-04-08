package org.xillium.data;


/**
 * A data transformer
 */
public interface Transformer<T, V> {
    public V transform(T object);
}
