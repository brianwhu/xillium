package org.xillium.base.util;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.regex.*;
import javax.management.*;
import org.xillium.base.beans.Beans;


/**
 * Utility class to manipulate deep object properties using a path like notation.
 */
public abstract class Objects {
    private static final Pattern ARRAY_INDEX = Pattern.compile("(.*)\\[([^]]+)\\]");

    /**
     * Reports a property.
     *
     * @param object - the target object
     * @param path - a string array path to the property
     * @param length - the number of components to consider in the path array
     * @return the property value
     */
    public static Object getProperty(Object object, String[] path, int length) throws AttributeNotFoundException {
        try {
            for (int i = 0; i < length; ++i) {
                Matcher matcher = ARRAY_INDEX.matcher(path[i]);
                if (matcher.find()) {
                    object = Array.get(Beans.getKnownField(object.getClass(), matcher.group(1)).get(object), Integer.parseInt(matcher.group(2)));
                } else {
                    object = Beans.getKnownField(object.getClass(), path[i]).get(object);
                }
            }
        } catch (Exception x) {
            throw new AttributeNotFoundException(x.getMessage());
        }

        return object;
    }

    /**
     * Reports a property.
     *
     * @param object - the target object
     * @param path - a string array path to the property
     * @return the property value
     */
    public static Object getProperty(Object object, String[] path) throws AttributeNotFoundException {
        return getProperty(object, path, path.length);
    }

    /**
     * Reports a property.
     *
     * @param object - the target object
     * @param name - a dot-separated string path to the property
     * @return the property value
     */
    public static Object getProperty(Object object, String name) throws AttributeNotFoundException {
        return getProperty(object, name.split("\\."));
    }

    /**
     * Updates a property.
     *
     * @param object - the target object
     * @param name - a dot-separated path to the property
     * @param text - a String representation of the new value
     */
    public static void setProperty(Object object, String name, String text) throws AttributeNotFoundException, BadAttributeValueExpException {
        String[] path = name.split("\\.");
        object = getProperty(object, path, path.length - 1);

        try {
            Matcher matcher = ARRAY_INDEX.matcher(path[path.length - 1]);
            if (matcher.find()) {
                Field field = Beans.getKnownField(object.getClass(), matcher.group(1));
                Array.set(field.get(object), Integer.parseInt(matcher.group(2)), Beans.valueOf(field.getType().getComponentType(), text));
            } else {
                Field field = Beans.getKnownField(object.getClass(), path[path.length - 1]);
                field.set(object, Beans.valueOf(field.getType(), text));
            }
        } catch (Exception x) {
            throw new BadAttributeValueExpException(text);
        }
    }

    /**
     * Concatenates several reference arrays.
     */
    public static <T> T[] concat(T[] first, T[]... rest) {
        int length = first.length;
        for (T[] array : rest) {
            length += array.length;
        }
        T[] result = Arrays.copyOf(first, length);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * Concatenates several int arrays.
     */
    public static int[] concat(int[] first, int[]... rest) {
        int length = first.length;
        for (int[] array : rest) {
            length += array.length;
        }
        int[] result = Arrays.copyOf(first, length);
        int offset = first.length;
        for (int[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
