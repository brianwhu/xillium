package org.xillium.base.beans;

//import java.beans.*;
//import java.lang.reflect.*;
//import java.util.*;


/**
* A collection of commonly used Throwable related utilities.
*/
public class Throwables {
	public static StringBuilder appendStackTrace(StringBuilder sb, Throwable t) {
		for (StackTraceElement e: t.getStackTrace()) {
			sb.append(e.toString()).append('\n');
		}
		return sb;
	}

    public static Throwable getRootCause(Throwable x) {
        Throwable t;
        while ((t = x.getCause()) != null) x = t;
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

    public static String getFullMessage(Throwable x) {
        StringBuilder sb = new StringBuilder(x.getClass().getName()).append(": ").append(x.getMessage());
        Throwable root = getRootCause(x);
        if (x != root) {
            sb.append(" caused by ").append(root.getClass().getName()).append(": ").append(root.getMessage());
            x = root;
        }
        return appendStackTrace(sb, x).toString();
    }
}
