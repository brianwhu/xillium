package org.xillium.core.util;

import java.util.logging.*;
import org.xillium.base.etc.Pair;
import org.xillium.data.DataBinder;
import org.xillium.core.Service;
import org.xillium.core.ServiceException;


/**
 * A compound service filter that binds together a pair of filters.
 */
public class CompoundFilter extends Pair<Service.Filter, Service.Filter> implements Service.Filter {
    private static final Logger _logger = Logger.getLogger(CompoundFilter.class.getName());

    /**
     * Constructs a CompoundFilter.
     */
    public CompoundFilter(Service.Filter a, Service.Filter b) {
        super(a, b);
    }

    /**
     * Calls filters' filtrate() method. Any exception stops the process.
     */
    @Override
    public void filtrate(DataBinder parameters) {
        first.filtrate(parameters);
        second.filtrate(parameters);
    }

    /**
     * Calls both filters' successful() method, ignoring anything thrown.
     */
    @Override
    public void successful(DataBinder parameters) {
        try { first.successful(parameters);  } catch (Throwable t) { _logger.log(Level.WARNING, "successful()", t); }
        try { second.successful(parameters); } catch (Throwable t) { _logger.log(Level.WARNING, "successful()", t); }
    }

    /**
     * Calls both filters' aborted() method, ignoring anything thrown.
     */
    @Override
    public void aborted(DataBinder parameters, Throwable throwable) {
        try { first.aborted(parameters, throwable);  } catch (Throwable t) { _logger.log(Level.WARNING, "aborted()", t); }
        try { second.aborted(parameters, throwable); } catch (Throwable t) { _logger.log(Level.WARNING, "aborted()", t); }
    }

    /**
     * Calls both filters' complete() method, ignoring anything thrown.
     */
    @Override
    public void complete(DataBinder parameters) {
        try { first.complete(parameters);  } catch (Throwable t) { _logger.log(Level.WARNING, "complete()", t); }
        try { second.complete(parameters); } catch (Throwable t) { _logger.log(Level.WARNING, "complete()", t); }
    }
}
