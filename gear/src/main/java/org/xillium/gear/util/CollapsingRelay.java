package org.xillium.gear.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import org.xillium.base.Functor;


/**
 * A CollapsingRelay is a protective relay placed in front of a resource to collapse high frequency requests to
 * the resource into lower frequency, consolidated requests. This helps reduce contention on the resource.
 */
@lombok.extern.log4j.Log4j2
public class CollapsingRelay<T> extends Thread {
    private final long _laziness;
    private final Functor<Void, Collection<T>> _worker;
    private Collection<T> _list;
    private Collection<T> _free;

    /**
     * Constructs a CollapsingRelay that use a type of Collection to collapse incoming requests.
     *
     * @param laziness - number of milliseconds, defining a minimal time gap between 2 consecutive requests on the back end resource
     * @param collection - a Callable that produces the collection to use to collapse incoming requests
     * @param worker - the functor that performs the actual update
     * @throws Exception that is thrown from the Callable while creating a collection
     */
    public CollapsingRelay(long laziness, Callable<Collection<T>> collection, Functor<Void, Collection<T>> worker) throws Exception {
        super("CollapsingRelay["+worker.getClass().getName()+']');
        _laziness = laziness;
        _worker = worker;
        _list = collection.call();
        _free = collection.call();
    }

    /**
     * Constructs a CollapsingRelay that uses a Set to collapse incoming requests.
     *
     * @param laziness - the number of milliseconds between 2 consecutive requests
     * @param worker - the functor that performs the actual update
     */
    public CollapsingRelay(long laziness, Functor<Void, Collection<T>> worker) {
        super("CollapsingRelay["+worker.getClass().getName()+']');
        _laziness = laziness;
        _worker = worker;
        _list = new HashSet<T>();
        _free = new HashSet<T>();
    }

    /**
     * Submits a request to the back end resource.
     */
    public synchronized void submit(T signal) {
        _list.add(signal);
        notify();
    }

    public void run() {
        _log.trace("{} starts, laziness = {}", getName(), _laziness);

running:while (!isInterrupted()) {
            _log.trace("{}: waiting for next updates", getName());
            synchronized (this) {
                while (_list.size() == 0) {
                    try { wait(); } catch (InterruptedException x) { break running; }
                }
                Collection<T> l = _list;
                _list = _free;
                _free = l;
            }
            _log.trace("{}: received some updates", getName());

            while (!isInterrupted()) {
                try {
                    _log.trace("{}: performing requested update", getName());
                    _worker.invoke(_free);
                    _log.trace("{}:  completed requested update", getName());
                    break;
                } catch (Throwable t) {
                    _log.warn("{}: failure in requested update, will retry", getName());
                    try { Thread.sleep(500); } catch (InterruptedException x) { break running; }
                }
            }

            try { Thread.sleep(_laziness); } catch (InterruptedException x) { break running; }
            _free.clear();
        }

        _log.trace("{} closes", getName());
    }
}
