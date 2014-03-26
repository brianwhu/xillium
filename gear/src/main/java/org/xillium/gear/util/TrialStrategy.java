package org.xillium.gear.util;


/**
 * A TrialStrategy is used by a VitalTask to formulate a sound trial-failure-retry cycle.
 */
public interface TrialStrategy {
    /**
     * Observes until it is okay to carry on another round of trial.
     */
    public void observe(int age) throws InterruptedException;

    /**
     * Backs off after a failed trial.
     */
    public void backoff(int age) throws InterruptedException;
}
