package org.xillium.data;


/**
 * An object collector.
 */
public interface Collector<T> {
    /**
     * returns false to stop data collection.
     *
     * @param object the object to add to the collection
     * @return whether the collection should continue
     */
    public boolean add(T object);
}
