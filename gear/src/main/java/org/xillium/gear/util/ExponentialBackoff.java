package org.xillium.gear.util;

import java.util.Random;


/**
 * A TrialStrategy that implements randomized exponential backoff
 */
public class ExponentialBackoff implements TrialStrategy {
    private static final long INI_BACKOFF =  1000;
    private static final long MAX_BACKOFF = 32000;
    private static final Random _random = new Random();

    /**
     * An instance of ExponentialBackoff.
     */
    public static final ExponentialBackoff instance = new ExponentialBackoff();

    /**
     * Observes till it is okay to carry on another round of trial.
     */
    @Override
    public void observe(int age) {
    }

    /**
     * Backs off after a failed trial.
     */
    @Override
    public void backoff(int age) throws InterruptedException {
        Thread.sleep(Math.min(MAX_BACKOFF, INI_BACKOFF + (int)Math.round(_random.nextDouble() * INI_BACKOFF * (1L << age))));
    }
}
