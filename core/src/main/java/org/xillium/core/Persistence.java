package org.xillium.core;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import javax.sql.DataSource;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.xillium.core.conf.*;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.support.DefaultTransactionDefinition;


public class Persistence {
    public static final SingleValueRetriever<BigDecimal> DecimalRetriever = new SingleValueRetriever<BigDecimal>();
    public static final SingleValueRetriever<Number> NumberRetriever = new SingleValueRetriever<Number>();
    public static final SingleValueRetriever<String> StringRetriever = new SingleValueRetriever<String>();

    private final DataSource _dataSource;
    private final Map<String, ParametricStatement> _statements;
    private PlatformTransactionManager _manager;
    private DefaultTransactionDefinition _readonly;

    /**
     * A task that can be wrapped in a Transaction.
     */
    public static interface Task<T, F> {
        public T run(F facility, Persistence persistence) throws Exception;
    }

    /**
     * Executes a task within a read-only transaction. Any exception rolls back the transaction and gets rethrown as a RuntimeException.
     */
    public <T, F> T doReadOnly(F facility, Task<T, F> task) {
        return doTransaction(facility, task, _readonly);
    }

    /**
     * Executes a task within a read-write transaction. Any exception rolls back the transaction and gets rethrown as a RuntimeException.
     */
    public <T, F> T doReadWrite(F facility, Task<T, F> task) {
        return doTransaction(facility, task, null);
    }

    /**
     * Constructs a Persistence that operates over the given data source.
     */
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
     * Returns a reference to the data source.
     */
    public DataSource getDataSource() {
        return _dataSource;
    }

    /**
     * Looks up a ParametricStatement by its name.
     */
    public ParametricStatement getParametricStatement(String name) {
        ParametricStatement statement = _statements.get(name);
        if (statement != null) {
            return statement;
        } else {
            throw new RuntimeException("ParametricStatement '" + name + "' not found");
        }
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
     * Executes a batch INSERT statement.
     */
    public int executeInsert(String name, Collection<? extends DataObject> objects) throws SQLException {
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
    public <T> T executeSelect(String name, DataObject object, ResultSetWorker<T> worker) throws Exception {
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
     * Executes a SELECT statement and returns a single row as an object
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
     * Executes a SELECT statement and return a single row as an object, with explicit type specification
     */
    public <T extends DataObject> T getObject(String name, DataObject object, Class<T> type) throws Exception {
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

    /**
     * Compile all ParametricStatement registered with this Persistence.
     */
    public int compile() throws SQLException {
        int count = 0;
        for (Map.Entry<String, ParametricStatement> entry: _statements.entrySet()) {
            try {
                DataSourceUtils.getConnection(_dataSource).prepareCall(entry.getValue().getSQL()).close();
                ++count;
            } catch (SQLException x) {
                throw new SQLException(entry.getKey(), x);
            }
        }
        return count;
    }

    public StringBuilder print(StringBuilder sb) {
        return sb.append("Persistence:DataSource=").append(_dataSource.toString());
    }

    public void setTransactionManager(PlatformTransactionManager manager) {
        _manager = manager;
        _readonly = new DefaultTransactionDefinition();
        _readonly.setReadOnly(true);
    }

    public PlatformTransactionManager getTransactionManager() {
        return _manager;
    }

    public void setIntrinsics(List<String> locations) {
        try {
            BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory(StorageConfiguration.class, _statements, "-");
            XMLBeanAssembler assembler = new XMLBeanAssembler(factory);

            for (String location: locations) {
                try {
                    assembler.build(getClass().getResourceAsStream(location));
                } catch (Exception x) {
                    System.err.println("Failure in loading configuration: " + x.getMessage());
                }
            }
        } catch (Exception x) {
            System.err.println("Failure in object assembly: " + x.getMessage());
        }
    }

    Map<String, ParametricStatement> getStatementMap() {
        return _statements;
    }

    private final <T, F> T doTransaction(F facility, Task<T, F> task, TransactionDefinition definition) {
        TransactionStatus transaction = _manager.getTransaction(definition);
        try {
            T value = task.run(facility, this);
            _manager.commit(transaction);
            return value;
        } catch (Exception x) {
            try { _manager.rollback(transaction); } catch (IllegalTransactionStateException t) { /* already rolled back by database */ }
            throw (x instanceof RuntimeException) ? (RuntimeException)x : new RuntimeException(x.getMessage(), x);
        }
    }
}
