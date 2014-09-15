package org.xillium.core.util;

import org.xillium.base.util.Pair;
import org.xillium.data.DataBinder;
import org.xillium.core.Service;
import org.xillium.core.ServiceException;


/**
 * A compound service filter that binds together a pair of filters.
 */
public class CompoundServiceFilter extends Pair<Service.Filter, Service.Filter> implements Service.Filter {

    /**
     * Constructs a CompoundServiceFilter.
     */
    public CompoundServiceFilter(Service.Filter a, Service.Filter b) {
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
     * Calls both filters' acknowledge() method, ignoring anything thrown.
     */
    @Override
    public void acknowledge(DataBinder parameters) {
        try { first.acknowledge(parameters);  } catch (Throwable t) {}
        try { second.acknowledge(parameters); } catch (Throwable t) {}
    }

    /**
     * Calls both filters' successful() method, ignoring anything thrown.
     */
    @Override
    public void successful(DataBinder parameters) {
        try { first.successful(parameters);  } catch (Throwable t) {}
        try { second.successful(parameters); } catch (Throwable t) {}
    }

    /**
     * Calls both filters' aborted() method, ignoring anything thrown.
     */
    @Override
    public void aborted(DataBinder parameters, Throwable throwable) {
        try { first.aborted(parameters, throwable);  } catch (Throwable t) {}
        try { second.aborted(parameters, throwable); } catch (Throwable t) {}
    }

    /**
     * Calls both filters' complete() method, ignoring anything thrown.
     */
    @Override
    public void complete(DataBinder parameters) {
        try { first.complete(parameters);  } catch (Throwable t) {}
        try { second.complete(parameters); } catch (Throwable t) {}
    }
}
