package org.xillium.base.beans;

import java.lang.reflect.InvocationTargetException;

/**
 * A generic object factory that creates objects from class names and arguments.
 */
public interface ObjectFactory {
    /**
     * Creates a new object of a given class with arguments.
     */
    public Object create(String className, Object... args)
    throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException;
}
