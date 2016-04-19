package org.xillium.gear.util;


/**
 * TimeLimitedStrategy is a TrialStrategy that implements fixed time delay between retries and a overall time limit.
 */
public class TimeLimitedStrategy implements TrialStrategy {
    private final long _limit, _delay;

    /**
     * Constructs a TimeLimitedStrategy
     */
    public TimeLimitedStrategy(long limit, long delay) {
        _limit = limit;
        _delay = delay;
    }

    /**
     * Checks the current system time against the time limit, throwing an InterruptedException if the time is up.
     */
    @Override
    public void observe(int age) throws InterruptedException {
        if (System.currentTimeMillis() >= _limit) {
            throw new InterruptedException("Time is up");
        }
    }

    /**
     * Sleeps for a delay whose length is specified at contruction.
     */
    @Override
    public void backoff(int age) throws InterruptedException {
        Thread.sleep(_delay);
    }
}
