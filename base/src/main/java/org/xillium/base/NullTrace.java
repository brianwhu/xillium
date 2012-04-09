package org.xillium.base;

import java.util.logging.Level;


/**
 * A basic trace facility.
 */
public class NullTrace implements Trace {
    /**
     * Sets the trace level.
     *
     * A level = OFF or above shows warn() and alert();
     * A level = SEVERE shows alert();
     * A level = WARNING shows warn() and alert();
     * A level = INFO shows note(), warn(), and alert();
     */
    public Trace setLevel(Level level) { return this; }

    /**
     * Sets the trace filter.
     *
     * A filter removes traces from objects that are not instances of this class or its subclasses.
     */
    public Trace setFilter(Class<?> filter) { return this; }

    /**
     * Reports an informational event.
     */
    public Trace note(Class<?> c, String message) { return this; }

    /**
     * Reports an alarming condition.
     */
    public Trace warn(Class<?> c, String message) { return this; }

    /**
     * Reports an alarming condition.
     */
    public Trace warn(Class<?> c, String message, Throwable t) { return this; }

    /**
     * Reports an error condition.
     */
    public Trace alert(Class<?> c, String message) { return this; }

    /**
     * Reports an error condition.
     */
    public Trace alert(Class<?> c, String message, Throwable t) { return this; }
}
