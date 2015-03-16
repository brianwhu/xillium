package lab;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.*;
import org.xillium.base.Functor;
import org.xillium.base.util.Pair;


public class PairTest {

    @SuppressWarnings("serial")
    public static class Chain extends Pair<Serializable, Serializable> implements Serializable {
        public Chain(Serializable a, Serializable b) {
            super(a, b);
        }
    }

    @Test(groups={"pair", "pair-cleansing"})
    public void cleansingTest() {
        String A0 = "A0";
        String A1 = "A1";
        String A2 = "A2";
        String A3 = "A3";
        String A4 = "A4";
        String A5 = "A5";
        String A6 = "A6";

        Chain chain = new Chain(A6, new Chain(A5, new Chain(A4, new Chain(A3, new Chain(A2, new Chain(A1, A0))))));

        assert Chain.count(chain) == 7;
        assert Chain.includes(chain, A6);
        assert Chain.includes(chain, A5);
        assert Chain.includes(chain, A4);
        assert Chain.includes(chain, A3);
        assert Chain.includes(chain, A2);
        assert Chain.includes(chain, A1);
        assert Chain.includes(chain, A0);
        chain = (Chain)Chain.cleanse(chain, A2);
        assert Chain.count(chain) == 6;
        assert Chain.includes(chain, A6);
        assert Chain.includes(chain, A5);
        assert Chain.includes(chain, A4);
        assert Chain.includes(chain, A3);
        assert !Chain.includes(chain, A2);
        assert Chain.includes(chain, A1);
        assert Chain.includes(chain, A0);
        chain = (Chain)Chain.cleanse(chain, A2);
        chain = (Chain)Chain.cleanse(chain, A4);
        chain = (Chain)Chain.cleanse(chain, A6);
        assert Chain.count(chain) == 4;
        assert !Chain.includes(chain, A6);
        assert Chain.includes(chain, A5);
        assert !Chain.includes(chain, A4);
        assert Chain.includes(chain, A3);
        assert !Chain.includes(chain, A2);
        assert Chain.includes(chain, A1);
        assert Chain.includes(chain, A0);
    }

    public static class Holder {
        public Serializable chain;

        public Holder(Serializable c) {
            chain = c;
        }
    }

    @Test(groups={"pair", "pair-concurrency"})
    public void concurrencyTest() {
        final Random random = new Random();
        final AtomicInteger threads = new AtomicInteger();
        List<String> tokens = Collections.synchronizedList(new ArrayList<String>());

        String A0 = "A0";   tokens.add(A0);
        String A1 = "A1";   tokens.add(A1);
        String A2 = "A2";   tokens.add(A2);
        String A3 = "A3";   tokens.add(A3);
        String A4 = "A4";   tokens.add(A4);
        String A5 = "A5";   tokens.add(A5);
        String A6 = "A6";   tokens.add(A6);

        final Holder holder = new Holder(new Chain(A6, new Chain(A5, new Chain(A4, new Chain(A3, new Chain(A2, new Chain(A1, A0)))))));

        for (int i = 0; i < 32; ++i) {
            new Thread(new Runnable() {
                public void run() {
                    final long[] count = new long[2];
                    threads.incrementAndGet();
                    while (Chain.traverse(holder.chain, new Functor<Void, Serializable>() {
                        public Void invoke(Serializable argument) {
                            assert argument != null;
                            ++count[0];
                            try { Thread.sleep(50 + random.nextInt(50)); } catch (InterruptedException x) {}
                            return null;
                        }
                    }) > 0) {
                        ++count[1];
                        try { Thread.sleep(100); } catch (InterruptedException x) {}
                    }
                    threads.decrementAndGet();
                    System.out.println(Thread.currentThread().getName() + " finished after " + count[1] + " iterations, " + count[0] + " visits");
                }
            }, "runner-" + i).start();
        }
        while (threads.get() < 32) try { Thread.sleep(200); } catch (InterruptedException x) {}
        System.out.println("Traversal threads running, count = " + threads.get());

        for (int i = 0; i < 256; ++i) {
            try { Thread.sleep(60); } catch (InterruptedException x) {}
            if (random.nextInt(100) < 75) {
                String z = "Z" + i;
                tokens.add(z);
                holder.chain = new Chain(z, holder.chain);
                System.out.print("+");
            } else {
                String a = tokens.remove(random.nextInt(tokens.size()));
                holder.chain = Chain.cleanse(holder.chain, a);
                System.out.print("-");
            }
        }
        System.out.println("\nAdd/cleanse cycle completed, chain length = " + Chain.count(holder.chain) + ", active traversal threads = " + threads.get());

        System.out.print("Cutting down elements in the chain ");
        while (tokens.size() > 0) {
            String a = tokens.remove(0);
            holder.chain = Chain.cleanse(holder.chain, a);
            System.out.print(".");
            try { Thread.sleep(100); } catch (InterruptedException x) {}
        }
        System.out.println("\nNow chain length = " + Chain.count(holder.chain));

        System.out.println("Waiting for " + threads.get() + " threads to terminate");
        while (threads.get() > 0) {
            try { Thread.sleep(100); } catch (InterruptedException x) {}
        }
    }
}
