package org.xillium.gear.util;

import java.util.Random;


/**
 * A TrialStrategy that implements randomized exponential backoff
 */
public class ExponentialBackoff implements TrialStrategy {
    public static final long INIT_BACKOFF = 1000;
    public static final long MAX_EXPONENT = 6;

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
        Thread.sleep(randomizedExponentialSequence(age));
    }

    public static long randomizedExponentialSequence(int age) {
        return INIT_BACKOFF + (long)Math.round(_random.nextDouble() * INIT_BACKOFF * (1L << Math.min(MAX_EXPONENT, age)));
    }
}
