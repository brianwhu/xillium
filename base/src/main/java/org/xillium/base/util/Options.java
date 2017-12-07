package org.xillium.base.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
import org.xillium.base.type.typeinfo;


/**
 * A simple command line options parser that interprets command line options based on the structure of a provided Java object.
 */
public class Options<T> {
    /**
     * Annotation to provide a description to an option item. A field must be annotated by {@code @Option.description}
     * to be recognized as a command line option. Typical uses include
     * <pre>{@code
     * public class Setting {
     *     @Option.description("Specify an additional class path") public String classpath;
     * }
     * }</pre>
     */
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface description {
        String value();
    }

    /**
     * Annotation to provide a placeholder to an option item.
     * Typical uses include
     * <pre>{@code
     * public class Setting {
     *     @Option.placeholder("additional-class-path-to-include") public String classpath;
     * }
     * }</pre>
     */
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface placeholder {
        String value();
    }

    public static enum Unrecognized {
        ARGUMENT,
        VALUE
    }

    private static final String  STOPPER = "--";
    private static final Pattern SOPTION = Pattern.compile("-([0-9A-Za-z]+)");
    private static final Pattern LOPTION = Pattern.compile("--(\\p{Alpha}[\\w-]*)(?:=(.*))?");

    private final T _options;
    private final Class<?> _prototype;
    private final Map<Character, Field> _booleans = new TreeMap<>();

    /**
     * Constructs an Options parser bound to a receiving object. Public fields of the object that are annotated by
     * {@code @Option.description} are recognized as command line options.
     *
     * @param options the Java object whose public data members are to be updated by command line arguments
     */
    public Options(T options) {
        this(options, options.getClass());
    }

    /**
     * Constructs an Options parser bound to a receiving object.
     *
     * @param options the Java object whose public data members are to be updated by command line arguments
     * @param prototype a Class object
     */
    public Options(T options, Class<?> prototype) {
        _options = options;
        _prototype = prototype;

        Set<Character> set = new HashSet<>();
        for (Field field: Beans.getKnownInstanceFields(prototype)) {
            if (field.getAnnotation(description.class) != null && (field.getType() == Boolean.class || field.getType() == Boolean.TYPE)) {
                char key = getShortOptionKey(field);
                // if any keys clash, reject all of them
                if (!set.contains(key)) {
                    set.add(key);
                    _booleans.put(key, field);
                } else {
                    _booleans.remove(key);
                }
            }
        }
    }

    /**
     * Writes usage documentation to an Appendable.
     * @param output an Appendable to write the usage to
     * @return the same Appendable
     */
    public <A extends Appendable> A document(A output) throws IOException {
        StringBuilder line = new StringBuilder();
        for (Field field: Beans.getKnownInstanceFields(_prototype)) {
            description d = field.getAnnotation(description.class);
            if (d == null) continue;

            placeholder p = field.getAnnotation(placeholder.class);
            String n = Strings.splitCamelCase(field.getName(), "-").toLowerCase();
            char key = getShortOptionKey(field);
            if ((field.getType() == Boolean.class || field.getType() == Boolean.TYPE) && _booleans.containsKey(key)) {
                line.append("  -").append(key).append(", --").append(n);
            } else {
                line.append("  --").append(n).append('=').append(p != null ? p.value() : "value");
            }
                if (line.length() < 16) {
                    for (int i = line.length(); i < 16; ++i) line.append(' ');
                    line.append(d.value());
                } else {
                    line.append("\n\t\t").append(d.value());
                }
            output.append(line.toString()).append('\n');
            line.setLength(0);
        }

        return output;
    }

    /**
     * Parses an argument list starting at the given index, populating the bound options object with the information collected.
     * Optional arguments are recognized as words starting with "-", at least 2 characters long, and not equal to "--". The
     * special argument "--" is recognized as a stop sign at which the parsing process stops after consuming this argument.
     *
     * @param args
     * @param index
     * @param invalid a list to collect unrecognized arguments or values
     * @return the stopping index - either the first index at which the argument is not an option, or args.length
     */
    @SuppressWarnings("unchecked")
    public int parse(String[] args, int index, List<Pair<Unrecognized, String>> invalid) {
        Matcher matcher = null;
        while (index < args.length && args[index].charAt(0) == '-' && args[index].length() > 1) {
            if (args[index].equals(STOPPER)) {
                ++index;
                break;
            } else {
                if ((matcher = SOPTION.matcher(args[index])).matches()) {
                    for (char letter: matcher.group(1).toCharArray()) {
                        char key = Character.toLowerCase(letter);
                        Field field = _booleans.get(key);
                        if (field != null && field.getAnnotation(description.class) != null) {
                            try {
                                field.set(_options, Character.isLowerCase(letter));
                            } catch (IllegalAccessException x) {
                                report(String.valueOf(letter), Unrecognized.ARGUMENT, invalid);
                            }
                        } else {
                            report(String.valueOf(letter), Unrecognized.ARGUMENT, invalid);
                        }
                    }
                } else if ((matcher = LOPTION.matcher(args[index])).matches()) {
//System.out.println("LOPTION: " + args[index]);
                    String param = matcher.group(1);
                    String value = matcher.group(2);
//System.out.println("groups = " + matcher.groupCount());
//System.out.println("value = " + value);
                    try {
                        Field field = Beans.getKnownField(_prototype, Strings.toLowerCamelCase(param, '-'));
                        if (field.getAnnotation(description.class) == null) {
                            throw new NoSuchFieldException(param);
                        } else if (value != null) { // allowing arguments like "--data=" if empty value is okay with the data object
                            if (Collection.class.isAssignableFrom(field.getType())) {
                                Class<?> elementType = field.getAnnotation(typeinfo.class).value()[0];
                                ((Collection)field.get(_options)).add(new ValueOf(elementType).invoke(value));
                            } else {
                                field.set(_options, new ValueOf(field.getType(), field.getAnnotation(typeinfo.class)).invoke(value));
                            }
                        } else {
                            field.set(_options, Boolean.TRUE);
                        }
                    } catch (NullPointerException x) {
                        throw new IllegalArgumentException("Missing @typeinfo on Collection: " + param);
                    } catch (NoSuchFieldException|IllegalAccessException x) {
                        report(args[index], Unrecognized.ARGUMENT, invalid);
                    } catch (IllegalArgumentException x) {
//x.printStackTrace(System.err);
                        report(args[index], Unrecognized.VALUE, invalid);
                    }
                } else {
                    report(args[index], Unrecognized.ARGUMENT, invalid);
                }
                ++index;
            }
        }
        return index;
    }

    public T get() {
        return _options;
    }

    private char getShortOptionKey(Field field) {
        return Character.toLowerCase(field.getName().charAt(0));
    }

    private void report(String option, Unrecognized kind, List<Pair<Unrecognized, String>> invalid) {
        if (invalid != null) {
            invalid.add(new Pair<Unrecognized, String>(kind, option));
        } else {
            throw new IllegalArgumentException(option);
        }
    }
}
