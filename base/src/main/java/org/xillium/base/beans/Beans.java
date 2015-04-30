package org.xillium.base.beans;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;
import org.xillium.base.type.typeinfo;
import org.xillium.base.util.ValueOf;


/**
 * A collection of commonly used bean manipulation utilities.
 */
public class Beans {
    /**
     * Tests whether a non-primitive type is directly displayable.
     *
     * @param type the class object to test
     * @return whether the type is displayable
     */
    public static boolean isDisplayable(Class<?> type) {
        return Enum.class.isAssignableFrom(type)
            || type == java.net.URL.class || type == java.io.File.class
            || java.math.BigInteger.class.isAssignableFrom(type)
            || java.math.BigDecimal.class.isAssignableFrom(type)
            || java.util.Date.class.isAssignableFrom(type)
            || java.sql.Date.class.isAssignableFrom(type)
            || java.sql.Time.class.isAssignableFrom(type);
    }

    /**
     * Class lookup by name, which also accepts primitive type names.
     *
     * @param name the full name of the class
     * @return the class object
     * @throws ClassNotFoundException if the class is not found
     */
    public static Class<?> classForName(String name) throws ClassNotFoundException {
        if ("void".equals(name)) return void.class;
        if ("char".equals(name)) return char.class;
        if ("boolean".equals(name)) return boolean.class;
        if ("byte".equals(name)) return byte.class;
        if ("short".equals(name)) return short.class;
        if ("int".equals(name)) return int.class;
        if ("long".equals(name)) return long.class;
        if ("float".equals(name)) return float.class;
        if ("double".equals(name)) return double.class;
        return Class.forName(name);
    }

    /**
     * Tests whether a class is a primitive type. Different from Class.isPrimitive, this method consider
     * the following also as "primitive types".
     * <ul>
     * <li>Wrapper classes of the primitive types
     * <li>Class
     * <li>String
     * </ul>
     *
     * @param type the type to test
     * @return whether the type is primitive
     */
    public static boolean isPrimitive(Class<?> type) {
        return type == Class.class || type == String.class
            || type == Character.class || type == Character.TYPE
            || type == Boolean.class || type == Boolean.TYPE
            || type == Byte.class || type == Byte.TYPE
            || type == Short.class || type == Short.TYPE
            || type == Integer.class || type == Integer.TYPE
            || type == Long.class || type == Long.TYPE
            || type == Float.class || type == Float.TYPE
            || type == Double.class || type == Double.TYPE
            || type == Void.TYPE;
    }

    /**
     * Converts a boxed type to its primitive counterpart.
     *
     * @param type the type to convert
     * @return the primitive counterpart
     */
    public static Class<?> toPrimitive(Class<?> type) {
        if (type.isPrimitive()) {
            return type;
        } else if (type == Boolean.class) {
            return Boolean.TYPE;
        } else if (type == Character.class) {
            return Character.TYPE;
        } else if (type == Byte.class) {
            return Byte.TYPE;
        } else if (type == Short.class) {
            return Short.TYPE;
        } else if (type == Integer.class) {
            return Integer.TYPE;
        } else if (type == Long.class) {
            return Long.TYPE;
        } else if (type == Float.class) {
            return Float.TYPE;
        } else if (type == Double.class) {
            return Double.TYPE;
        } else {
            return null;
        }
    }

    /**
     * Boxes a primitive type.
     *
     * @param type the primitive type to box
     * @return the boxed type
     */
    public static Class<?> boxPrimitive(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == char.class) {
            return Character.class;
        } else {
            return type;
        }
    }

    /**
     * Boxes primitive types.
     *
     * @param types an array of primitive types to box
     * @return an array of boxed types
     */
    public static Class<?>[] boxPrimitives(Class<?>[] types) {
        for (int i = 0; i < types.length; ++i) {
            types[i] = boxPrimitive(types[i]);
        }
        return types;
    }

    /**
     * Returns a known field by name from the given class disregarding its access control setting, looking through
     * all super classes if needed.
     *
     * @param type the class to start with
     * @param name the name of the field
     * @return a Field object representing the known field
     * @throws NoSuchFieldException if the field is not found
     */
    public static Field getKnownField(Class<?> type, String name) throws NoSuchFieldException {
        NoSuchFieldException last = null;
        do {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException x) {
                last = x;
                type = type.getSuperclass();
            }
        } while (type != null);
        throw last;
    }

    /**
     * Returns all known fields in the given class and all its super classes.
     *
     * @param type the class to start with
     * @return an array of Field objects representing all known fields
     * @throws SecurityException if a security manager denies access
     */
    public static Field[] getKnownFields(Class<?> type) throws SecurityException {
        List<Field> fields = new ArrayList<Field>();
        while (type != null) {
            for (Field field: type.getDeclaredFields()) {
                field.setAccessible(true);
                fields.add(field);
            }
            type = type.getSuperclass();
        }
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Returns all known fields in the given class and all its super classes.
     *
     * @param type the class to start with
     * @return an array of Field objects representing all known instance fields
     * @throws SecurityException if a security manager denies access
     */
    public static Field[] getKnownInstanceFields(Class<?> type) throws SecurityException {
        List<Field> fields = new ArrayList<Field>();
        while (type != null) {
            for (Field field: type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                fields.add(field);
            }
            type = type.getSuperclass();
        }
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Overrides access control of an AccessibleObject, facilitating fluent coding style.
     *
     * @param <T> the type of the accessible object
     * @param object the object to start with
     * @return the same object with its access control overridden to allow all access
     * @throws SecurityException if a security manager denies access
     */
    public static <T extends AccessibleObject> T accessible(T object) throws SecurityException {
        object.setAccessible(true);
        return object;
    }

    /**
     * Creates an instance of a given type by choosing the best constructor that matches the given list of arguments.
     *
     * @param <T> the type of the object to create
     * @param type the type of the object to create
     * @param args the arguments to pass to the constructor
     * @return the object created
     * @throws NoSuchMethodException if an appropriate constructor cannot be found
     * @throws IllegalAccessException if an appropriate constructor cannot be accessed
     * @throws InvocationTargetException if errors occur while invoking the constructor
     * @throws InstantiationException if the constructor itself fails in any way
     */
    public static <T> T create(Class<T> type, Object... args) throws
    NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class<?>[] argumentTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argumentTypes[i] = args[i].getClass();
        }
        return type.cast(choose(type.getConstructors(), new ConstructorParameterExtractor<T>(), null, argumentTypes).newInstance(args));
    }

    /**
     * Creates an instance of a given type by choosing the best constructor that matches the given list of arguments.
     *
     * @param <T> the type of the object to create
     * @param type the type of the object to create
     * @param args the arguments to pass to the constructor
     * @param offset the offset into the args array from which to retrieve arguments
     * @param count the number of arguments from the args array to pass to the constructor
     * @return the object created
     * @throws NoSuchMethodException if an appropriate constructor cannot be found
     * @throws IllegalAccessException if an appropriate constructor cannot be accessed
     * @throws InvocationTargetException if errors occur while invoking the constructor
     * @throws InstantiationException if the constructor itself fails in any way
     */
    public static <T> T create(Class<T> type, Object[] args, int offset, int count) throws
    NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (offset != 0 || count != args.length) {
            return create(type, Arrays.copyOfRange(args, offset, offset + count));
        } else {
            return create(type, args);
        }
    }

    /**
     * Invokes a bean method by choosing the best method that matches the given name and the list of arguments.
     *
     * @param bean the invocation target
     * @param name the name of the method to invoke
     * @param args the arguments to pass to the method
     * @param offset the offset into the args array from which to retrieve arguments
     * @param count the number of arguments from the args array to pass to the method
     * @return the return value from the method
     * @throws NoSuchMethodException if an appropriate method cannot be found
     * @throws IllegalAccessException if an appropriate method cannot be accessed
     * @throws InvocationTargetException if errors occur while invoking the method
     */
    public static Object invoke(Object bean, String name, Object[] args, int offset, int count) throws
    NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (offset != 0 || count != args.length) {
            return invoke(bean, name, Arrays.copyOfRange(args, offset, offset + count));
        } else {
            return invoke(bean, name, args);
        }
    }

    /**
     * Invokes a bean method by choosing the best method that matches the given name and the list of arguments.
     *
     * @param bean the invocation target
     * @param name the name of the method to invoke
     * @param args the arguments to pass to the method
     * @return the return value from the method
     * @throws NoSuchMethodException if an appropriate method cannot be found
     * @throws IllegalAccessException if an appropriate method cannot be accessed
     * @throws InvocationTargetException if errors occur while invoking the method
     */
    public static Object invoke(Object bean, String name, Object... args) throws
    NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class<?>[] argumentTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argumentTypes[i] = args[i].getClass();
        }
        return choose(bean.getClass().getMethods(), new MethodParameterExtractor(), name, argumentTypes).invoke(bean, args);
    }

    /**
     * Chooses a method/constructor of a given name, whose signature best matches the given list of argument types.
     *
     * @param <T> the type of the member/constructor to return
     * @param candidates the list of method/constructor candidates to choose from
     * @param pe the ParameterExtractor
     * @param name the name of the method/constructor
     * @param argumentTypes the argument types
     * @return the method/constructor
     * @throws NoSuchMethodException if an appropriate method/constructor cannot be found
     */
    public static <T extends AccessibleObject & Member> T choose(T[] candidates, ParameterExtractor pe, String name, Class<?>[] argumentTypes)
    throws NoSuchMethodException {
        T chosenCandidate = null;
        Class<?>[] chosenParamTypes = null;

        // try to find the most applicable candidate
        Search: for (int i = 0; i < candidates.length; i++) {
//System.err.println("Looking at candidate " + candidates[i]);
            // ignore functions with different name
            if (name != null && !candidates[i].getName().equals(name)) continue;

            // ignore covariance on static candidates
            if (Modifier.isStatic(candidates[i].getModifiers())) continue;

            //Class<?>[] parameterTypes = candidates[i].getParameterTypes();
            Class<?>[] parameterTypes = pe.getParameterTypes(candidates[i]);

            // ignore functions with wrong number of parameters
            if (parameterTypes.length != argumentTypes.length) continue;

            // coerce the primitives to objects
            boxPrimitives(parameterTypes);

            // ignore functions with incompatible parameter types
            for (int j = 0; j < parameterTypes.length; j++) {
                if (!parameterTypes[j].isAssignableFrom(argumentTypes[j])) continue Search;
            }

//System.err.println("Considering candidate " + candidates[i]);

            // if this is the first match then save it
            if (chosenCandidate == null) {
                chosenCandidate = candidates[i];
                chosenParamTypes = parameterTypes;
            } else {
                // if this candidate is more specific in compatibility then save it
                for (int j = 0; j < chosenParamTypes.length; j++) {
                    if (!chosenParamTypes[j].isAssignableFrom(parameterTypes[j])) continue Search;
                }

                // this is the best fit so far
                chosenCandidate = candidates[i];
                chosenParamTypes = parameterTypes;
//System.err.println("Best candidate so far " + chosenCandidate);
            }
        }

        // return to the caller indicating that candidate was not found
        if (chosenCandidate == null) {
            throw(new NoSuchMethodException("Method not found: " + name));
        }

//System.err.println("Chosen candidate is " + chosenCandidate);

        // return the covariant candidate
        return accessible(chosenCandidate); // Java bug #4071957 - have to call setAccessible even on public methods
    }

    private static interface ParameterExtractor {
        public Class<?>[] getParameterTypes(Object object);
    }

    private static class ConstructorParameterExtractor<T> implements ParameterExtractor {
        @SuppressWarnings("unchecked")
        public Class<?>[] getParameterTypes(Object object) {
            return ((Constructor<T>)object).getParameterTypes();
        }
    }

    private static class MethodParameterExtractor implements ParameterExtractor {
        public Class<?>[] getParameterTypes(Object object) {
            return ((Method)object).getParameterTypes();
        }
    }

    /**
     * Assigns a value to the field in an object, converting value type as necessary.
     *
     * @param object an object
     * @param field a field of the object
     * @param value a value to set to the field
     * @throws IllegalArgumentException if the value is not acceptable by the field
     * @throws IllegalAccessException if a security manager denies access
     */
    @SuppressWarnings("unchecked")
    public static void setValue(Object object, Field field, Object value) throws IllegalArgumentException, IllegalAccessException {
        if (value == null) {
            //if (Number.class.isAssignableFrom(field.getType())) {
                //value = java.math.BigDecimal.ZERO;
            //} else return;
            return;
        }

        try {
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalArgumentException x) {
            @SuppressWarnings("rawtypes")
            Class ftype = field.getType();
            if (value instanceof Number) {
                // size of "value" bigger than that of "field"?
                try {
                    Number number = (Number)value;
                    if (Enum.class.isAssignableFrom(ftype)) {
                        field.set(object, ftype.getEnumConstants()[number.intValue()]);
                    } else if (Double.TYPE == ftype || Double.class.isAssignableFrom(ftype)) {
                        field.set(object, number.doubleValue());
                    } else if (Float.TYPE == ftype || Float.class.isAssignableFrom(ftype)) {
                        field.set(object, number.floatValue());
                    } else if (Long.TYPE == ftype || Long.class.isAssignableFrom(ftype)) {
                        field.set(object, number.longValue());
                    } else if (Integer.TYPE == ftype || Integer.class.isAssignableFrom(ftype)) {
                        field.set(object, number.intValue());
                    } else if (Short.TYPE == ftype || Short.class.isAssignableFrom(ftype)) {
                        field.set(object, number.shortValue());
                    } else {
                        field.set(object, number.byteValue());
                    }
                } catch (Throwable t) {
                    throw new IllegalArgumentException(t);
                }
            } else if (value instanceof java.sql.Timestamp) {
                try {
                    field.set(object, new java.sql.Date(((java.sql.Timestamp)value).getTime()));
                } catch (Throwable t) {
                    throw new IllegalArgumentException(t);
                }
            } else if (value instanceof String) {
                field.set(object, new ValueOf(ftype, field.getAnnotation(typeinfo.class)).invoke((String)value));
            } else {
                throw new IllegalArgumentException(x);
            }
        }

    }

    /**
     * Converts a String representation into a value of a given type. Conversion attempts are made in the following order:
     * <ol>
     * <li>Try to invoke {@code public static SomeClass valueOf(String text)}.</li>
     * <li>Try to invoke {@code public static SomeClass valueOf(Class<SomeClass> type, String text)},
     *     or {@code public static SomeClass<T, V, ...> valueOf(Class<T> type1, Class<V> type2, ..., String text)} if a @typeinfo annotation exists to
     *     give argument types {@code T}, {@code V}, ...</li>
     * <li>Try to invoke {@code <init>(String text)}.</li>
     * </ol>
     *
     * @param <T> the target class
     * @param type the target class
     * @param value the string to convert
     * @param annotation an optional typeinfo annotation containing type arguments to {@code type}, which should be a generic type in this case
     * @return the converted value
     * @throws IllegalArgumentException if all conversion attempts fail
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(Class<T> type, String value, typeinfo annotation) {
        if (type.equals(String.class)) {
            return type.cast(value);
        } else {
            try {
                Class<?> boxed = boxPrimitive(type);
                try {
                    return (T)boxed.getMethod("valueOf", String.class).invoke(null, value);
                } catch (NoSuchMethodException x) {
                    try {
                        return (T)boxed.getMethod("valueOf", Class.class, String.class).invoke(null, annotation != null ? annotation.value()[0] : type, value);
                    } catch (NoSuchMethodException y) {
                        return (T)boxed.getConstructor(String.class).newInstance(value);
                    }
                }
            } catch (Exception x) {
                throw new IllegalArgumentException(x.getMessage(), x);
            }
        }
    }

    /**
     * Converts a String representation into a value of a given type. This method calls
     * {@code>valueOf(type, value, null)}.
     *
     * @param <T> the target class
     * @param type the target class
     * @param value the string to convert
     * @return the converted value
     * @throws IllegalArgumentException if all conversion attempts fail
     */
    public static <T> T valueOf(Class<T> type, String value) {
        return valueOf(type, value, null);
    }

    /**
     * Fills empty, identically named public fields with values from another object.
     *
     * @param <T> the class of the destination object
     * @param destination a destination object
     * @param source a source object
     * @return the same destination object with fields filled by source
     */
    public static <T> T fill(T destination, Object source) {
        if (destination != source) {
            Class<?> stype = source.getClass();
            for (Field field: destination.getClass().getFields()) {
                try {
                    Object value = field.get(destination);
                    if (value == null) {
                        field.set(destination, stype.getField(field.getName()).get(source));
                    }
                } catch (Exception x) {
                }
            }
        }
        return destination;
    }

    /**
     * Overrides identically named public fields with non-empty values from another object.
     *
     * @param <T> the class of the destination object
     * @param destination a destination object
     * @param source a source object
     * @return the same destination object with fields overridden by source
     */
    public static <T> T override(T destination, Object source) {
        if (destination != source) {
            Class<?> dtype = destination.getClass();
            for (Field field: source.getClass().getFields()) {
                try {
                    Object value = field.get(source);
                    if (value != null) {
                        dtype.getField(field.getName()).set(destination, value);
                    }
                } catch (Exception x) {
                }
            }
        }
        return destination;
    }

    /**
     * A convenience utility method to convert a bean to a formatted string.
     *
     * @param bean an object
     * @return a string representation of the object
     */
    public static String toString(Object bean) {
        try {
            return bean != null ? print(new StringBuilder(), bean, 0).toString() : null;
        } catch (IntrospectionException x) {
            return bean.toString() + "(***" + x.getMessage() + ')';
        }
    }

    /**
     * Prints a bean to the StringBuilder.
     *
     * @param sb a StringBuilder
     * @param bean an object
     * @param level indentation level
     * @return the original StringBuilder
     * @throws IntrospectionException if bean introspection fails by {@link java.beans.Introspector Introspector}
     */
    public static StringBuilder print(StringBuilder sb, Object bean, int level) throws IntrospectionException {
        return print(sb, Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()), bean, level);
    }

    private static StringBuilder print(StringBuilder sb, Set<Object> objects, Object bean, int level) throws IntrospectionException {
        // reference loop detection
        if (objects.contains(bean)) {
            indent(sb, level+1);
            sb.append("<reference>: ").append(bean.getClass().getName()).append('@').append(Integer.toHexString(bean.hashCode())).append('\n');
            return sb;
        } else {
            objects.add(bean);
        }

        Class<?> type = bean.getClass();

        if (isPrimitive(type) || isDisplayable(type)) {
            indent(sb, level);
            sb.append(bean);
        } else {

            // public fields including those declared by super classes
            for (Field field: type.getFields()) {
                int modifier = field.getModifiers();
                if (!Modifier.isStatic(modifier) && !Modifier.isTransient(modifier)) {
                    try {
                        indent(sb, level);
                        printNameValue(sb, objects, field.getName(), field.get(bean), level+1);
                    } catch (IllegalAccessException x) {}
                }
            }

            // properties
            if (Map.class.isInstance(bean)) {
                Iterator<?> it = ((Map<?, ?>)bean).keySet().iterator();
                while (it.hasNext()) {
                    Object key = it.next();
                    indent(sb, level);
                    printNameValue(sb, objects, key.toString(), ((Map<?, ?>)bean).get(key), level+1);
                }
            } else if (Iterable.class.isInstance(bean)) {
                Iterator<?> it = ((Iterable<?>)bean).iterator();
                int index = 0;
                while (it.hasNext()) {
                    indent(sb, level+1);
                    printNameValue(sb, objects, "[" + index + "]", it.next(), level+1);
                    ++index;
                }
            } else if (type.isArray()) {
                for (int i = 0; i < Array.getLength(bean); ++i) {
                    indent(sb, level+1);
                    printNameValue(sb, objects, "[" + i + "]", Array.get(bean, i), level+1);
                }
            } else {
                PropertyDescriptor[] properties = Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors();
                for (PropertyDescriptor property : properties) {
                    Object value = null;
                    Class<?> ptype = property.getPropertyType();
                    if (ptype != null) {
                        try {
                            value = accessible(property.getReadMethod()).invoke(bean); // Java bug #4071957
                        } catch (Exception x) {
                            value = x.getMessage();
                        }
                        indent(sb, level);
                        printNameValue(sb, objects, property.getDisplayName() + '<' + ptype.getName() + '>', value, level);
                    } else {
                        try {
                            Method reader = accessible(((IndexedPropertyDescriptor)property).getIndexedReadMethod()); // Java bug #4071957
                            for (int i = 0; ; ++i) {
                                value = reader.invoke(bean, i);
                                indent(sb, level);
                                printNameValue(sb, objects, property.getDisplayName() + '[' + i + ']', value, level);
                            }
                        } catch (Exception x) {}
                    }
                }
            }
        }

        return sb;
    }

    private static void printNameValue(StringBuilder sb, Set<Object> objects, String name, Object value, int level) {
        if (value == null) {
            sb.append(name).append(":\n");
        } else if (isPrimitive(value.getClass()) || isDisplayable(value.getClass())) {
            sb.append(name).append(": ").append(value).append('\n');
        } else {
            //sb.append(name).append(": {\n");
            sb.append(name).append(": ").append(value.getClass().getName()).append('@').append(Integer.toHexString(value.hashCode())).append(" {\n");
                try {
                    print(sb, objects, value, level+1);
                } catch (IntrospectionException x) {
                    indent(sb, level+1);
                    sb.append("!error! ").append(x.getMessage());
                }
            indent(sb, level);
            sb.append("}\n");
        }
    }

    private static void indent(StringBuilder sb, int level) {
        for(int i = 0; i < level*INDENTATION; ++i) sb.append(' ');
    }

    private static int INDENTATION = 2;
}
