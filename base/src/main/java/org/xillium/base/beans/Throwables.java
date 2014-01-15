package org.xillium.base.beans;

import java.nio.ByteBuffer;
import java.util.*;


/**
* A collection of commonly used Throwable related utilities.
*/
public class Throwables {
	public static StringBuilder appendStackTrace(StringBuilder sb, Throwable t) {
		for (StackTraceElement e: t.getStackTrace()) {
			sb.append("\tat ").append(e.toString()).append('\n');
		}
		return sb;
	}

    public static Throwable getRootCause(Throwable x) {
        for (Throwable t; (t = x.getCause()) != null;  x = t);
        return x;
    }

    public static String getFirstMessage(Throwable x) {
        Throwable cause = x;
        String message;
        while ((message = x.getMessage()) == null && (x = x.getCause()) != null) cause = x;
        if (message == null || message.length() == 0) {
            message = "***" + cause.getClass().getSimpleName();
        }
        return message;
    }

    public static String getExplanation(Throwable x) {
        StringBuilder sb = new StringBuilder(x.getClass().getName()).append(": ").append(x.getMessage());
        Throwable root = getRootCause(x);
        if (x != root) {
            sb.append(" caused by ").append(root.getClass().getName()).append(": ").append(root.getMessage());
        }
        return sb.toString();
    }

    public static String getFullMessage(Throwable x) {
        StringBuilder sb = new StringBuilder(x.getClass().getName()).append(": ").append(x.getMessage());
        Throwable root = getRootCause(x);
        if (x != root) {
            sb.append(" caused by ").append(root.getClass().getName()).append(": ").append(root.getMessage());
            x = root;
        }
        return appendStackTrace(sb.append("\n"), x).toString();
    }

    public static byte[] hash(Throwable x) throws Exception {
        List<byte[]> list = new ArrayList<byte[]>();
        for (Class<?> type = x.getClass(); type != RuntimeException.class; type = type.getSuperclass()) {
            java.lang.reflect.Field f = type.getDeclaredField("serialVersionUID");
            f.setAccessible(true);
            list.add(ByteBuffer.allocate(8).putLong(f.getLong(null)).array());
        }
        byte[] bytes = new byte[list.size()*8+1];
        for (int i = 0; i < list.size(); ++i) {
            System.arraycopy(list.get(i), 0, bytes, i*8+1, 8);
        }
        return bytes;
    }
}
