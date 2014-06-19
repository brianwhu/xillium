package org.xillium.gear.util;

import java.util.logging.*;
import java.util.concurrent.Callable;
import org.xillium.base.Functor;
import org.xillium.core.util.RemoteService;


/**
 * A utility to sync with a remote service by calling continuously until a "final" response is received.
 * Before reaching the final state, the service should return a "message" parameter indicating the progress
 * it goes through. When the service returns the final response, no "message" parameter is included in the
 * response.
 */
public class RemoteSynchronization {
    private static final long PAUSE_BETWEEN_RETRIES = 5000L;

    private final Callable<RemoteService.Response> _caller;
    private final Progressive.State _state;

    /**
     * Constructs a RemoteSynchronization equipped with a <code>Callable&lt;RemoteService.Response&gt;</code>
     * that does the actual calls to the remote service to produce a <code>RemoteService.Response</code>.
     */
    public RemoteSynchronization(Callable<RemoteService.Response> caller) {
        _caller = caller;
        _state = null;
    }

    /**
     * Constructs a RemoteSynchronization equipped with a <code>Callable&lt;RemoteService.Response&gt;</code>
     * that does the actual calls to the remote service to produce a <code>RemoteService.Response</code>,
     * associating the process with a <code>Progressive.State</code>.
     */
    public RemoteSynchronization(Callable<RemoteService.Response> caller, Progressive.State state) {
        _caller = caller;
        _state = state;
    }

    /**
     * Calls the remote service continuously until a "final" response is received.
     */
    public RemoteService.Response call(long pause) throws Exception {
        return call(pause, null);
    }

    /**
     * Calls the remote service continuously until a "final" response is received, reporting progress messages
     * via the reporter.
     */
    public <T> RemoteService.Response call(long pause, Functor<T, String> reporter) throws Exception {
        String message = null;

        while (true) {
            RemoteService.Response response = _caller.call();
            String news = response.params.get("message");
            if (news == null) {
                if (_state != null && message != null) {
                    _state.param = null;
                    _state.markAttempt();
                }
                return response;
            } else {
                if (!news.equals(message)) {
                    if (_state != null) {
                        if (reporter != null) reporter.invoke("Attempting " + _state.state + ": " + news);
                        _state.param = Progressive.State.clean(news);
                        _state.markAttempt();
                    } else {
                        if (reporter != null) reporter.invoke(news);
                    }
                    message = news;
                }

                Thread.sleep(pause < PAUSE_BETWEEN_RETRIES ? PAUSE_BETWEEN_RETRIES : pause);
            }
        }
    }
}
