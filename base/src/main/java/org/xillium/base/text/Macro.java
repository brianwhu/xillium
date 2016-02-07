package org.xillium.base.text;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;

import org.xillium.base.*;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
import org.xillium.base.text.GuidedTransformer;


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
    private static final Pattern PARAMETER = Pattern.compile("\\{([^{}@:-]+)(?::-([^{}@]+))?\\}");
    private static final Pattern REFERENCE = Pattern.compile("\\{([^{}@]+)?@([^{}@]+)@([^{}@]+)?\\}");
    private static final GuidedTransformer<List<String>> MarkUpParser = new GuidedTransformer<>(Pattern.compile("[^:()]+(?:\\([^()]*\\))?"),
        new Trifunctor<StringBuilder, StringBuilder, List<String>, Matcher>() {
            public StringBuilder invoke(StringBuilder sb, List<String> names, Matcher matcher) {
                names.add(matcher.group(0));
                return sb;
            }
        },
        GuidedTransformer.Action.SKIP
    );


    /**
     * Expands a text markup by resolving embedded parameters and references to other text markups, with the help of a companion
     * open object.
     * <p>
     * A reference pointing to another markup in the resources is marked up as {@code {PREFIX@MARKUP(ARGS):MEMBER:ALTERN(ARGS)@SUFFIX}},
     * where</p>
     * <ul>
     * <li>{@code PREFIX} and {@code SUFFIX} are optional pieces of text to be placed before and after the markup insertion.</li>
     * <li>{@code MARKUP} is a required element, which gives the name of the markup to be expanded recursively.</li>
     * <li>{@code MEMBER} gives the name of the data member within the companion object to be used as the companion object for the
     *     recursive expansion of the markup. If this name is "-", the current companion object is reused instead. If omitted, the
     *     value of {@code MARKUP} is used as this name.</li>
     * <li>{@code ALTERN} is an optional element, which gives the name of an alternative markup in the case the data member has no
     *     value.</li>
     * <li>{@code (ARGS)} is an optional list of positional arguments to be passed to the markup, which refers to such arguments using
     *     positional argument parameters {@code {1}}, {@code {2}}, etc.</li>
     * </ul>
     * 
     * @param resources a collection of named text resources
     * @param name the name of the text markup to be expanded
     * @param object an object providing values to the parameters, which will be wrapped if not an open object already
     * @throws IllegalArgumentException if any reference to a text markup cannot be resolved
     * @throws NullPointerException if any reference to a data member cannot be resolved and the data member is required in the
     *         subsequence markup expansion
     * @return the fully expanded text
     */
    public static String expand(Map<String, String> resources, String name, final Object object, String[] args) {
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
                    List<String> names = new ArrayList<>();
                    MarkUpParser.invoke(null, names, matcher.group(2));
                    switch (names.size()) {
                    case 3:
                        altern = names.get(2);
                    case 2:
                        member = names.get(1);
                    case 1:
                        markup = names.get(0);
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
                                Field field = Beans.getKnownField(data.getClass(), member);
                                Object bean = field.get(data);
                                if (field.getType().isArray()) {
                                    if (bean != null) for (int i = 0; i < Array.getLength(bean); ++i) items.add(Array.get(bean, i));
                                } else if (Collection.class.isAssignableFrom(field.getType())) {
                                    if (bean != null) items.addAll((Collection<?>)bean);
                                } else {
                                    if (bean != null) items.add(bean);
                                }
                            }
                        } else {
                            // non-existent member: allow expansion to continue
                            items.add(null);
                        }
                    } catch (NoSuchFieldException|IllegalAccessException x) {
                        // non-existent member: allow expansion to continue
                        items.add(null);
                    }
                    if (items.size() > 0) {
                        if (namespace != null && markup.indexOf('/') == -1) {
                            markup = namespace + '/' + markup;
                        }
                        sb.append(Strings.toString(matcher.group(1)));
                        for (Object item: items) {
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
     * A parameter is marked up as {@code {MEMBER:-DEFAULT}}, where
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
                try { return Beans.getKnownField(object.getClass(), name).get(object); } catch (Exception x) { return null; }
            }
        }, args);
    }

    public static String expand(String markup, final Object object) {
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
                        try {
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
        if (lpara > -1) {
            int rpara = text.indexOf(')', lpara + 1);
            if (rpara != text.length() - 1) {
                throw new IllegalArgumentException("Invalid markup with arguments '" + text + '\'');
            } else {
                return text.split("[,()]"); // spaces are to be preserved
            }
        } else {
            return null;
        }
    }
}
