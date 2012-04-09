package org.xillium.base;

import java.util.logging.Level;


/**
 * A basic trace facility using standard error output.
 */
public class StandardTrace implements Trace {
    /**
     * Sets the trace level.
     *
     * A level = OFF or below shows warn() and dump();
     * A level = SEVERE or below shows warn() and dump();
     * A level = WARNING or below shows warn() and dump();
     * A level = INFO or below shows note() and dump();
     */
    public Trace setLevel(Level level) {
        this.level = level;
        return this;
    }

    /**
     * Sets the trace filter.
     *
     * A filter removes traces from objects that are not instances of this class or its subclasses.
     */
    public Trace setFilter(Class<?> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Reports an informational event.
     */
    public Trace note(Class<?> c, String message) {
        if ((filter == null || filter.isAssignableFrom(c)) && level.intValue() <= Level.INFO.intValue()) {
            System.err.println(String.format("%s INFO %s", c.getName(), message));
        }
        return this;
    }

    /**
     * Reports an alarming condition.
     */
    public Trace warn(Class<?> c, String message) {
        if ((filter == null || filter.isAssignableFrom(c)) && level.intValue() <= Level.WARNING.intValue()) {
            System.err.println(String.format("%s WARN %s", c.getName(), message));
        }
        return this;
    }

    /**
     * Reports an alarming condition.
     */
    public Trace warn(Class<?> c, String message, Throwable t) {
        if ((filter == null || filter.isAssignableFrom(c)) && level.intValue() <= Level.WARNING.intValue()) {
            System.err.println(String.format("%s WARN %s", c.getName(), message));
            t.printStackTrace(System.err);
        }
        return this;
    }

    /**
     * Reports an error condition.
     */
    public Trace alert(Class<?> c, String message) {
        if ((filter == null || filter.isAssignableFrom(c)) && level.intValue() <= Level.SEVERE.intValue()) {
            System.err.println(String.format("%s SEVERE %s", c.getName(), message));
        }
        return this;
    }

    /**
     * Reports an error condition.
     */
    public Trace alert(Class<?> c, String message, Throwable t) {
        if ((filter == null || filter.isAssignableFrom(c)) && level.intValue() <= Level.SEVERE.intValue()) {
            System.err.println(String.format("%s SEVERE %s", c.getName(), message));
            t.printStackTrace(System.err);
        }
        return this;
    }

    private Level level = Level.ALL;
    private Class<?> filter;
}
