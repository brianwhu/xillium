package org.xillium.base.beans;

import java.lang.reflect.*;
import java.util.Map;
import java.util.HashMap;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class BurnedInArgumentsObjectFactory extends DefaultObjectFactory {
    private Map<Class<?>, Object[]> _arguments = new HashMap<Class<?>, Object[]>();
    private Map<Class<?>, Object> _singleton = new HashMap<Class<?>, Object>();

    public BurnedInArgumentsObjectFactory() {
    }

    public BurnedInArgumentsObjectFactory(Class<?> type, Object... burnedin) {
        _arguments.put(type, burnedin);
    }

    public BurnedInArgumentsObjectFactory setBurnedIn(Class<?> type, Object... burnedin) {
        _arguments.put(type, burnedin);
        return this;
    }

    public BurnedInArgumentsObjectFactory setSingleton(Class<?> type, Object singleton) {
        _singleton.put(type, singleton);
        return this;
    }

    /**
     * Creates a new object of a given class with arguments.
     */
    public Object create(String name, Object... args)
    throws ClassNotFoundException,
           NoSuchMethodException,
           IllegalAccessException,
           InstantiationException,
           InvocationTargetException
    {
        try {
            return super.create(name, args);
        } catch (NoSuchMethodException x) {
            Class<?> type = Class.forName(name);
            Object singleton = _singleton.get(name);

            if (singleton != null) {
                return singleton;
            } else {
                Object[] burnedin = _arguments.get(type);
                if (burnedin != null) {
                    Object[] nargs = new Object[burnedin.length + args.length];
                    System.arraycopy(burnedin, 0, nargs, 0, burnedin.length);
                    System.arraycopy(args, 0, nargs, burnedin.length, args.length);
                    return Beans.create(type, nargs);
                } else {
                    throw x;
                }
            }
        }
    }
}
