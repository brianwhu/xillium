package org.xillium.core.intrinsic;

import java.sql.Connection;
import java.sql.SQLIntegrityConstraintViolationException;
import javax.sql.DataSource;
import java.util.Map;
import java.util.logging.*;
import java.util.regex.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.*;
import org.xillium.data.persistence.crud.*;
import org.xillium.core.*;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service description.
 */
public class CrudService extends SecuredService implements Service.Extended, DynamicService {
    private static final Logger _logger = Logger.getLogger(CrudService.class.getName());
    private static final Pattern CONSTRAINT = Pattern.compile("\\([A-Z_]+\\.([A-Z_]+)\\)");

    private final CrudCommand _command;
    private boolean _isUnique;
    private boolean _debugging;
    private String _missing;
    private Service.Filter _filter;

	/**
	 * Creates a non-retrieval CRUD service. A CRUD service object is typically configured in service-configuration.xml.
	 *
	 * @param source - a DataSource object
	 * @param prefix - a package prefix for the generated Request class
	 * @param tables - a comma-separated list of tables
	 * @param action - the string name of one of the CRUD operations, as defined in enum type CrudCommand.Operation
	 */
    public CrudService(DataSource source, String prefix, String tables, String action) throws Exception {
        Connection connection = DataSourceUtils.getConnection(source);
        try {
            _command = new CrudCommand(connection, prefix, tables, new CrudCommand.Action(Enum.valueOf(CrudCommand.Operation.class, action)));
        } finally {
            connection.close();
        }
    }

    /**
     * Creates a non-retrieval CRUD service. A CRUD service object is typically configured in service-configuration.xml.
     *
     * @param source - a DataSource object
     * @param prefix - a package prefix for the generated Request class
     * @param tables - a comma-separated list of tables
     * @param action - the string name of one of the CRUD operations, as defined in enum type CrudCommand.Operation
     */
    public CrudService(DataSource source, String prefix, String tables, String action, Map<String, String> restrictions) throws Exception {
        Connection connection = DataSourceUtils.getConnection(source);
        try {
            _command = new CrudCommand(connection, prefix, tables, new CrudCommand.Action(Enum.valueOf(CrudCommand.Operation.class, action), restrictions));
        } finally {
            connection.close();
        }
    }

	/**
	 * Creates a RETRIEVAL or SEARCH service. A CRUD service object is typically configured in service-configuration.xml.
	 *
	 * @param source - a DataSource object
	 * @param prefix - a package prefix for the generated Request class
	 * @param tables - a comma-separated list of tables
	 * @param action - the string name of one of the CRUD operations, as defined in enum type CrudCommand.Operation
	 * @param columns - a comma-separated list of columns to be updated or to be searched by
	 */
    public CrudService(DataSource source, String prefix, String tables, String action, String... columns) throws Exception {
        Connection connection = DataSourceUtils.getConnection(source);
        try {
            _command = new CrudCommand(connection, prefix, tables, new CrudCommand.Action(Enum.valueOf(CrudCommand.Operation.class, action), columns));
        } finally {
            connection.close();
        }
    }

    /**
     * Creates a RETRIEVAL or SEARCH service. A CRUD service object is typically configured in service-configuration.xml.
     *
     * @param source - a DataSource object
     * @param prefix - a package prefix for the generated Request class
     * @param tables - a comma-separated list of tables
     * @param action - the string name of one of the CRUD operations, as defined in enum type CrudCommand.Operation
     * @param columns - a comma-separated list of columns to be updated or to be searched by
     */
    public CrudService(DataSource source, String prefix, String tables, String action, Map<String, String> restrictions, String... columns)
    throws Exception {
        Connection connection = DataSourceUtils.getConnection(source);
        try {
            _command = new CrudCommand(connection, prefix, tables,
                new CrudCommand.Action(Enum.valueOf(CrudCommand.Operation.class, action), columns, restrictions)
            );
        } finally {
            connection.close();
        }
    }

    public Class<? extends DataObject> getRequestType() {
        return _command.getRequestType();
    }

    public void setFilter(Service.Filter filter) {
        _filter = filter;
    }

    public void setUnique(boolean unique) {
        _isUnique = unique;
    }

    public void setDebugging(boolean debugging) {
        _debugging = debugging;
    }

    /**
     * Sets a message to throw when an expected row is missing.
     */
    public void setMissing(String missing) {
        _missing = missing;
    }

    /**
     * An extra step that runs before the service transaction starts.
     */
    public void filtrate(DataBinder parameters) {
        if (_filter != null) _filter.filtrate(parameters);
    }

    /**
     * An extra step that runs after the service is successful and the associated transaction committed.
     * It will NOT run if the service has failed.
     */
    public void successful(DataBinder parameters) {
        if (_filter != null) _filter.successful(parameters);
    }

    /**
     * An extra step that runs after the service has failed and the associated transaction rolled back.
     * It will NOT run if the service is successful.
     */
    public void aborted(DataBinder parameters, Throwable throwable) {
        if (_filter != null) _filter.aborted(parameters, throwable);
    }

    /**
     * An extra step that always runs after the service has been completed, disregard whether the associated transaction is committed or rolled back.
     */
    public void complete(DataBinder parameters) {
        if (_filter != null) _filter.complete(parameters);
    }

    @Transactional
    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
        int count = 0;
        try {
            DataObject request = dict.collect(_command.getRequestType().newInstance(), binder);
            if (_debugging) {
                _logger.info("CrudService.run: request = " + DataObject.Util.describe(request.getClass()));
                for (int i = 0; i < _command.getStatements().length; ++i) {
                    _logger.info(_command.getStatements()[i].getSQL());
                }
            }

            Connection connection = persist.getConnection();

            switch (_command.getOperation()) {
            case CREATE:
                for (ParametricStatement statement: _command.getStatements()) {
                    count += statement.executeInsert(connection, request, false).length;
                }
                binder.put("NumRowsInserted", String.valueOf(count));
                break;
            case UPDATE:
            case DELETE:
                for (ParametricStatement statement: _command.getStatements()) {
                    int rows = statement.executeUpdate(connection, request);
                    if (_missing != null && rows == 0) {
                        throw new ServiceException(_missing+statement.getTag());
                    }
                    count += rows;
                }
                binder.put("NumRowsAffected", String.valueOf(count));
                break;
            case RETRIEVE:
            case SEARCH:
                if (_isUnique) {
                    ((ParametricQuery)_command.getStatements()[0]).executeSelect(connection, request, binder);
                } else {
                    binder.putResultSet(_command.getName(),
                        ((ParametricQuery)_command.getStatements()[0]).executeSelect(connection, request, CachedResultSet.BUILDER)
                    );
                }
                break;
            }
        } catch (SQLIntegrityConstraintViolationException x) {
            Matcher matcher = CONSTRAINT.matcher(x.getMessage());
            if (matcher.find()) {
                String message = CrudConfiguration.icve.get(matcher.group(1));
                if (message != null) {
                    throw new ServiceException(message);
                } else {
                    throw new ServiceException(matcher.group(1), x);
                }
            } else {
                throw new ServiceException(x.getMessage(), x);
            }
        } catch (ServiceException x) {
            throw x;
        } catch (Exception x) {
            throw new ServiceException(x.getMessage(), x);
        }

        return binder;
    }
}
