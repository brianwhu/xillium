package lab.gear.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.xillium.base.Bifunctor;
import org.xillium.gear.util.*;
import org.testng.annotations.*;


public class CloseableFutureGroupTest {
    private static final int SIZE = 32;

    public static class Tester implements AutoCloseable, Runnable {
        private volatile boolean _working = true;

        @Override
        public void close() {
            _working = false;
        }

        @Override
        public void run() {
            while (_working) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException x) {
                    System.out.println("Interruption");
                }
            }
            System.out.println("Shutdown");
        }
    }

    private Bifunctor<Boolean, Integer, Integer> _waiting = new Bifunctor<Boolean, Integer, Integer>() {
        @Override
        public Boolean invoke(Integer count, Integer change) {
            if (change == -1) {
                System.out.println("Stopped waiting for Tester");
            }
            return false;
        }
    };

    private Bifunctor<Void, Tester, Exception> _closing = new Bifunctor<Void, Tester, Exception>() {
        @Override
        public Void invoke(Tester worker, Exception x) {
            if (x == null) {
                System.out.println("Forcefully closing " + worker);
            } else {
                System.out.println("Failure in forcing down Tester");
            }
            return null;
        }
    };


    @Test(groups={"gear-util", "futuregroup"})
    public void test() throws Exception {
        ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(SIZE);
        CloseableFutureGroup<Tester> cfg = new CloseableFutureGroup<>(_waiting, _closing);

        for (int i = 0; i < SIZE; ++i) {
            Tester t = new Tester();
            cfg.add(t, pool.submit(t));
        }

        assert cfg.size() == SIZE;
        Thread.sleep(100);
        cfg.close();
        assert cfg.isDone();
    }
}
