package lab;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.xillium.base.Singleton;
import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * Multithreading access to Singleton
 */
public class SingletonTest {
    private static final int SIZE = 64;
    private static final int COST = 64;

    static AtomicInteger sequence = new AtomicInteger();
    static Singleton<S> singleton = new Singleton<S>();

    public static class S {
        int value;

        public S(int v) throws Exception {
            Thread.sleep(COST);
            value = v;
        }
    }

    public static class Provider implements Callable<S> {
        public S call() throws Exception {
            return new S(sequence.incrementAndGet());
        }
    }

    static Provider provider = new Provider();

    public static class Runner implements Runnable {
        public void run() {
            sequence.incrementAndGet();
            try {
                synchronized(singleton) {
                    singleton.wait();
                }
                S s = singleton.get(provider);
                assert s != null;
                assert s.value == SIZE + 1;
            } catch (Exception x) {
                throw new RuntimeException(x.getMessage(), x);
            }
        }
    }

    @Test(groups={"singleton"})
    public void testSingleton() throws Exception {
        Thread[] runners = new Thread[SIZE];
        for (int i = 0; i < runners.length; ++i) {
            runners[i] = new Thread(new Runner());
            runners[i].start();
        }

        while (sequence.get() < SIZE) {
            Thread.sleep(100);
        }

        synchronized(singleton) {
            singleton.notifyAll();
        }

        for (int i = 0; i < runners.length; ++i) {
            runners[i].join();
        }

        assert singleton.get(provider).value == SIZE + 1;
        assert sequence.get() == SIZE + 1;
    }
}
