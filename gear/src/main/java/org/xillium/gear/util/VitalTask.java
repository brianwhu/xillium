package org.xillium.gear.util;

import java.util.Random;
import java.util.logging.*;
import org.xillium.base.beans.Throwables;
import org.xillium.core.management.Manageable;


/**
 * A VitalTask is a Runnable that detects execution failure and undertakes retries automatically.
 * Failure and recovery notifications are reported via an associated Manageable.
 *
 * By default, a VitalTask uses randomized exponetial backoff to avoid retrying the task too eagerly. Other TrialStrategy can be used as well.
 *
 * A VitalTask is interruptible. If a VitalTask is submitted to a separate thread, a negative age reveals execution interruption. When running a
 * VitalTask on the local thread, call runAsInterruptible() instead of run() to get execution interruption reported as an InterruptedException.
 */
public abstract class VitalTask<T extends Manageable> implements Runnable {
    protected static final Logger _logger = Logger.getLogger(VitalTask.class.getName());

    private final T _manageable;
    private final TrialStrategy _strategy;
    private InterruptedException _interrupted;
    private int _age;

    protected VitalTask(T manageable) {
        _manageable = manageable;
        _strategy = ExponentialBackoff.instance;
    }

    protected VitalTask(T manageable, TrialStrategy strategy) {
        _manageable = manageable;
        _strategy = strategy;
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
        return _age;
    }

    /**
     * Returns the InterruptedException if an interruption occured, null otherwise;
     */
    public final InterruptedException getInterruptedException() {
        return _interrupted;
    }

    /**
     * Task entry point.
     */
    public final void run() {
        _interrupted = null;
        _age = 0;
        while (true) {
            try {
                _strategy.observe(_age);
                execute();
                if (_age > 0) {
                    _manageable.emit(Manageable.Severity.NOTICE, "Failure recovered: " + toString(), _age);
                }
                break;
            } catch (InterruptedException x) {
                _logger.log(Level.WARNING, "Interrupted, age = " + _age, x);
                _interrupted = x;
                break;
            } catch (Exception x) {
                _manageable.emit(Manageable.Severity.ALERT, "Failure detected, age = " + _age + ": " + Throwables.getFullMessage(x), _age);
                try {
                    _strategy.backoff(_age);
                } catch (InterruptedException i) {
                    _logger.log(Level.WARNING, "Interrupted, age = " + _age, x);
                    _interrupted = i;
                    break;
                }
                ++_age;
            }
        }
    }

    /**
     * Calls run() and returns "this". Throws an InterruptedException if thread interruption is detected during run().
     */
    public final VitalTask<T> runAsInterruptible() throws InterruptedException {
        run();
        if (_interrupted != null) throw _interrupted;
        return this;
    }
}
