package org.xillium.gear.util;

import java.util.concurrent.Executor;
import org.xillium.base.Functor;
import org.xillium.data.*;
import org.xillium.core.util.*;
import org.xillium.core.management.*;


/**
 * All remote service calls may end up with one of 3 outcomes:
 * <ol>
 * <li>clear success</li>
 * <li>clear failure</li>
 * <li>uncertainty</li>
 * </ol>
 * A CriticalService is a remote service that performs an important update to a resource and therefore an uncertain outcome must be managed
 * carefully from the caller's perspective. Based on the nature of the service, there are 2 strategies to manage this uncertainty.
 * <ol>
 * <li>The server might have completed the requested update, and assuming otherwise has the potential of incurring severe data or monetary losses.
 *     The caller therefore records the service call as successful, then starts sending RESUBMIT requests until a success is received, or a
 *     predefined number of requests have been made. The server must support the RESUBMIT version of the original service.</li>
 * <li>The server might have failed to make the requested update, and assuming otherwise has the potential of incurring severe data or monetary losses.
 *     The caller therefore records the service call as a failure, then starts sending ROLLBACK requests until a success is received, or a
 *     predefined number of requests have been made. The server must support the ROLLBACK version of the original service.</li>
 * </ol>
 */
public class CriticalService<T extends DataObject> {
    private static final int DEFAULT_RETRY_LIMIT = 6;

    final Executor executor;
    final String location;
    final String endpoint;
    final String recovery;
    final Functor<DataObject, T> repacker;
    final Reporting reporting;

    /**
     * Constructs a CriticalService.
     *
     * @param location - server base URL
     * @param endpoint - service endpoint
     * @param recovery - endpoint for either retransmission or rollback
     * @param repacker - a functor that repacks the request object for retransmission or rollback
     * @param reporting - a Reporting
     * @param executor - an Executor
     */
    public CriticalService(String location, String endpoint, String recovery, Functor<DataObject, T> repacker, Reporting reporting, Executor executor) {
        this.location = location;
        this.endpoint = endpoint;
        this.recovery = recovery;
        this.repacker = repacker;
        this.reporting = reporting;
        this.executor = executor;
    }

    /**
     * Starts a critical call and automatically manages uncertain outcomes.
     *
     * @param request - the request packet
     * @param process - a functor that processes an invocation/retransmission response. This functor's invoke method must
     *          <ol>
     *          <li>return null if the call is successful</li>
     *          <li>return a non-null message if the call ends in a clean failure</li>
     *          <li>throw a RuntimeException if the outcome is uncertain</li>
     *          </ol>
     *        If the retransmission limit has been reached, this functor is invoked for the last time with a null argument to allow any fallback
     *        processing deemed necessary. Any exception thrown during this invocation is silently ignored.
     * @param confirm - a functor to confirm that a rollback response is positive, and to throw an exception otherwise. This functor can be null
     *        to indicate that the remote call should be retransmissted rather than rolled back.
     */
    public void call(final T request, final Functor<String, RemoteService.Response> process, final Functor<Void, RemoteService.Response> confirm) {
        try {
            String message = process.invoke(RemoteService.call(location, endpoint, true, request));
            if (message != null) {
                // clean failure
                throw new RuntimeException(message);
            }
        } catch (RuntimeException x) {
            if (confirm == null) {
                executor.execute(new VitalTask<Reporting, Void>(reporting) {
                    protected Void execute() throws Exception {
                        if (getAge() > DEFAULT_RETRY_LIMIT) {
                            try { process.invoke(null); } catch (Exception x) {}
                        } else {
                            process.invoke(RemoteService.call(location, recovery, repacker.invoke(request)));
                        }
                        return null;
                    }
                });
                // positive!
            } else {
                executor.execute(new VitalTask<Reporting, Void>(reporting) {
                    protected Void execute() throws Exception {
                        if (getAge() > DEFAULT_RETRY_LIMIT) {
                            return null;
                        } else {
                            confirm.invoke(RemoteService.call(location, recovery, repacker.invoke(request)));
                        }
                        return null;
                    }
                });
                // negative!
                throw x;
            }
        }
    }

    /**
     * Simply invokes <code>call(request, process, null)</code>.
     */
    public void call(final T request, final Functor<String, RemoteService.Response> process) {
        call(request, process, null);
    }
}
