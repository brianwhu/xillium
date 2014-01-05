package org.xillium.core.util;

import java.util.List;
import java.util.logging.*;
import java.sql.SQLException;
import org.springframework.transaction.annotation.*;
import org.xillium.base.etc.Pair;
import org.xillium.core.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;


/**
 * This class wraps a callable ParametricStatement into a Service. A Service.Filter can be installed to support extended behaviors.
 *
 * Any specified DataBinder parameter renaming (mapping) is also performed before the ParametricStatement is called.
 */
public class DatabaseCallableService extends SecuredService implements Service.Extended, DynamicService {
    private static final Logger _logger = Logger.getLogger(DatabaseCallableService.class.getName());

    private final Persistence _persistence;
    private final String _name;
    private final String _callable;
    private Pair<String, String>[] _renames;
    private Service.Filter _filter;

    public Class<? extends DataObject> getRequestType() {
        try {
            return _persistence.getParametricStatement(_callable).getDataObjectClass("com.yizheng.yep.data." + _name.replace('/', '.'));
        } catch (Exception x) {
            throw new RuntimeException("Failed to generate callable request class", x);
        }
    }

    public DatabaseCallableService(Persistence persistence, String name, String callable) throws Exception {
        _persistence = persistence;
        _name = name;
        _callable = callable;
    }

    /**
     * Defines parameter mappings. One parameter can be mapped to multiple new names, so a list is used instead of a map as the input to this method.
     */
    @SuppressWarnings("unchecked")
    public void setMappings(List<String> mappings) {
        if (mappings.size() > 0) {
            _renames = (Pair<String, String>[])new Pair[mappings.size()];
            for (int i = 0; i < _renames.length; ++i) {
                String[] names = mappings.get(i).split("[ ,]+");
                _renames[i] = new Pair<String, String>(names[0], names[1]);
            }
        }
    }

    public void setFilter(Service.Filter filter) {
        _filter = filter;
    }

    @Override
    @Transactional
    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
		try {
            for (Pair<String, String> pair: _renames) {
                binder.put(pair.second, binder.get(pair.first));
            }
            DataObject request = dict.collect(getRequestType().newInstance(), binder);
            persist.executeProcedure(_callable, request);
		} catch (SQLException x) {
            throw new org.springframework.transaction.TransactionSystemException(x.getMessage(), x);
		} catch (ServiceException x) {
			throw x;
		} catch (Exception x) {
			throw new ServiceException(x.getMessage(), x);
		}
        return binder;
    }

    /**
     * An extra step that runs before the service transaction starts.
     */
    @Override
    public void filtrate(DataBinder parameters) {
        if (_filter != null) _filter.filtrate(parameters);
    }

    /**
     * An extra step that runs after the service is successful and the associated transaction committed.
     * It will NOT run if the service has failed.
     */
    @Override
    public void successful(DataBinder parameters) throws Throwable {
        if (_filter != null) _filter.successful(parameters);
    }

    /**
     * An extra step that runs after the service has failed and the associated transaction rolled back.
     * It will NOT run if the service is successful.
     */
    @Override
    public void aborted(DataBinder parameters, Throwable throwable) throws Throwable {
        if (_filter != null) _filter.aborted(parameters, throwable);
    }

    /**
     * An extra step that always runs after the service has been completed, disregard whether the associated transaction is committed or rolled back.
     */
    @Override
    public void complete(DataBinder parameters) throws Throwable {
        if (_filter != null) _filter.complete(parameters);
    }
}
