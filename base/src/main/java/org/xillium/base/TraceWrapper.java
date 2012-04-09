package org.xillium.base;

import java.util.logging.Level;


/**
 * A basic trace facility using standard error output.
 */
public class TraceWrapper implements Trace {
    private Trace _trace;

    public TraceWrapper(Trace trace) {
        if (trace != null) {
            _trace = trace;
        } else {
            throw new NullPointerException();
        }
    }

    public Trace configure(Trace trace) {
        if (trace != null) {
            _trace = trace;
        } else {
            throw new NullPointerException();
        }
        return this;
    }

    /**
     * Sets the trace level.
     *
     * A level = OFF or below shows warn() and dump();
     * A level = SEVERE or below shows warn() and dump();
     * A level = WARNING or below shows warn() and dump();
     * A level = INFO or below shows note() and dump();
     */
    public final Trace setLevel(Level level) {
        return _trace.setLevel(level);
    }

    /**
     * Sets the trace filter.
     *
     * A filter removes traces from objects that are not instances of this class or its subclasses.
     */
    public Trace setFilter(Class<?> filter) {
        return _trace.setFilter(filter);
    }

    /**
     * Reports an informational event.
     */
    public final Trace note(Class<?> c, String message) {
        return _trace.note(c, message);
    }

    /**
     * Reports an alarming condition.
     */
    public final Trace warn(Class<?> c, String message) {
        return _trace.warn(c, message);
    }

    /**
     * Reports an alarming condition.
     */
    public final Trace warn(Class<?> c, String message, Throwable t) {
        return _trace.warn(c, message, t);
    }

    /**
     * Reports an error condition.
     */
    public final Trace alert(Class<?> c, String message) {
        return _trace.alert(c, message);
    }

    /**
     * Reports an error condition.
     */
    public final Trace alert(Class<?> c, String message, Throwable t) {
        return _trace.alert(c, message, t);
    }
}
