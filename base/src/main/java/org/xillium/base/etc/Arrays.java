package org.xillium.base.etc;

import java.io.*;


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
}
