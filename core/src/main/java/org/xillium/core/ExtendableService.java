package org.xillium.core;

import java.util.List;
import java.util.logging.*;
import org.xillium.data.*;
import org.xillium.core.util.CompoundFilter;


/**
 * ExtendableService is a basic implementation of Service.Extendable, providing functions
 * to install and invoke externally defined filters while still allowing subclasses to provide
 * extended functions.
 */
public abstract class ExtendableService implements Service.Extendable {
    private Service.Filter _filter;

    /**
     * Installs a service filter.
     */
    @Override
    public void setFilter(Service.Filter filter) {
        _filter = _filter == null ? filter : new CompoundFilter(filter, _filter);
    }

    /**
     * Installs a list of service filters. This method is mostly designed to facilitate Spring bean assembly.
     */
    public void setFilters(List<Service.Filter> filters) {
        for (Service.Filter filter: filters) setFilter(filter);
    }

    @Override
    public final void filtrate(DataBinder parameters) {
        filtrateRequest(parameters);
        if (_filter != null) _filter.filtrate(parameters);
    }

    @Override
    public final void successful(DataBinder parameters) throws Throwable {
        try { reportSuccessful(parameters); } catch (Throwable t) {}
        if (_filter != null) _filter.successful(parameters);
    }

    @Override
    public final void aborted(DataBinder parameters, Throwable throwable) throws Throwable {
        try { reportAborted(parameters, throwable); } catch (Throwable t) {}
        if (_filter != null) _filter.aborted(parameters, throwable);
    }

    @Override
    public final void complete(DataBinder parameters) throws Throwable {
        try { completeService(parameters); } catch (Throwable t) {}
        if (_filter != null) _filter.complete(parameters);
    }

    /**
     * Filtrates requests, before all other filters.
     */
    protected void filtrateRequest(DataBinder parameters) {}

    /**
     * Performs extra work upon service success, before all other filters.
     */
    protected void reportSuccessful(DataBinder parameters) throws Throwable {}

    /**
     * Performs extra work upon service failure, before all other filters.
     */
    protected void reportAborted(DataBinder parameters, Throwable throwable) throws Throwable {}

    /**
     * Completes the service, before all other filters.
     */
    protected void completeService(DataBinder parameters) throws Throwable {}
}
