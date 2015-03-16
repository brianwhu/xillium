package org.xillium.base.util;

import java.lang.reflect.*;
import java.util.Arrays;
import org.xillium.base.Functor;
import org.xillium.base.beans.Beans;
import org.xillium.base.type.typeinfo;


/**
 * A ValueOf is a utility to converts a String representation into a value of a given type. Conversion is
 * possible if the type defines one of the following.
 * <ol>
 * <li>a static method <code>valueOf(String text)</code></li>
 * <li>a static method <code>valueOf(Class<?>... types, String text)</code></li>
 * <li>a constructor <code>&lt;init&gt;(String text)</code></li>
 * </ol>
 * If none of these mechanisms are available, construction of ValueOf fails with an IllegalArgumentException.
 */
public class  ValueOf implements Functor<Object, String> {
    private Class<?>[] _args;
    private Method _valueOf;
    private Constructor<?> _init;

    /**
     * Constructs a ValueOf on a simple type.
     *
     * @param type - the target class
     * @throws IllegalArgumentException if the type does not support conversion from String texts
     */
    public ValueOf(Class<?> type) {
        this(type, (Class<?>[])null);
    }

    /**
     * Constructs a ValueOf on a parametric type.
     *
     * @param type - the target class
     * @param info - a typeinfo annotation that maintains an array of type arguments bound to the parametric type
     * @throws IllegalArgumentException if the type does not support conversion from String texts
     */
    public ValueOf(Class<?> type, typeinfo info) {
        this(type, info != null ? info.value() : null);
    }

    /**
     * Constructs a ValueOf on a parametric type.
     *
     * @param type - the target class
     * @param args - an array of type arguments bound to the parametric type
     * @throws IllegalArgumentException if the type does not support conversion from String texts
     */
    public ValueOf(Class<?> type, Class<?>[] args) {
        if (!type.equals(String.class)) {
            _args = args;

            try {
                Class<?> boxed = Beans.boxPrimitive(type);
                try {
                    if (_args != null) {
                        Class<?>[] types = new Class<?>[_args.length + 1];
                        Arrays.fill(types, Class.class);
                        types[_args.length] = String.class;
                        _valueOf = boxed.getMethod("valueOf", types);
                    } else {
                        _valueOf = boxed.getMethod("valueOf", String.class);
                    }
                } catch (NoSuchMethodException x) {
                    _init = boxed.getConstructor(String.class);
                }
            } catch (NoSuchMethodException x) {
                throw new IllegalArgumentException(x.getMessage(), x);
            }
        }
    }

    /**
     * Reports whether the type associated with this ValueOf is String or not.
     */
    public boolean isString() {
        return _valueOf == null && _init == null;
    }

    /**
     * Converts a text into a value.
     *
     * Note: If the text is an empty string, it is converted to
     * <ul>
     * <li>Empty string "", if the associated type is String
     * <li>null, otherwise
     * </li>
     *
     * @param text - the string to convert
     * @throws IllegalArgumentException if the text cannot be converted into a value
     */
    public Object invoke(String text) {
        try {
            if (text == null || text.length() == 0) {
                return (_valueOf != null || _init != null) ? null : text;
            } else if (_valueOf != null) {
                return _valueOf.getParameterTypes().length == 1 ? _valueOf.invoke(null, text) : _valueOf.invoke(null, Objects.store(_args, text));
            } else if (_init != null) {
                return _init.newInstance(text);
            } else {
                return text;
            }
        } catch (Exception x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }
}
