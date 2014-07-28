package org.xillium.core.util;

import java.util.logging.*;
import org.xillium.base.etc.Pair;
import org.xillium.data.DataBinder;
import org.xillium.core.Service;
import org.xillium.core.ServiceException;


/**
 * A compound service filter that chains together a pair of filters.
 */
public class CompoundFilter extends Pair<Service.Filter, Service.Filter> implements Service.Filter {
    private static final Logger _logger = Logger.getLogger(CompoundFilter.class.getName());

    /**
     * Constructs a CompoundFilter that multicasts to the given channels.
     */
    public CompoundFilter(Service.Filter a, Service.Filter b) {
        super(a, b);
    }

    /**
     * An extra step that runs before the service transaction starts.
     */
    @Override
    public void filtrate(DataBinder parameters) throws ServiceException {
        first.filtrate(parameters);
        second.filtrate(parameters);
    }

    /**
     * An extra step that runs after the service is successful and the associated transaction committed.
     * It will NOT run if the service has failed.
     *
     * Anything thrown from this method is silently ignored.
     */
    @Override
    public void successful(DataBinder parameters) throws Throwable {
        try { first.successful(parameters);  } catch (Throwable t) { _logger.log(Level.WARNING, "successful()", t); }
        try { second.successful(parameters); } catch (Throwable t) { _logger.log(Level.WARNING, "successful()", t); }
    }

    /**
     * An extra step that runs after the service has failed and the associated transaction rolled back.
     * It will NOT run if the service is successful.
     *
     * Anything thrown from this method is silently ignored.
     */
    @Override
    public void aborted(DataBinder parameters, Throwable throwable) throws Throwable {
        try { first.aborted(parameters, throwable);  } catch (Throwable t) { _logger.log(Level.WARNING, "aborted()", t); }
        try { second.aborted(parameters, throwable); } catch (Throwable t) { _logger.log(Level.WARNING, "aborted()", t); }
    }

    /**
     * An extra step that always runs after the service has been completed, disregarding whether the associated transaction is committed or rolled back.
     *
     * Anything thrown from this method is silently ignored.
     */
    @Override
    public void complete(DataBinder parameters) throws Throwable {
        try { first.complete(parameters);  } catch (Throwable t) { _logger.log(Level.WARNING, "complete()", t); }
        try { second.complete(parameters); } catch (Throwable t) { _logger.log(Level.WARNING, "complete()", t); }
    }
}
