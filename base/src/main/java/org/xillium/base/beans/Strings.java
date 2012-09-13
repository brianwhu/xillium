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
        return  matcher.replaceAll("{}");
    }
}
