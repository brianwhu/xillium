package org.xillium.core;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import javax.sql.DataSource;
import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.springframework.jdbc.datasource.DataSourceUtils;


public class Persistence {
    public static final SingleValueRetriever<BigDecimal> NumberRetriever = new SingleValueRetriever<BigDecimal>();
    public static final SingleValueRetriever<String> StringRetriever = new SingleValueRetriever<String>();

    private final DataSource _dataSource;
    private final Map<String, ParametricStatement> _statements;

    public Persistence(DataSource source) {
        _dataSource = source;
        _statements = new HashMap<String, ParametricStatement>();
    }

    /**
     * Obtains a connection that is bound to the current transaction, which will be released automatically
     * upon transaction commit/rollback.
     */
    public Connection getConnection() {
        return DataSourceUtils.getConnection(_dataSource);
    }

    /**
     * Executes an UPDATE/DELETE statement or an anonymous block.
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
     * Executes an UPDATE/DELETE statement with data from a collection of DataObjects.
     */
    public int executeUpdate(String name, Collection<? extends DataObject> objects) throws SQLException {
        ParametricStatement statement = _statements.get(name);
        if (statement != null) {
            return statement.executeUpdate(DataSourceUtils.getConnection(_dataSource), objects);
        } else {
            throw new RuntimeException("ParametricStatement '" + name + "' not found");
        }
    }

    /**
     * Executes an stored procedure or function
     */
    public int executeProcedure(String name, DataObject object) throws SQLException {
        ParametricStatement statement = _statements.get(name);
        if (statement != null) {
            return statement.executeProcedure(DataSourceUtils.getConnection(_dataSource), object);
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
     * Executes a batch INSERT statement.
     */
    public int executeInsert(String name, DataObject[] objects) throws SQLException {
        ParametricStatement statement = _statements.get(name);
        if (statement != null) {
            return statement.executeInsert(DataSourceUtils.getConnection(_dataSource), objects);
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
        @SuppressWarnings("unchecked")
        ObjectMappedQuery<T> statement = (ObjectMappedQuery<T>)_statements.get(name);
        if (statement != null) {
            return statement.getResults(DataSourceUtils.getConnection(_dataSource), object);
        } else {
            throw new RuntimeException("ObjectMappedQuery '" + name + "' not found");
        }
    }

    /**
     * Executes a SELECT statement and returns the result set as a list of objects, with explicit type specification
     */
    public <T extends DataObject> List<T> getResults(String name, DataObject object, Class<T> type) throws Exception {
        @SuppressWarnings("unchecked")
        ObjectMappedQuery<T> statement = (ObjectMappedQuery<T>)_statements.get(name);
        if (statement != null) {
            return statement.getResults(DataSourceUtils.getConnection(_dataSource), object);
        } else {
            throw new RuntimeException("ObjectMappedQuery '" + name + "' not found");
        }
    }

    /**
     * Executes a SELECT statement and returns the result set as a list of objects
     */
    public <T extends DataObject> T getObject(String name, DataObject object) throws Exception {
        @SuppressWarnings("unchecked")
        ObjectMappedQuery<T> statement = (ObjectMappedQuery<T>)_statements.get(name);
        if (statement != null) {
            return statement.getObject(DataSourceUtils.getConnection(_dataSource), object);
        } else {
            throw new RuntimeException("ObjectMappedQuery '" + name + "' not found");
        }
    }

    /**
     * Executes a SELECT statement and returns the result set as a list of objects
     */
    public <T extends DataObject> Collector<T> getResults(String name, DataObject object, Collector<T> collector) throws Exception {
        @SuppressWarnings("unchecked")
        ObjectMappedQuery<T> statement = (ObjectMappedQuery<T>)_statements.get(name);
        if (statement != null) {
            return statement.getResults(DataSourceUtils.getConnection(_dataSource), object, collector);
        } else {
            throw new RuntimeException("ObjectMappedQuery '" + name + "' not found");
        }
    }

    public StringBuilder print(StringBuilder sb) {
        return sb.append("Persistence:DataSource=").append(_dataSource.toString());
    }

    Map<String, ParametricStatement> getStatementMap() {
        return _statements;
    }
}
