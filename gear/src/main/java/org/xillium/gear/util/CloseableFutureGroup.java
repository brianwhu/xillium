package org.xillium.gear.util;

import java.util.*;
import java.util.concurrent.Future;
import org.xillium.base.Bifunctor;
import org.xillium.base.util.Pair;


/**
 * A CloseableFutureGroup is an AutoCloseable for monitoring and closing of concurrent Futures associated with Closeable tasks.
 */
public class CloseableFutureGroup<T extends AutoCloseable> implements AutoCloseable {
    private final List<Pair<T, Future<?>>> _pairs = new ArrayList<>();
    private Bifunctor<Boolean, Integer, Integer> _waiting;
    private Bifunctor<Void, T, Exception> _closing;

    /**
     * Constructs a CloseableFutureGroup.
     */
    public CloseableFutureGroup() {
    }

    /**
     * Constructs a CloseableFutureGroup.
     *
     * @param waiting the waiting attendant
     * @param closing the closing attendant
     */
    public CloseableFutureGroup(Bifunctor<Boolean, Integer, Integer> waiting, Bifunctor<Void, T, Exception> closing) {
        _waiting = waiting;
        _closing = closing;
    }

    /**
     * Configures a <i>waiting attendant</i>, which during the waiting phase is to
     * <ol>
     * <li>receive invocations with 2 arguments: the number of futures that has completed so far, and how much this number has
     *     changed since the last invocation,</li> 
     * <li>return a Boolean value indicating whether to continue waiting for the rest of the futures, or to stop and enter the
     *     closing phase.</li>
     * </ol>
     * This attendant will be invoked many times during the waiting phase. After the waiting phase has ended, this report
     * will be invoked one more time with the current number of futures that has completed, and -1 as the second argument.
     *
     * @param waiting the waiting attendant
     */
    public void setWaitingAttendant(Bifunctor<Boolean, Integer, Integer> waiting) {
        _waiting = waiting;
    }

    /**
     * Configures a <i>closing attendant</i>, which during the closing phase is to
     * <ol>
     * <li>receive invocations with 2 arguments: the AutoCloseable that is being closed, and an exception thrown during the closing
     *     attempt.</li>
     * </ol>
     * This attendant will be invoked for each (AutoCloseable, Future) pair before the pair is closed, receiving null as the second
     * argument. If an exception is caught duing the closing attempt, this attendant is invoked again receiving the caught exception
     * as the second argument.
     *
     * @param closing the closing attendant
     */
    public void setClosingAttendant(Bifunctor<Void, T, Exception> closing) {
        _closing = closing;
    }

    /**
     * Adds an AutoCloseable task and associated Future to this group.
     */
    public void add(T closeable, Future<?> future) {
        _pairs.add(new Pair<T, Future<?>>(closeable, future));
    }

    /**
     * Reports the total number of futures in this CloseableFutureGroup.
     */
    public int size() {
        return _pairs.size();
    }

    /**
     * Reports whether all futures in this group has completed.
     */
    public boolean isDone() {
        int count = 0;
        for (Pair<T, Future<?>> pair: _pairs) {
            if (pair.second.isDone()) ++count;
        }
        return count == _pairs.size();
    }

    /**
     * Closes all Futures.
     * Waits for futures to complete, then closes those still running after the "waiting phase" has ended.
     */
    @Override
    public void close() {
        // waiting phase
        int count = 0, last = 0;
        do {
            last = count;
            try { Thread.sleep(500); } catch (Exception x) {}
            count = 0;
            for (Pair<T, Future<?>> pair: _pairs) {
                if (pair.second.isDone()) ++count;
            }
        } while ((_waiting == null || _waiting.invoke(count, count - last)) && count < _pairs.size());
        if (_waiting != null) _waiting.invoke(count, -1);

        // closing phase
        for (Pair<T, Future<?>> pair: _pairs) {
            if (!pair.second.isDone()) {
                try {
                    if (_closing != null) _closing.invoke(pair.first, null);
                    pair.second.cancel(true);
                    pair.first.close();
                } catch (Exception x) {
                    if (_closing != null) _closing.invoke(pair.first, x);
                }
            }
        }
    }
}
