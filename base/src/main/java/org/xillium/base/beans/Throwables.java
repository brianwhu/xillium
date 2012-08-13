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
        for (Throwable cause = x.getCause(); cause != null; cause = x.getCause()) {
            x = cause;
        }
        return x;
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
