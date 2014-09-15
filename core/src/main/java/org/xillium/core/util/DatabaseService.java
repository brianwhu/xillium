package org.xillium.core.util;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.*;
import java.sql.SQLException;
import org.springframework.transaction.annotation.*;
import org.xillium.base.util.Pair;
import org.xillium.core.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.ParametricQuery;


/**
 * This class wraps a ParametricStatement into a Service. A Service.Filter can be installed to support extended behaviors.
 *
 * Any specified DataBinder parameter renaming (mapping) is also performed before the ParametricStatement is called.
 */
public class DatabaseService extends ExtendableAndSecured implements DynamicService {
    private static final Logger _logger = Logger.getLogger(DatabaseService.class.getName());
    private static final String RSET = "results";

    private final Persistence _persistence;
    private final String _statement;

    private Pair<String, String>[] _renames;
    private String _rset;
    private String _missing;
    private String _page;


    public Class<? extends DataObject> getRequestType() {
        try {
            return _persistence.getParametricStatement(_statement).getDataObjectClass(_statement.replace('/', '.'));
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
    public void setMappings(List<String> mappings) {
        if (mappings.size() > 0) {
            @SuppressWarnings("unchecked") Pair<String, String>[] renames = (Pair<String, String>[])Array.newInstance(Pair.class, mappings.size());
            for (int i = 0; i < _renames.length; ++i) {
                String[] names = mappings.get(i).split("[ ,]+");
                renames[i] = new Pair<String, String>(names[0], names[1]);
            }
            _renames = renames;
        }
    }

    /**
     * Specifies the name of the result set if the ParametricStatement is a query. If not specified, the name of the result set
     * is DatabaseService.RSET;
     */
    public void setResultSetName(String name) {
        _rset = name;
    }

    /**
     * Indicates that the query is expected to return exactly 1 row, whose columns are to be placed into the data binder as simple params.
     *
     * @param missing - the error message to use when the expected row is missing
     */
    public void setMissing(String missing) {
        _missing = missing;
    }

    /**
     * Specifies a page template to forward to after the service is complete.
     *
     * @param page - the location of the page template
     */
    public void setPage(String page) {
        _page = page;
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
                if (_missing != null) {
                    try {
                        persist.executeSelect(_statement, request, binder);
                    } catch (java.util.NoSuchElementException x) {
                        throw new ServiceException(_missing);
                    }
                } else {
                    binder.putResultSet(_rset != null ? _rset : RSET, persist.executeSelect(_statement, request, CachedResultSet.BUILDER));
                }
            } else {
                persist.executeProcedure(_statement, request);
            }
		} catch (SQLException x) {
            throw new org.springframework.transaction.TransactionSystemException(x.getMessage(), x);
		} catch (ServiceException x) {
			throw x;
		} catch (Exception x) {
			throw new ServiceException(x.getMessage(), x);
		} finally {
            if (_page != null) {
                binder.map(Service.SERVICE_HTTP_HEADER, String.class, String.class).put("Content-Type", "text/html; charset=utf-8");
                binder.put(Service.SERVICE_PAGE_TARGET, _page);
            }
        }
        return binder;
    }
}
