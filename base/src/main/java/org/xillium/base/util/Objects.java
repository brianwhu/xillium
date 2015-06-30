package org.xillium.base.util;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.*;
import org.xillium.base.Functor;
import org.xillium.base.beans.Beans;
import org.xillium.base.type.typeinfo;


/**
 * Utility class to manipulate deep object properties using a path like notation.
 */
public abstract class Objects {
    private static final Pattern ARRAY_INDEX = Pattern.compile("(.*)\\[([^]]+)\\]");

    /**
     * Reports a property.
     *
     * @param object the target object
     * @param name a dot-separated path to the property
     * @return the property value
     * @throws NoSuchFieldException if the property is not found
     */
    public static Object getProperty(Object object, String name) throws NoSuchFieldException {
        try {
            Matcher matcher = ARRAY_INDEX.matcher(name);
            if (matcher.matches()) {
                object = getProperty(object, matcher.group(1));
                if (object.getClass().isArray()) {
                    return Array.get(object, Integer.parseInt(matcher.group(2)));
                } else {
                    return ((List)object).get(Integer.parseInt(matcher.group(2)));
                }
            } else {
                int dot = name.lastIndexOf('.');
                if (dot > 0) {
                    object = getProperty(object, name.substring(0, dot));
                    name = name.substring(dot + 1);
                }
                return Beans.getKnownField(object.getClass(), name).get(object);
            }
        } catch (NoSuchFieldException x) {
            throw x;
        } catch (Exception x) {
            throw new IllegalArgumentException(x.getMessage() + ": " + name, x);
        }
    }

    /**
     * Updates a property.
     *
     * @param object the target object
     * @param name a dot-separated path to the property
     * @param text a String representation of the new value
     * @throws NoSuchFieldException if the property is not found
     * @throws IllegalArgumentException if the value expression is invalid or can't be assigned to the property
     */
    @SuppressWarnings("unchecked")
    public static void setProperty(Object object, String name, String text) throws NoSuchFieldException {
        try {
            // need to get to the field for any typeinfo annotation, so here we go ...
            int length = name.lastIndexOf('.');
            if (length > 0) {
                object = getProperty(object, name.substring(0, length));
                name = name.substring(length + 1);
            }
            length = name.length();

            Matcher matcher = ARRAY_INDEX.matcher(name);
            for (Matcher m = matcher; m.matches(); m = ARRAY_INDEX.matcher(name)) {
                name = m.group(1);
            }
            Field field = Beans.getKnownField(object.getClass(), name);

            if (name.length() != length) {
                int index = Integer.parseInt(matcher.group(2));
                object = getProperty(object, matcher.group(1));
                if (object.getClass().isArray()) {
                    Array.set(object, index, new ValueOf(object.getClass().getComponentType(), field.getAnnotation(typeinfo.class)).invoke(text));
                } else {
                    ((List)object).set(index, new ValueOf(field.getAnnotation(typeinfo.class).value()[0]).invoke(text));
                }
            } else {
                field.set(object, new ValueOf(field.getType(), field.getAnnotation(typeinfo.class)).invoke(text));
            }
        } catch (NoSuchFieldException x) {
            throw x;
        } catch (Exception x) {
            throw new IllegalArgumentException(x.getMessage() + ": " + name, x);
        }
    }

    /**
     * Concatenates several reference arrays.
     *
     * @param <T> the type of the elements in the array
     * @param first the first array
     * @param rest the rest of the arrays
     * @return a single array containing all elements in all arrays
     */
    @SafeVarargs
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
     *
     * @param first the first array
     * @param rest the rest of the arrays
     * @return a single array containing all elements in all arrays
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

    /**
     * Appends more elements into an array to form a new array.
     */
//  public static <T> T[] append(T[] array, T... elements) {
//      T[] result = Arrays.copyOf(array, array.length + elements.length);
//      System.arraycopy(elements, 0, result, array.length, elements.length);
//      return result;
//  }

    /**
     * Stores array elements along with more individuals into a new Object[].
     *
     * @param <T> the type of the elements in the array
     * @param array an array
     * @param elements a list of elements
     * @return an {@code Object} array containing all elements
     */
    public static <T> Object[] store(T[] array, Object... elements) {
        Object[] result = new Object[array.length + elements.length];
        System.arraycopy(array, 0, result, 0, array.length);
        System.arraycopy(elements, 0, result, array.length, elements.length);
        return result;
    }

    /**
     * Invokes a functor over all elements in an array, and returns the results in a provided array.
     * <code>
     *  result = functor(array)
     * </code>
     *
     * @param <R> the result type of the functor
     * @param <T> the type of the elements in the array
     * @param result an array to store functor return values
     * @param array an array
     * @param functor a functor
     * @return an array containing invocation results
     */
    public static <R, T> R[] apply(R[] result, T[] array, Functor<R, T> functor) {
        for (int i = 0; i < array.length; ++i) result[i] = functor.invoke(array[i]);
        return result;
    }

    /**
     * Parses an array of Strings as int's, and returns the produced values in a new array.
     */
//  public static int[] parse(String[] text) {
//      int[] array = new int[text.length];
//      for (int i = 0; i < array.length; ++i) {
//          array[i] = Integer.parseInt(text[i]);
//      }
//      return array;
//  }
}
