package org.xillium.base.beans;

import java.lang.reflect.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class DefaultObjectFactory implements ObjectFactory {
    /**
     * Creates a new object of a given class with arguments.
     */
    public Object create(String name, Object[] args)
    throws ClassNotFoundException,
           NoSuchMethodException,
           IllegalAccessException,
           InstantiationException,
           InvocationTargetException
    {
//StringBuilder sb = new StringBuilder("DefaultObjectFactory.create(").append(name).append(',');
//for (Object arg: args) {
//sb.append(arg.getClass().getName()).append(' ');
//}
//sb.append(')');
//System.err.println(sb);
        try {
            Class type = Class.forName(name);
/*
            Constructor<?>[] constructors = type.getConstructors();
            for (Constructor<?> constructor: constructors) {
                Class<?>[] ptypes = constructor.getParameterTypes();
                if (ptypes.length == args.length) {
                    try {
                        return constructor.newInstance(args);
                    } catch (IllegalArgumentException x) {
                        // ignore and try the next one
                    } catch (IllegalAccessException x) {
                        throw x;
                    } catch (InstantiationException x) {
                        throw x;
                    } catch (InvocationTargetException x) {
                        throw x;
                    }
                }
            }
            throw new NoSuchMethodException("Can't locate a desired constructor in " + name);
*/
//System.err.println("DefaultObjectFactory.create: successful");
            return Beans.create(type, args);
        } catch (ClassNotFoundException x) {
            throw x;
        }
    }
}
