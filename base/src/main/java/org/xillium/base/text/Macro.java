package org.xillium.base.text;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;

import org.xillium.base.Open;
import org.xillium.base.Functor;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;


/**
 * This is a macro processor that can handle 2 types of macro expansions.
 * <ol>
 * <li>Placeholder expansion - see {@link Macro#expand(String, Object) Macro.expand} method and
 *     {@link Macro#expand(String, Pattern, Functor<Object, String>)} method</li>
 * <li>Reference expansion - insertion of another markup snippet with its own embedded placeholders and references</li>
 * </ol>
 * Dynamic data are provided by an open object.
 */
public class Macro {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_]\\w*)(?::-([^{}]+))?\\}");
    private static final Pattern REFERENCE = Pattern.compile("\\{([^{}@]+)?@([^{}@]+)@([^{}@]+)?\\}");

    /**
     * Expands a text markup by resolving embedded placeholders and references to other text markups, with the help of an accompanying
     * open object.
     * <p>
     * A reference pointing to another markup in the resources is marked up as {@code {PREFIX@MARKUP:MEMBER@SUFFIX}}, where</p>
     * <ul>
     * <li>{@code MARKUP} is a required element, which gives the name of the markup as well as the data member within the accompnaying
     *     open object that is to be used as the accompanying open object for the recursive expansion of the markup</li>
     * <li>{@code PREFIX} and {@code SUFFIX} are optional pieces of text to be placed before and after the markup insertion.</li>
     * <li>{@code :MEMBER} gives the name of the data member to accompany the markup. If omitted, the value of {@code MARKUP} is used as
     *     this name.</li>
     * </ul>
     * 
     * @param resources a collection of named text resources
     * @param name the name of the text markup to be expanded
     * @param object an object providing values to the placeholders, which will be wrapped if not an open object already
     * @throws IllegalArgumentException if any reference to a text markup cannot be resolved
     * @throws NullPointerException if any reference to a data member cannot be resolved and the data member is required in the
     *         subsequence markup expansion
     * @return the fully expanded text
     */
    public static String expand(Map<String, String> resources, String name, final Object object) {
        String text = resources.get(name);
        if (text == null) {
            throw new IllegalArgumentException("Unknown text resource '" + name + '\'');
        }

        // use of namespace
        int slash = name.indexOf('/');
        String namespace = slash > -1 ? name.substring(0, slash) : null;

        Object data = object == null ? null : ((object instanceof Open) ? object : new Open.Wrapper<String>(Strings.toString(object)));

        // expand all placeholders first
        text = expand(text, data);

        while (true) {
            StringBuilder sb = null;
            int top = 0;

            Matcher matcher = REFERENCE.matcher(text);
            if (matcher.find()) {
                sb = new StringBuilder();
                List<Object> items = new ArrayList<>();
                do {
                    sb.append(text.substring(top, matcher.start()));
                    if (data != null) {
                        String reference = matcher.group(2);
                        String markup = Strings.substringBefore(reference, ':');
                        String member = Strings.substringAfter(reference, ':');
                        items.clear();
                        try {
                            Field field = Beans.getKnownField(data.getClass(), member);
                            Object bean = field.get(data);
                            if (field.getType().isArray()) {
                                if (bean != null) for (int i = 0; i < Array.getLength(bean); ++i) items.add(Array.get(bean, i));
                            } else if (Collection.class.isAssignableFrom(field.getType())) {
                                if (bean != null) items.addAll((Collection<?>)bean);
                            } else {
                                if (bean != null) items.add(bean);
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
                                sb.append(expand(resources, markup, item));
                            }
                            sb.append(Strings.toString(matcher.group(3)));
                        }
                    }
                    top = matcher.end();
                } while (matcher.find());

                text = sb == null ? text : sb.append(text.substring(top)).toString();
            } else {
                break;
            }
        }

        return text;
    }

    /**
     * Expands a text markup by resolving embedded placeholders, with the help of an accompanying open object.
     * <p>A placeholder is marked up as {@code {MEMBER:-DEFAULT}}, where</p>
     * <ul>
     * <li>{@code MEMBER} is a required element, which gives the name of the data member within the accompnaying open object that
     *     is to be used to provide a replacement value for this placeholder</li>
     * <li>{@code :-DEFAULT} is an optional piece of text to be used if the named data member doesn't exist or is null</li>
     * </ul>
     * <p>
     * During expansion, placeholders are simply replaced by values of the named data members in the accompanying open object.</p>
     *
     * @param markup the original text containing placeholders
     * @param object an object providing data members as values to the placeholders
     * @return the text will all placeholders expanded
     * @see Macro#expand(String, Pattern, Functor<Object, String>) expand
     */
    public static String expand(String markup, final Object object) {
        return expand(markup, PLACEHOLDER, new Functor<Object, String>() {
             public Object invoke(String name) {
                try { return Beans.getKnownField(object.getClass(), name).get(object); } catch (Exception x) { return null; }
            }
        });
    }

    /**
     * Expands a text markup by resolving embedded placeholders, recognized by a pattern, with text retrieved through a functor.
     * The pattern must include at least one capturing group, whose matched value is used as a key to retrieve corresponding
     * text from the provider.
     *
     * @param markup the original text containing placeholders
     * @param placeholder a capturing regex pattern
     * @param provider a functor that maps placeholder keys to corresponding text strings
     * @return the text will all placeholders expanded
     */
    public static String expand(String markup, Pattern placeholder, Functor<Object, String> provider) {
        StringBuilder sb = null;
        int top = 0;
        Matcher matcher = placeholder.matcher(markup);
        if (matcher.find()) {
            sb = new StringBuilder();
            do {
                sb.append(markup.substring(top, matcher.start()));
                try {
                    Object bean = provider.invoke(matcher.group(1));
                    if (bean != null) {
                        if (bean.getClass().isArray()) {
                            for (int i = 0; i < Array.getLength(bean); ++i) sb.append(Strings.toString(Array.get(bean, i)));
                        } else if (Iterable.class.isAssignableFrom(bean.getClass())) {
                            for (Object item: (Iterable<?>)bean) sb.append(Strings.toString(item));
                        } else {
                            sb.append(Strings.toString(bean));
                        }
                    } else if (matcher.groupCount() > 1) {
                        sb.append(Strings.toString(matcher.group(2)));
                    }
                } catch (Exception x) {
                    if (matcher.groupCount() > 1) {
                        sb.append(Strings.toString(matcher.group(2)));
                    }
                }
                top = matcher.end();
            } while (matcher.find());
        }

        return (sb == null) ? markup : sb.append(markup.substring(top)).toString();
    }
}
