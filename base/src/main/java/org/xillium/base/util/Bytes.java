package org.xillium.base.util;

import java.io.*;


/**
 * A collection of commonly used byte array related utilities.
 */
public class Bytes {
    /**
     * Reads the whole contents of an input stream into a byte array, leaving the stream open afterwards.
     */
    public static byte[] read(InputStream in) throws IOException {
        return read(in, false);
    }

    /**
     * Reads the whole contents of an input stream into a byte array, closing the stream if so requested.
     */
    public static byte[] read(InputStream in, boolean closeAfterwards) throws IOException {
        byte[] buffer = new byte[32*1024];
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        for (int length; (length = in.read(buffer, 0, buffer.length)) > -1; bas.write(buffer, 0, length));
        if (closeAfterwards) in.close();
        return bas.toByteArray();
    }
}
