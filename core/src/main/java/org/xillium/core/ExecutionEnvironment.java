package org.xillium.core;

import java.sql.*;
import java.util.Map;
import java.util.List;
import javax.sql.DataSource;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.*;
import org.xillium.core.*;
import org.springframework.jdbc.datasource.DataSourceUtils;


public class ExecutionEnvironment {
    private Dictionary _dictionary;
    private DataSource _dataSource;
    private Map<String, ParametricStatement> _statements;

    ExecutionEnvironment(Dictionary dictionary, DataSource source, Map<String, ParametricStatement> statements) {
        _dictionary = dictionary;
        _dataSource = source;
        _statements = statements;
    }

    /**
     * Acquires a data validation dictionary.
     */
    public <T extends DataObject> T collect(T data, DataBinder binder) throws SecurityException, DataValidationException {
        return _dictionary.collect(data, binder);
    }

    /**
     * Executes an UPDATE/DELETE statement.
     */
    public int executeUpdate(String name, DataObject object) throws SQLException {
        ParametricStatement statement = _statements.get(name);
        if (statement != null) {
            return statement.executeUpdate(DataSourceUtils.getConnection(_dataSource), object);
        } else {
            throw new RuntimeException("ParametricStatement '" + name + "' not found");
        }
    }

    /**
     * Executes an INSERT statement.
     */
    public long[] executeInsert(String name, DataObject object, boolean generatedKeys) throws SQLException {
        ParametricStatement statement = _statements.get(name);
        if (statement != null) {
            return statement.executeInsert(DataSourceUtils.getConnection(_dataSource), object, generatedKeys);
        } else {
            throw new RuntimeException("ParametricStatement '" + name + "' not found");
        }
    }

    /**
     * Executes a SELECT statement and passes the result set to the ResultSetWorker.
     */
    public <T> T executeSelect(String name, DataObject object, ParametricQuery.ResultSetWorker<T> worker) throws Exception {
        ParametricQuery statement = (ParametricQuery)_statements.get(name);
        if (statement != null) {
            return statement.executeSelect(DataSourceUtils.getConnection(_dataSource), object, worker);
        } else {
            throw new RuntimeException("ParametricQuery '" + name + "' not found");
        }
    }

    /**
     * Executes a SELECT statement and returns the result set as a list of objects
     */
    public <T extends DataObject> List<T> getResults(String name, DataObject object) throws Exception {
        ObjectMappedQuery<T> statement = (ObjectMappedQuery<T>)_statements.get(name);
        if (statement != null) {
            return statement.getResults(DataSourceUtils.getConnection(_dataSource), object);
        } else {
            throw new RuntimeException("ObjectMappedQuery '" + name + "' not found");
        }
    }

    public StringBuilder print(StringBuilder sb) {
        return sb.append("ExecutionEnvironment:Dictionary=").append(_dictionary.toString()).append(";DataSource=").append(_dataSource.toString());
    }
}
