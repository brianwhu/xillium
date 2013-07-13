package org.xillium.base.etc;

import java.io.*;
import java.lang.reflect.*;


/**
 * A collection of commonly used array related utilities.
 */
public class Arrays {
    public static byte[] read(InputStream in) throws IOException {
        byte[] buffer = new byte[32*1024];
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        for (int length; (length = in.read(buffer, 0, buffer.length)) > -1; bas.write(buffer, 0, length));
        return bas.toByteArray();
    }

    public static String join(Object array, char separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, ii = Array.getLength(array); i < ii; ++i) {
            Object element = Array.get(array, i);
            sb.append(element != null ? element.toString() : "null").append(separator);
        }
        if (sb.length() > 0) sb.setLength(sb.length()-1);
        return sb.toString();
    }
}
