package org.xillium.base.beans;

import java.util.*;
import java.util.regex.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
* A collection of commonly used String related utilities.
*/
public class Strings {
    static final byte[] HEX_CHARS = {
        (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
        (byte)'8', (byte)'9', (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f'
    };

    public static String toString(Object object) {
        return object != null ? object.toString() : "";
    }

    public static String toHexString(byte[] raw) throws java.io.UnsupportedEncodingException {
        byte[] hex = new byte[2 * raw.length];
        int index = 0;
        for (byte b : raw) {
            int v = b & 0x00ff;
            hex[index++] = HEX_CHARS[v >>> 4];
            hex[index++] = HEX_CHARS[v & 0xf];
        }
        return new String(hex, "ASCII");
    }

    /**
     * Converts a hyphen-separated word sequence into a single camel-case word.
     */
    public static String toCamelCase(String text) {
        return toCamelCase(text, '-');
    }

    /**
     * Converts a word sequence into a single camel-case word.
     */
    public static String toCamelCase(String text, char separator) {
        return toCamelCase(text, separator, true);
    }

    /**
     * Converts a word sequence into a single camel-case sequence.
     *
     * @param strict - if true, all letters following the first are forced into lower case in each word
     */
    public static String toCamelCase(String text, char separator, boolean strict) {
        char[] chars = text.toCharArray();
        int base = 0, top = 0;

        while (top < chars.length) {
            while (top < chars.length && chars[top] == separator) {
                ++top;
            }
            if (top < chars.length) {
                chars[base++] = Character.toUpperCase(chars[top++]);
            }
            if (strict) {
                while (top < chars.length && chars[top] != separator) {
                    chars[base++] = Character.toLowerCase(chars[top++]);
                }
            } else {
                while (top < chars.length && chars[top] != separator) {
                    chars[base++] = chars[top++];
                }
            }
        }

        return new String(chars, 0, base);
    }

    /**
     * Converts a word sequence into a single camel-case word that starts with a lowercase letter.
     */
    public static String toLowerCamelCase(String text, char separator) {
        char[] chars = text.toCharArray();
        int base = 0, top = 0;

        do {
            while (top < chars.length && chars[top] != separator) {
                chars[base++] = Character.toLowerCase(chars[top++]);
            }
            while (top < chars.length && chars[top] == separator) {
                ++top;
            }
            if (top < chars.length) {
                chars[base++] = Character.toUpperCase(chars[top++]);
            }
        } while (top < chars.length);

        return new String(chars, 0, base);
    }

    static final Pattern CAMEL_REGEX = Pattern.compile(
        String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])")
    );

    public static String splitCamelCase(String text, String separator) {
        return CAMEL_REGEX.matcher(text).replaceAll(separator);
    }

    /**
     * Capitalizes a word
     */
    public static String capitalize(String text) {
        char[] chars = text.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    /**
     * Generates a secure hash from a given text.
     */
    public static String hash(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return Strings.toHexString(MessageDigest.getInstance("SHA").digest(text.getBytes("UTF-8")));
    }

    /**
     * Extends a string to have at least the given length.
     */
    public static String extend(String text, char fill, int length) {
        if (text.length() < length) {
            char[] buffer = new char[length];
            java.util.Arrays.fill(buffer, 0, length, fill);
            System.arraycopy(text.toCharArray(), 0, buffer, 0, text.length());
            return new String(buffer);
        } else {
            return text;
        }
    }

    /**
     * Returns a substring from position 0 up to just before the stopper character.
     */
    public static String substringBefore(String text, char stopper) {
        int p = text.indexOf(stopper);
        return p < 0 ? text : text.substring(0, p);
    }

    /**
     * Returns the substring following a given character, or the original if the character is not found.
     */
    public static String substringAfter(String text, char match) {
        return text.substring(text.indexOf(match)+1);
    }


    private static final Pattern PARAM_SYNTAX = Pattern.compile("\\{([^{}]+)\\}");

    /**
     * Extracts specially marked arguments from a string, and returns the string with such arguments replaced with "{}".
     * <ol>
     * <li> Arguments are enclosed between '{' and '}'</li>
     * <li> Optional extra text is allowed at the end of line starting with '#', which is removed before returning</li>
     * </ol>
     */
    public static String extractArguments(List<String> params, String original) {
        int eol = original.indexOf('#');
        if (eol > -1) {
            original = original.substring(0, eol);
        }
        Matcher matcher = PARAM_SYNTAX.matcher(original);
        while (matcher.find()) {
            try {
                params.add(matcher.group(1));
            } catch (Exception x) {
                throw new IllegalArgumentException("Translation format specification", x);
            }
        }
        return matcher.replaceAll("{}");
    }

    /**
     * Collects object member values into an array of Strings, each prefixed by the field name.
     */
    public static String[] collect(Object object, String... names) {
        return collect(null, 0, "", object, ':', names);
    }

    /**
     * Collects object member values into an array of Strings, each prefixed by the field name.
     */
    public static String[] collect(Object object, char separator, String... names) {
        return collect(null, 0, "", object, separator, names);
    }

    /**
     * Collects object member values into an array of Strings, each prefixed by the field name.
     */
    public static String[] collect(String[] storage, int offset, String prefix, Object object, char separator, String... names) {
        if (storage == null) {
            storage = new String[names.length];
            offset = 0;
        }
        Class<?> type = object.getClass();
        for (int i = 0; i < names.length; ++i) {
            try {
                storage[offset + i] = prefix + names[i] + separator + Beans.getKnownField(type, names[i]).get(object);
            } catch (Exception x) {
                storage[offset + i] = prefix + names[i] + separator;
            }
        }
        return storage;
    }

    /**
     * Returns a formatted string using the specified format string and object fields.
     *
     * <ol>
     * <li> Parameter names in the format string are enclosed between '{' and '}'</li>
     * <li> After formatting, each value is added between a parameter name and the closing brace
     * </ol>
     */
    public static String format(String pattern, Object args) {
        return format(pattern, args, false);
    }

    /**
     * @param preserve - preserves original parameter marks
     */
    public static String format(String pattern, Object args, boolean preserve) {
        StringBuilder sb = null;
        int top = 0;
        Matcher matcher = PARAM_SYNTAX.matcher(pattern);

        if (matcher.find()) {
            sb = new StringBuilder();
            Class<?> type = args.getClass();
            do {
                try {
                    Object value = Beans.getKnownField(type, substringAfter(matcher.group(1), '.')).get(args);
                    if (preserve) {
                        sb.append(pattern.substring(top, matcher.end() - 1)).append(':').append(value);
                        top = matcher.end() - 1;
                    } else {
                        sb.append(pattern.substring(top, matcher.start())).append(value);
                        top = matcher.end();
                    }
                } catch (Exception x) {
                    //throw new IllegalArgumentException("Translation format specification", x);
                    // no value in args that matches the parameter
                }
            } while (matcher.find());
        }

        return (sb == null) ? pattern : sb.append(pattern.substring(top)).toString();
    }
}
