package org.xillium.data.persistence;

import org.xillium.data.*;
import java.sql.*;
import java.util.*;
import java.lang.reflect.Field;


/**
 * A prepared SQL statement with named parameters. A parameter accepts null value if its name ends with '?'.
 */
public class ParametricStatement {
    public static class Param {
        public final boolean nullable;
        public final String name;
        public final int type;

        /**
         * Constructs a named formal parameter with name <code>n</code> and type <code>t</code>.
         *
         * If n ends with '?' the parameter accepts null values.
         */
        public Param(String n, int t) {
            if (n.endsWith("?")) {
                nullable = true;
                name = n.substring(0, n.length()-1);
            } else {
                nullable = false;
                name = n;
            }
            type = t;
        }
    };

    public ParametricStatement(Param[] parameters, String sql) throws IllegalArgumentException {
        _params = parameters;
        set(sql);
    }

    public ParametricStatement(String parameters) throws IllegalArgumentException {
        if (parameters != null && parameters.length() > 0) {
            String[] params = parameters.split(",");
            _params = new Param[params.length];
            for (int i = 0; i < params.length; ++i) {
                int colon = params[i].indexOf(':');
                if (colon > 0) {
                    try {
                        int type = java.sql.Types.class.getField(params[i].substring(colon+1)).getInt(null);
                        _params[i] = new Param(params[i].substring(0, colon), type);
                    } catch (Exception x) {
                        throw new IllegalArgumentException("Parameter specification", x);
                    }
                } else {
                    throw new IllegalArgumentException("Parameter specification: missing type in " + params[i]);
                }
            }
        } else {
            throw new NullPointerException();
        }
    }

    public void set(String sql) throws IllegalArgumentException {
        int count = 0;
        int qmark = sql.indexOf('?');
        while (qmark > 0) {
            ++count;
            qmark = sql.indexOf('?', qmark+1);
        }
        if (_params.length == count) {
            _sql = sql;
        } else {
            throw new IllegalArgumentException("Wrong number of parameters in '" + sql +'\'');
        }
    }

    protected PreparedStatement prepare(Connection conn, DataObject object) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(_sql, Statement.RETURN_GENERATED_KEYS);

        Class type = object.getClass();
        //statement.clearParameters();
        for (int i = 0; i < _params.length; ++i) {
            try {
                Field field = type.getDeclaredField(_params[i].name);
                field.setAccessible(true);
                statement.setObject(i+1, field.get(object), _params[i].type);
            } catch (NoSuchFieldException x) {
                if (_params[i].nullable) {
                    statement.setNull(i+1, _params[i].type);
                } else {
                    statement.close();
            throw new SQLException("Failed to retrieve '" + _params[i].name + "' from DataObject (" + type.getName() + ')', x);
                }
            } catch (Exception x) {
                statement.close();
            throw new SQLException("Failed to retrieve '" + _params[i].name + "' from DataObject (" + type.getName() + ')', x);
            }
        }
        return statement;
    }

/*
    private void prepare(Connection conn, DataBinder binder) throws SQLException {
        if (_statement == null) {
            _statement = conn.prepareStatement(_sql);
        }

        _statement.clearParameters();
        for (int i = 0; i < _parameters.length; ++i) {
            try {
                _statement.setObject(i+1, ?, _parameters[i].type);
            } catch (Exception x) {
                throw new SQLException("Failed to retrieve parameter from DataBinder", x);
            }
        }
    }
*/

    /**
     * Executes an UPDATE or DELETE statement
     */
    public int executeUpdate(Connection conn, DataObject object) throws SQLException {
        PreparedStatement statement = prepare(conn, object);
        try {
            return statement.executeUpdate();
        } finally {
            statement.close();
        }
    }

    /**
     * Executes an INSERT statement
     */
    public long[] executeInsert(Connection conn, DataObject object, boolean generatedKeys) throws SQLException {
        PreparedStatement statement = prepare(conn, object);
        try {
            long[] keys = new long[statement.executeUpdate()];
            if (generatedKeys) {
                ResultSet rs = statement.getGeneratedKeys();
                for (int i = 0; rs.next(); ++i) {
                    keys[i] = rs.getLong(1);
                }
            }
            return keys;
        } finally {
            statement.close();
        }
    }

/*
    public ResultSet intoResultSet(Connection conn, DataObject object) throws SQLException {
        PreparedStatement statement = prepare(conn, object);
        try {
            return statement.executeQuery();
        } finally {
            statement.close();
        }
    }

    public <T> void executeQuery(Connection conn, DataObject object, ResultSetWorker<T> worker) throws Exception {
        PreparedStatement statement = prepare(conn, object);
        try {
            return worker.process(statement.executeQuery());
        } finally {
            statement.close();
        }
    }
*/
    public StringBuilder print(StringBuilder sb) {
        sb.append('[');
        for (Param param: _params) {
            sb.append('<').append(param.name).append('>');
        }
        sb.append(']').append(_sql);
        return sb;
    }

    private final Param[] _params;
    private String _sql;
}
