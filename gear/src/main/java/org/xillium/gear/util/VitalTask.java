package org.xillium.gear.util;

import java.util.Random;
import java.util.logging.*;
import org.xillium.base.beans.Throwables;
import org.xillium.core.management.Manageable;


/**
 * A VitalTask is a Runnable that detects task failure and undertaks retries automatically.
 * Failure and recovery reports are send to an associated Manageable.
 *
 * It uses randomized exponetial backoff to avoid retrying the task too eagerly.
 */
public abstract class VitalTask<T extends Manageable> implements Runnable {
    protected static final Logger _logger = Logger.getLogger(VitalTask.class.getName());

    private static final long INI_BACKOFF =  1000;
    private static final long MAX_BACKOFF = 32000;
    private static final Random _random = new Random();
    private final T _manageable;
    private int _times;

    protected VitalTask(T manageable) {
        _manageable = manageable;
    }

    protected T getManageable() {
        return _manageable;
    }

    /**
     * Performs the task. Subclass overrides this method to implement the task.
     */
    protected abstract void execute() throws Exception;

    /**
     * Returns the number of retries this VitalTask has undertaken so far.
     */
    public final int getAge() {
        return _times;
    }

    public final void run() {
        while (true) {
            try {
                execute();
                if (_times > 0) {
                    String t = "Failure recovered: " + toString();
                    _logger.info(t);
                    _manageable.emit(Manageable.Severity.NOTICE, t, _times);
                }
                return;
            } catch (Exception x) {
                _logger.log(Level.WARNING, "Failure detected, age = " + _times, x);
                _manageable.emit(Manageable.Severity.ALERT, Throwables.getFullMessage(x), _times);

                // exponential back off
                try {
                    Thread.sleep(Math.min(MAX_BACKOFF, INI_BACKOFF + (int)Math.round(_random.nextDouble() * INI_BACKOFF * (1L << _times))));
                } catch (Throwable t) {}
                ++_times;
            }
        }
    }
}
