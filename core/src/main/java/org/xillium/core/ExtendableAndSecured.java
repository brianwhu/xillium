package org.xillium.core;

import java.util.List;
import java.util.logging.*;
import org.xillium.data.*;
import org.xillium.core.util.CompoundServiceFilter;


/**
 * An implementation of Service.Extendable and a subclass of SecuredService, this abstract service provides a
 * starting point for services that are both extendable and secured.
 */
public abstract class ExtendableAndSecured extends SecuredService implements Service.Extendable {
    private Service.Filter _filter;

    /**
     * Installs a service filter.
     */
    @Override
    public void setFilter(Service.Filter filter) {
        _filter = _filter == null ? filter : new CompoundServiceFilter(filter, _filter);
    }

    /**
     * Installs a list of service filters. This method is designed to facilitate Spring bean assembly.
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
    public final void acknowledge(DataBinder parameters) throws Exception {
        try { acknowledgeReception(parameters); } catch (Throwable t) {}
        if (_filter != null) _filter.acknowledge(parameters);
    }

    @Override
    public final void successful(DataBinder parameters) throws Exception {
        try { reportSuccessful(parameters); } catch (Throwable t) {}
        if (_filter != null) _filter.successful(parameters);
    }

    @Override
    public final void aborted(DataBinder parameters, Throwable throwable) throws Exception {
        try { reportAborted(parameters, throwable); } catch (Throwable t) {}
        if (_filter != null) _filter.aborted(parameters, throwable);
    }

    @Override
    public final void complete(DataBinder parameters) throws Exception {
        try { completeService(parameters); } catch (Throwable t) {}
        if (_filter != null) _filter.complete(parameters);
    }

    /**
     * Filtrates requests, before all other filters.
     */
    protected void filtrateRequest(DataBinder parameters) {}

    /**
     * Acknowledges reception, before all other filters.
     */
    protected void acknowledgeReception(DataBinder parameters) throws Exception {}

    /**
     * Performs extra work upon service success, before all other filters.
     */
    protected void reportSuccessful(DataBinder parameters) throws Exception {}

    /**
     * Performs extra work upon service failure, before all other filters.
     */
    protected void reportAborted(DataBinder parameters, Throwable throwable) throws Exception {}

    /**
     * Completes the service, before all other filters.
     */
    protected void completeService(DataBinder parameters) throws Exception {}
}
