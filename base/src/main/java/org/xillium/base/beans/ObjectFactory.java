package org.xillium.base.beans;

import java.lang.reflect.InvocationTargetException;

/**
 * A generic object factory that creates objects from class names and arguments.
 */
public interface ObjectFactory {
    /**
     * Creates a new object of a given class with arguments.
     *
     * @param className the name of the object class
     * @param args the arguments to pass to the constructor
     * @return the created object
     * @throws Exception if object construction fails
     */
    public Object create(String className, Object... args) throws Exception;
}
