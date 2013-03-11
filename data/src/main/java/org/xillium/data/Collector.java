package org.xillium.data;

//import java.lang.reflect.*;
//import java.util.*;
//import org.xillium.data.validation.*;


/**
 * An object collector.
 */
public interface Collector<T> {
    /**
     * returns false to stop data collection.
     */
    public boolean add(T object);
}
