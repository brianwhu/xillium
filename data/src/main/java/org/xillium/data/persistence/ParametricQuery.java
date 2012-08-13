package org.xillium.data.persistence;

import org.xillium.data.*;
import java.sql.*;
import java.util.*;


/**
 * A prepared SQL SELECT statement with named parameters.
 */
public class ParametricQuery extends ParametricStatement {
	/**
	 * All query results are passed to a ResultSetWorker, which processes the result set and produces an object of type T.
	 */
    public static interface ResultSetWorker<T> {
        public T process(ResultSet rs) throws Exception;
    }

    /**
     * Constructs a ParametricQuery with the named parameters and the SQL string.
     */
    public ParametricQuery(Param[] parameters, String sql) throws IllegalArgumentException {
		super(parameters, sql);
    }

    /**
     * Constructs a ParametricQuery with the named parameters in a coded string.
     */
    public ParametricQuery(String parameters) throws IllegalArgumentException {
        super(parameters);
    }

    /**
     * Constructs a ParametricQuery with the named parameters embedded in the SQL.
     */
    public ParametricQuery() throws IllegalArgumentException {
    }

    /**
     * Executes the SELECT statement, passing the result set to the ResultSetWorker for processing.
     *
     * The ResultSetWorker must close the result set before returning.
     */
    public <T> T executeSelect(Connection conn, DataObject object, ResultSetWorker<T> worker) throws Exception {
        PreparedStatement statement = conn.prepareStatement(_sql);
        try {
            load(statement, object);
            return worker.process(statement.executeQuery());
        } finally {
            statement.close();
        }
    }
}
