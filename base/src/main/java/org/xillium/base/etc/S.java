package org.xillium.base.etc;

import java.util.logging.*;


/**
 * A collection of API shortcuts
 */
public class S {
    public static boolean info(Logger logger) {
        return logger.isLoggable(Level.INFO);
    }
    public static boolean fine(Logger logger) {
        return logger.isLoggable(Level.FINE);
    }
}
