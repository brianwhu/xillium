package org.xillium.gear.util;

import java.util.Random;
import java.util.logging.*;
import org.xillium.base.beans.Throwables;
import org.xillium.core.management.Reporting;


/**
 * <p>
 * A VitalTask is a Runnable that detects operation failure and undertakes retries automatically. An operation failure happens when method
 * execute() throws an exception before completion.</p>
 * <p>
 * Failure and recovery notifications are reported via an associated Reporting object.</p>
 * <p>
 * By default, a VitalTask uses randomized exponetial backoff to avoid retrying the task too eagerly. Other TrialStrategy can be used as well.</p>
 * <p>
 * A VitalTask is interruptible. If a VitalTask is submitted to a separate thread, call getInterruptedException() to detect execution interruption
 * after the thread has joined.  When running a VitalTask on the local thread, call runAsInterruptible() instead of run() to get interruption
 * reported as an InterruptedException.</p>
 */
public abstract class VitalTask<T extends Reporting> implements Runnable {
    protected static final Logger _logger = Logger.getLogger(VitalTask.class.getName());

    private final T _reporting;
    private final TrialStrategy _strategy;
    private final Runnable _preparation;

    private InterruptedException _interrupted;
    private int _age;

    /**
     * Constructs a VitalTask associated with the given Reporting object, adopting a randomized exponential-backoff trial strategy.
     */
    protected VitalTask(T reporting) {
        _reporting = reporting;
        _strategy = ExponentialBackoff.instance;
        _preparation = null;
    }

    /**
     * Constructs a VitalTask associated with the given Reporting object, adopting a randomized exponential-backoff trial strategy.
     * The provided Runnable is called before each trial of the task to perform any preparation work.
     */
    protected VitalTask(T reporting, Runnable preparation) {
        _reporting = reporting;
        _strategy = ExponentialBackoff.instance;
        _preparation = preparation;
    }

    /**
     * Constructs a VitalTask associated with the given Reporting object and the given trial strategy.
     */
    protected VitalTask(T reporting, TrialStrategy strategy) {
        _reporting = reporting;
        _strategy = strategy;
        _preparation = null;
    }

    /**
     * Constructs a VitalTask associated with the given Reporting object and the given trial strategy.
     * The provided Runnable is called before each trial of the task to perform any preparation work.
     */
    protected VitalTask(T reporting, TrialStrategy strategy, Runnable preparation) {
        _reporting = reporting;
        _strategy = strategy;
        _preparation = preparation;
    }

    @Deprecated
    protected T getManageable() {
        return _reporting;
    }

    protected T getReporting() {
        return _reporting;
    }

    /**
     * Performs the task. Subclass overrides this method to implement the task. This method must guarantee that when it fails (i.e. throws an
     * exception) it fails completely. Such guarantee allows the task to be meaningfully retried. In other words, if a task fails many times
     * through the retry cycles until it eventually succeeds, it makes the same effect as if it had succeeded on the first attempt.
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
                if (_preparation != null) _preparation.run();
                execute();
                if (_age > 0) {
                    _reporting.emit(Reporting.Severity.NOTICE, "Failure recovered: " + toString(), _age, _logger);
                }
                break;
            } catch (InterruptedException x) {
                _logger.log(Level.WARNING, "Interrupted, age = " + _age, x);
                _interrupted = x;
                break;
            } catch (Exception x) {
                _reporting.emit(x, "Failure detected, age = " + _age, _age, _logger);
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
