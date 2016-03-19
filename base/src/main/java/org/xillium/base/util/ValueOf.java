package org.xillium.base.util;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.xillium.base.Functor;
import org.xillium.base.beans.Beans;
import org.xillium.base.type.typeinfo;


/**
 * A ValueOf is a utility to convert a String representation into a value of a given type. Conversion is
 * possible if the type defines one of the following.
 * <ol>
 * <li>a static method {@code valueOf(String text)}</li>
 * <li>a static method {@code valueOf(Class<?>... types, String text)}</li>
 * <li>a constructor {@code <init>(String text)}</li>
 * </ol>
 * If none of these mechanisms are available, construction of ValueOf fails with an IllegalArgumentException.
 * <p>
 * If the given type is an array, the String reprensentation is first split around commas and then converted
 * to the element type of the array.
 */
public class ValueOf implements Functor<Object, String> {
    public static final Pattern ARRAY_VALUE_SEPARATOR = Pattern.compile(" *, *");

    private final boolean _isArray;
    private Class<?>[] _args;
    private Method _valueOf;
    private Constructor<?> _init;

    /**
     * Constructs a ValueOf on a simple type, or an array type with elements of the given type.
     *
     * @param type the target class
     * @throws IllegalArgumentException if the type does not support conversion from String texts
     */
    public ValueOf(Class<?> type) {
        this(type, (Class<?>[])null);
    }

    /**
     * Constructs a ValueOf on a parametric type, or an array type with elements of the given type.
     *
     * @param type the target class
     * @param info a typeinfo annotation that maintains an array of type arguments bound to the parametric type
     * @throws IllegalArgumentException if the type does not support conversion from String texts
     */
    public ValueOf(Class<?> type, typeinfo info) {
        this(type, info != null ? info.value() : null);
    }

    /**
     * Constructs a ValueOf on a parametric type, or an array type with elements of the given type.
     *
     * @param type the target class
     * @param args an array of type arguments bound to the parametric type
     * @throws IllegalArgumentException if the type does not support conversion from String texts
     */
    public ValueOf(Class<?> type, Class<?>[] args) {
        _isArray = type.isArray();
        if (_isArray) {
            type = type.getComponentType();
        }
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
     *
     * @return whether the type associated with this ValueOf is String or not
     */
    public boolean isString() {
        return _valueOf == null && _init == null;
    }

    /**
     * Converts a text into a value.
     * <p>
     * Note: If the text is an empty string, it is converted to
     * <ul>
     * <li>Empty string "", if the associated type is String</li>
     * <li>null, otherwise</li>
     * </ul>
     *
     * @param text the string to convert
     * @return the converted object
     * @throws IllegalArgumentException if the text cannot be converted into a value
     */
    public Object invoke(String text) {
        if (_isArray) {
            if (text == null || text.length() == 0) {
                return null;
            } else {
                String[] texts = ARRAY_VALUE_SEPARATOR.split(text);
                Class<?> type = _valueOf != null ? _valueOf.getReturnType() : (_init != null ? _init.getDeclaringClass() : String.class);
                Object array = Array.newInstance(type, texts.length);
                for (int i = 0; i < texts.length; ++i) {
                    Array.set(array, i, convert(texts[i]));
                }
                return array;
            }
        } else {
            return convert(text);
        }
    }

    private Object convert(String text) {
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
