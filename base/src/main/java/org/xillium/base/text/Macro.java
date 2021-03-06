package org.xillium.base.text;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

import org.xillium.base.*;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
import org.xillium.base.util.Objects;
import org.xillium.base.util.ValueOf;


/**
 * This is a macro processor that handles 2 types of macro expansions in a text markup.
 * <ol>
 * <li>Parameter expansion - see {@link #expand(String, Pattern, Functor, String[]) Macro.expand(String, Pattern, Functor, String[])} and
 *     {@link #expand(String, Object, String[]) Macro.expand(String, Object, String[])}</li>
 * <li>Reference expansion - insertion of another markup snippet with its own embedded parameters and references</li>
 * </ol>
 * <p>
 * Dynamic data are provided by either a functor or an open object.
 * </p>
 */
public class Macro {
    private static final Pattern PARAMETER = Pattern.compile("\\{\\{([^{}@:-]+)(?::-([^{}@]+))?\\}\\}");
    private static final Pattern REFERENCE = Pattern.compile("\\{([^{}@]+)?@([^{}@]+)@([^{}@]+)?\\}");

    /**
     * Expands a text markup by resolving embedded parameters and references to other text markups, with the help of a companion
     * {@code Open} object.
     * <p>
     * A reference pointing to another markup in the resources is marked up as {@code {PREFIX@MARKUP(ARGS):MEMBER:ALTERN(ARGS)@SUFFIX}},
     * where</p>
     * <ul>
     * <li>{@code PREFIX} and {@code SUFFIX} are optional pieces of text to be placed before and after the markup insertion.</li>
     * <li>{@code MARKUP} is a required element, which gives the name of the markup to be expanded recursively.</li>
     * <li>{@code MEMBER} identifies the companion object for the recursive expansion of the markup. This can be
     *     <ol>
     *       <li>A method invocation, if {@code MEMBER} follows the method invocation syntax with optional arguments.</li>
     *       <li>A "path" to the data member within the companion object. Any notation supported by {@link Objects.getProperty()} can be
     *           used here. If this name is "-", the current companion object is reused instead.</li>
     *     </ol>
     *     If omitted, the value of {@code MARKUP} is used as the name of an immediate data member.</li>
     * <li>{@code ALTERN} is an optional element, which gives the name of an alternative markup in the case the data member has no
     *     value.</li>
     * <li>{@code (ARGS)} is an optional list of positional arguments to be passed to the markup, which refers to such arguments using
     *     positional argument parameters {@code {1}}, {@code {2}}, etc.</li>
     * </ul>
     * During reference expansion, if a companion object for the expansion is not found within the current companion object, {@code MARKUP}
     * is expanded with a {@code null} companion object. If the companion object is found but has no value, {@code ALTERN} is expanded with
     * a {@code null} companion object if {@code ALTERN} is provided.
     * 
     * @param resources a collection of named text resources
     * @param name the name of the text markup to be expanded
     * @param object an object providing values to the parameters, which will be wrapped if not an open object already
     * @param args positional arguments
     * @throws IllegalArgumentException if any reference to a text markup cannot be resolved
     * @throws NullPointerException if any reference to a data member cannot be resolved and the data member is required in the
     *         subsequence markup expansion
     * @return the fully expanded text
     */
    @SuppressWarnings("fallthrough")
    public static String expand(Map<String, String> resources, final String name, final Object object, String[] args) {
        String text = resources.get(name);
        if (text == null) {
            throw new IllegalArgumentException("Unknown text resource '" + name + '\'');
        }

        // use of namespace
        int slash = name.indexOf('/');
        String namespace = slash > -1 ? name.substring(0, slash) : null;

        Object data = object == null ? null : ((object instanceof Open) ? object : new Open.Wrapper<String>(Strings.toString(object)));

        // expand all parameters first
        text = expand(text, data, args);

        while (true) {
            Matcher matcher = REFERENCE.matcher(text);
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                int top = 0;
                List<Object> items = new ArrayList<>();
                do {
                    sb.append(text.substring(top, matcher.start()));

                    String markup = null, member = null, altern = null;
                    String[] parts = Balanced.split(matcher.group(2), ':');
                    switch (parts.length) {
                    case 3:
                        altern = parts[2];
                    case 2:
                        member = parts[1];
                    case 1:
                        markup = parts[0];
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid markup specification: " + matcher.group(2));
                    }
                    if (member == null) member = markup;

                    args = parse(markup);
                    if (args != null) markup = args[0];
                    items.clear();
                    try {
                        if (data != null) {
                            if (member.equals("-")) {
                                items.add(data);
                            } else {
                                String[] params = parse(member);
                                if (params != null) {
                                    Object bean = invoke(data, params);
                                    if (bean != null) items.add(bean);
                                } else {
                                    Object bean = Objects.getProperty(data, member);
                                    if (bean != null) {
                                        Class<?> type = bean.getClass();
                                        if (type.isArray()) {
                                            for (int i = 0; i < Array.getLength(bean); ++i) items.add(Array.get(bean, i));
                                        } else if (Collection.class.isAssignableFrom(type)) {
                                            items.addAll((Collection<?>)bean);
                                        } else {
                                            items.add(bean);
                                        }
                                    }
                                }
                            }
                        } else {
                            // non-existent member: allow expansion to continue
                            items.add(null);
                        }
                    } catch (NoSuchFieldException|NoSuchMethodException x) {
                        // non-existent member: allow expansion to continue
                        items.add(null);
                    }
                    if (items.size() > 0) {
                        if (namespace != null && markup.indexOf('/') == -1) {
                            markup = namespace + '/' + markup;
                        }
                        sb.append(Strings.toString(matcher.group(1)));
                        for (Object item: items) {
                            if (markup.equals(name) && item == object) {
                                throw new IllegalStateException("Infinite recursion: " + name + ":" + object);
                            }
                            sb.append(expand(resources, markup, item, args));
                        }
                        sb.append(Strings.toString(matcher.group(3)));
                    } else if (altern != null) {
                        args = parse(altern);
                        if (args != null) altern = args[0];
                        if (namespace != null && altern.indexOf('/') == -1) {
                            altern = namespace + '/' + altern;
                        }
                        sb.append(expand(resources, altern, null, args));
                    }
                    top = matcher.end();
                } while (matcher.find());

                text = sb.append(text.substring(top)).toString();
            } else {
                break;
            }
        }

        return text;
    }

    public static String expand(Map<String, String> resources, String name, Object object) {
        return expand(resources, name, object, null);
    }

    /**
     * Expands a text markup by resolving embedded parameters, with text retrieved from an accompanying {@code Open} object.
     * <p>
     * This method is a specialization of {@link #expand(String, Pattern, Functor, String[]) Macro.expand(String, Pattern, Functor, String[])}
     * </p>
     * <p>
     * A parameter is marked up as {@code {{MEMBER:-DEFAULT}}}, where
     * <ul>
     * <li>{@code MEMBER} is a required element, which gives the name of the data member within the accompnaying object that
     *     is to be used to provide a value for this parameter</li>
     * <li>{@code :-DEFAULT} is an optional piece of text to be used if the named data member doesn't exist or is null</li>
     * </ul>
     * </p>
     * @param markup the original text containing parameters
     * @param object an object providing data members as values to the parameters
     * @param args positional arguments
     * @return the text will all parameters expanded
     * @see Macro#expand(String, Pattern, Functor, String[])
     */
    public static String expand(String markup, final Object object, String[] args) {
        return expand(markup, PARAMETER, new Functor<Object, String>() {
             public Object invoke(String name) {
                try { return Objects.getProperty(object, name); } catch (Exception x) { return null; }
            }
        }, args);
    }

    public static String expand(String markup, Object object) {
        return expand(markup, object, null);
    }

    public static String expand(String markup, Functor<Object, String> provider, String[] args) {
        return expand(markup, PARAMETER, provider, args);
    }

    public static String expand(String markup, Functor<Object, String> provider) {
        return expand(markup, PARAMETER, provider, null);
    }

    /**
     * Expands a text markup by resolving embedded parameters, recognized by a pattern, with text retrieved from a functor.
     * The pattern must include at least one capturing group, whose matched value is used as a key to retrieve
     * text from the provider.
     * <p>
     * This method repeats pattern scanning after each round of parameter expansion until no more parameters are detected.
     * </p>
     * @param markup the original text containing parameters
     * @param pattern a capturing regex pattern
     * @param provider a functor that maps parameter names to objects
     * @param args optional positional arguments, which are used when the pattern's first capturing group captures a name that
     *        can be interpreted as an integer
     * @return the text with all parameters expanded
     */
    public static String expand(String markup, Pattern pattern, Functor<Object, String> provider, String[] args) {
        while (true) {
            Matcher matcher = pattern.matcher(markup);
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                int top = 0;
                do {
                    sb.append(markup.substring(top, matcher.start()));
                    try {
                        String name = matcher.group(1);
                        if ("*".equals(name)) { // all positional arguments with comma in between
                            if (args != null) {
                                for (int i = 1; i < args.length; ++i) {
                                    if (i > 1) sb.append(',');
                                    sb.append(args[i]);
                                }
                            }
                        } else try {
                            // positional arguments take precedence
                            int pos = Integer.parseInt(name);
                            if (args != null && -1 < pos && pos < args.length) {
                                sb.append(args[pos]);
                            }
                        } catch (NumberFormatException x) {
                            // named parameter
                            Object bean = provider.invoke(name);
                            if (bean != null) {
                                if (bean.getClass().isArray()) {
                                    // note: zero-length array produces no text
                                    for (int i = 0; i < Array.getLength(bean); ++i) sb.append(Strings.toString(Array.get(bean, i)));
                                } else if (Iterable.class.isAssignableFrom(bean.getClass())) {
                                    // note: zero-length iterable produces no text
                                    for (Object item: (Iterable<?>)bean) sb.append(Strings.toString(item));
                                } else {
                                    // non-container object
                                    sb.append(Strings.toString(bean));
                                }
                            } else if (matcher.groupCount() > 1) {
                                // second capturing group provides an alternative value
                                sb.append(Strings.toString(matcher.group(2)));
                            }
                        }
                    } catch (Exception x) {
                        // name look up failure
                        if (matcher.groupCount() > 1) {
                            // second capturing group provides an alternative value
                            sb.append(Strings.toString(matcher.group(2)));
                        }
                    }
                    top = matcher.end();
                } while (matcher.find());
                markup = sb.append(markup.substring(top)).toString();
            } else {
                break;
            }
        }

        return markup;
    }

    public static String expand(String markup, Pattern pattern, Functor<Object, String> provider) {
        return expand(markup, pattern, provider, null);
    }

    // parses a markup spec for arguments between paratheses. returning an array with the markup name as the first element
    private static String[] parse(String text) {
        int lpara = text.indexOf('(');
        switch (lpara) {
        case -1:
            return null;
        case 0:
            throw new IllegalArgumentException("Invalid markup with arguments '" + text + '\'');
        default:
            int rpara = text.lastIndexOf(')');
            if (rpara != text.length() - 1) {
                throw new IllegalArgumentException("Invalid markup with arguments '" + text + '\'');
            } else {
                List<String> args = new ArrayList<>();
                args.add(text.substring(0, lpara));
                return Balanced.split(args, null, text, lpara + 1, rpara, ',', null).toArray(new String[args.size()]);
            }
        }
    }

    private static Object invoke(Object bean, String[] args) throws NoSuchMethodException {
        try {
            for (Method candidate: bean.getClass().getMethods()) {
                if (!candidate.getName().equals(args[0]) || candidate.getParameterCount() != args.length - 1) continue;
                Class<?>[] ptypes = candidate.getParameterTypes();
                Object[] params = new Object[ptypes.length];
                for (int i = 0; i < ptypes.length; ++i) {
                    params[i] = new ValueOf(ptypes[i]).invoke(args[i + 1].trim());
                }
                return Beans.accessible(candidate).invoke(bean, params);
            }
            throw new NoSuchMethodException(args[0]);
        } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException x) {
            throw new NoSuchMethodException(x.getMessage());
        }
    }
}
