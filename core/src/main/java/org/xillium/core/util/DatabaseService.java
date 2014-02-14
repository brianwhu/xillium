package org.xillium.core.util;

import java.util.List;
import java.util.logging.*;
import java.sql.SQLException;
import org.springframework.transaction.annotation.*;
import org.xillium.base.etc.Pair;
import org.xillium.core.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.ParametricQuery;


/**
 * This class wraps a ParametricStatement into a Service. A Service.Filter can be installed to support extended behaviors.
 *
 * Any specified DataBinder parameter renaming (mapping) is also performed before the ParametricStatement is called.
 */
public class DatabaseService extends SecuredService implements Service.Extended, DynamicService {
    private static final Logger _logger = Logger.getLogger(DatabaseService.class.getName());
    private static final String RSET = "results";

    private final Persistence _persistence;
    private final String _statement;

    private Pair<String, String>[] _renames;
    private Service.Filter _filter;
    private String _rset;


    public Class<? extends DataObject> getRequestType() {
        try {
            return _persistence.getParametricStatement(_statement).getDataObjectClass("org.xillium.core.util.dynamic.data." + _statement.replace('/', '.'));
        } catch (Exception x) {
            throw new RuntimeException("Failed to obtain database service request class", x);
        }
    }

    /**
     * Constructs a DatabaseService that uses the Persistence and wraps the named ParametricStatement into a service.
     */
    public DatabaseService(Persistence persistence, String statement) throws Exception {
        _persistence = persistence;
        _statement = statement;
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

    /**
     * Installs a service filter.
     */
    public void setFilter(Service.Filter filter) {
        _filter = filter;
    }

    /**
     * Specifies the name of the result set if the ParametricStatement is a query. If not specified, the name of the result set
     * is DatabaseService.RSET;
     */
    public void setResultSetName(String name) {
        _rset = name;
    }

    @Override
    @Transactional
    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
		try {
            if (_renames != null) for (Pair<String, String> pair: _renames) {
                binder.put(pair.second, binder.get(pair.first));
            }
            DataObject request = dict.collect(getRequestType().newInstance(), binder);
            if (persist.getParametricStatement(_statement) instanceof ParametricQuery) {
                binder.putResultSet(_rset != null ? _rset : RSET, persist.executeSelect(_statement, request, CachedResultSet.BUILDER));
            } else {
                persist.executeProcedure(_statement, request);
            }
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
