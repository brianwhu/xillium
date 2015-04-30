package lab;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.xillium.base.*;
import org.xillium.base.beans.*;
import org.testng.annotations.Test;


/**
 * Multithreading access to Singleton
 */
public class SingletonTest {
    private static final int SIZE = 64;
    private static final int COST = 128;
    private static final int SEED = 256;

    static enum Strategy {
        DCL_ARG0,
        DCL_ARG1,
        DCL_ARG2,
        SYN_FULL
    }

    static int VARIETY = Strategy.values().length;

    static Random random = new Random();
    static AtomicInteger sequence = new AtomicInteger();
    static Singleton<S> singleton = new Singleton<S>();

    public static class S {
        int value;

        public S(int v) throws Exception {
            Thread.sleep(COST);
            value = v;
        }
    }

    private static final Callable<S> provider0 = new Callable<S>() {
        public S call() throws Exception {
            return new S(sequence.incrementAndGet());
        }
    };

    private static final Functor<S, Integer> provider1 = new Functor<S, Integer>() {
        public S invoke(Integer base) {
            try {
                return new S(sequence.incrementAndGet());
            } catch (Exception x) {
                throw new RuntimeException(x.getMessage(), x);
            }
        }
    };

    private static final Factory<S> provider2 = new Factory<S>() {
        public S make(Object... args) {
            try {
                return new S(sequence.incrementAndGet());
            } catch (Exception x) {
                throw new RuntimeException(x.getMessage(), x);
            }
        }
    };

    public static class Runner implements Runnable {
        final Strategy strategy;
        long cost;

        public Runner(Strategy s) {
            strategy = s;
        }

        public void run() {
            sequence.incrementAndGet();
            try {
                synchronized(singleton) {
                    singleton.wait();
                }
                S s = null;
                int r1 = random.nextInt(SEED), r2 = random.nextInt(SEED);
                long t = System.nanoTime();
for (int i = 0; i < SIZE; ++i) {
                switch (strategy) {
                case DCL_ARG0:
                    s = singleton.get(provider0);
                    break;
                case DCL_ARG1:
                    s = singleton.get(provider1, r1);
                    break;
                case DCL_ARG2:
                    s = singleton.get(provider2, r1, r2);
                    break;
                default:
                    s = singleton.slow(provider2, r1);
                    break;
                }
}
                cost += System.nanoTime() - t;
                assert s != null;
                assert s.value == VARIETY*SIZE + 1;
            } catch (Exception x) {
                throw new RuntimeException(x.getMessage(), x);
            }
        }
    }

    @Test(groups={"singleton"})
    public void testSingleton() throws Exception {
        run();

        assert singleton.get(provider0).value == VARIETY*SIZE + 1;
        assert sequence.get() == VARIETY*SIZE + 1;

        sequence.set(0);

        System.out.println(String.format("Singleton tests with %d threads", VARIETY*SIZE));
        double[] cost = run();
        for (int i = 0; i < cost.length; ++i) {
            System.out.println(String.format("Strategy: %s, cost = %12.2f", Strategy.values()[i].toString(), cost[i]));
        }
    }

    private double[] run() throws Exception {
        Runner[] runners = new Runner[VARIETY*SIZE];
        Thread[] threads = new Thread[VARIETY*SIZE];
        for (int i = 0; i < VARIETY; ++i) {
            for (int j = 0; j < SIZE; ++j) {
                runners[i*SIZE + j] = new Runner(Strategy.values()[i]);
                threads[i*SIZE + j] = new Thread(runners[i*SIZE + j]);
                threads[i*SIZE + j].start();
            }
        }

        while (sequence.get() < threads.length) {
            Thread.sleep(100);
        }

        synchronized(singleton) {
            singleton.notifyAll();
        }

        for (int i = 0; i < threads.length; ++i) {
            threads[i].join();
        }

        long[] cost = new long[VARIETY];
        for (int i = 0; i < runners.length; ++i) {
            cost[runners[i].strategy.ordinal()] += runners[i].cost;
        }

        double[] average = new double[VARIETY];
        for (int i = 0; i < average.length; ++i) {
            average[i] = ((double)cost[i]) / ((double)SIZE);
        }

        return average;
    }
}
