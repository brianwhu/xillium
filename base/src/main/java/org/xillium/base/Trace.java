package org.xillium.base;

import java.util.logging.Level;


/**
 * A basic trace facility.
 */
public interface Trace {
    static class Global {
        public final Trace std = new NullTrace();

        Global() {
            try {
                _std = Global.class.getField("std");
                _std.setAccessible(true);
            } catch (NoSuchFieldException x) {
                // won't happen
            }
        }

        public Global configure(Trace trace) {
            try {
                _std.set(this, trace);
            } catch (IllegalAccessException x) {
                // won't happen
                throw new RuntimeException("Failed to set value to 'std'", x);
            }
            return this;
        }

        private java.lang.reflect.Field _std;
    }

    /**
     * Sets the trace level.
     *
     * A level = OFF or above shows warn() and dump();
     * A level = SEVERE or above shows warn() and dump();
     * A level = WARNING or above shows warn() and dump();
     * A level = INFO or above shows note() and dump();
     */
    public Trace setLevel(Level level);

    /**
     * Sets the trace filter.
     *
     * A filter removes traces from objects that are not instances of this class or its subclasses.
     */
    public Trace setFilter(Class<?> filter);

    /**
     * Reports an informational event, with level = INFO
     */
    public Trace note(Class<?> c, String message);

    /**
     * Reports a warning condition, with level = WARNING
     */
    public Trace warn(Class<?> c, String message);

    /**
     * Reports a warning condition with a throwable, with level = WARNING
     */
    public Trace warn(Class<?> c, String message, Throwable t);

    /**
     * Reports an alerting condition, with level = SEVERE
     */
    public Trace alert(Class<?> c, String message);

    /**
     * Reports an alerting condition with a throwable, with level = SEVERE
     */
    public Trace alert(Class<?> c, String message, Throwable t);

    public static final Global g = new Global();
}
