package org.xillium.core;

import java.util.List;
import java.util.logging.*;
import org.xillium.data.*;
import org.xillium.core.util.CompoundFilter;


/**
 * ExtendableService is a basic implementation of Service.Extendable, providing functions
 * to install and invoke filters.
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

    /**
     * An extra step that runs before the service transaction starts.
     */
    @Override
    public final void filtrate(DataBinder parameters) {
        if (_filter != null) _filter.filtrate(parameters);
    }

    /**
     * An extra step that runs after the service is successful and the associated transaction committed.
     * It will NOT run if the service has failed.
     */
    @Override
    public final void successful(DataBinder parameters) throws Throwable {
        if (_filter != null) _filter.successful(parameters);
    }

    /**
     * An extra step that runs after the service has failed and the associated transaction rolled back.
     * It will NOT run if the service is successful.
     */
    @Override
    public final void aborted(DataBinder parameters, Throwable throwable) throws Throwable {
        if (_filter != null) _filter.aborted(parameters, throwable);
    }

    /**
     * An extra step that always runs after the service has been completed, disregard whether the associated transaction is committed or rolled back.
     */
    @Override
    public final void complete(DataBinder parameters) throws Throwable {
        if (_filter != null) _filter.complete(parameters);
    }
}
