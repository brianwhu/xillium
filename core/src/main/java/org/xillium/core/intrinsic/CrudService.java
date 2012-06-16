package org.xillium.core.intrinsic;

import java.sql.Connection;
import javax.sql.DataSource;
import java.util.logging.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.*;
import org.xillium.data.persistence.crud.CrudCommand;
import org.xillium.core.*;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service description.
 */
public class CrudService extends SecuredService implements DynamicService {
    private static final Logger _logger = Logger.getLogger(CrudService.class.getName());

    private final CrudCommand _command;

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

    public Class<? extends DataObject> getRequestType() {
        return _command.getRequestType();
    }

    @Transactional
    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
        int count = 0;
        try {
            DataObject request = dict.collect(_command.getRequestType().newInstance(), binder);
_logger.info("CrudService.run: request = " + DataObject.Util.describe(request.getClass()));
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
                    count += statement.executeUpdate(connection, request);
                }
                binder.put("NumRowsAffected", String.valueOf(count));
                break;
            case RETRIEVE:
            case SEARCH:
                CachedResultSet rs = ((ParametricQuery)_command.getStatements()[0]).executeSelect(connection, request, new CachedResultSet.Builder());
                binder.putResultSet(_command.getName(), rs);
                break;
            }
        } catch (Exception x) {
            throw new ServiceException(x.getMessage(), x);
        }

        return binder;
    }
}
