package org.xillium.base.beans;

import java.util.*;
import java.util.regex.*;


/**
* A collection of commonly used String related utilities.
*/
public class Strings {
    static final byte[] HEX_CHARS = {
        (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
        (byte)'8', (byte)'9', (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f'
    };

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
     * @strict - if true, all letters following the first are forced into lower case in each word
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

    /**
     * Capitalizes a word
     */
    public static String capitalize(String text) {
        char[] chars = text.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
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
     * Formats a string that contains named parameters.
     * <ol>
     * <li> Parameters are enclosed between '{' and '}'</li>
     * <li> Optional extra text is allowed at the end of line starting with '#', which is removed before returning</li>
     * </ol>
     */
    public static String format(String pattern, Object args) {
        int eol = pattern.indexOf('#');
        if (eol > -1) {
            pattern = pattern.substring(0, eol);
        }
        Class<?> type = args.getClass();
        StringBuilder sb = new StringBuilder();
        int top = 0;

        Matcher matcher = PARAM_SYNTAX.matcher(pattern);
        while (matcher.find()) {
            try {
                sb.append(pattern.substring(top, matcher.start()));
                java.lang.reflect.Field field = Beans.getKnownField(type, matcher.group(1));
                sb.append(field.get(args));
                top = matcher.end();
            } catch (Exception x) {
                throw new IllegalArgumentException("Translation format specification", x);
            }
        }
        sb.append(pattern.substring(top));
        return sb.toString();
    }
}
