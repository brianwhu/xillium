package org.xillium.data;

//import java.lang.reflect.*;
//import java.util.*;
//import org.xillium.data.validation.*;


/**
 * An object collector.
 */
public interface Collector<T> {
    public boolean add(T object);
}
