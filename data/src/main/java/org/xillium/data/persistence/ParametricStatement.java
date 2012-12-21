package org.xillium.data.persistence;

import org.xillium.base.beans.Beans;
import org.xillium.data.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
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
         * If the name ends with '?' the parameter accepts null values.
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
            String[] params = parameters.trim().split("\\s*,\\s*");
            _params = new Param[params.length];
            for (int i = 0; i < params.length; ++i) {
                int colon = params[i].indexOf(':');
                if (colon > 0) {
                    try {
                        int type = Integer.parseInt(params[i].substring(colon+1));
                        _params[i] = new Param(params[i].substring(0, colon), type);
                    } catch (NumberFormatException t) {
                        try {
                            int type = java.sql.Types.class.getField(params[i].substring(colon+1)).getInt(null);
                            _params[i] = new Param(params[i].substring(0, colon), type);
                        } catch (Exception x) {
                            throw new IllegalArgumentException("Parameter specification", x);
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Parameter specification: missing type in " + params[i]);
                }
            }
        } else {
            _params = NoParams;
        }
    }

    public ParametricStatement() {
        _params = null;
    }

    public ParametricStatement set(String sql) throws IllegalArgumentException {
    if (_params != null) {
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
    } else {
        List<Param> params = new ArrayList<Param>();
        Matcher matcher = PARAM_SYNTAX.matcher(sql);
        while (matcher.find()) {
            //System.err.println("[" + matcher.start() + ", " + matcher.end() + "] " + matcher.group());
            try {
                params.add(new Param(matcher.group(1), java.sql.Types.class.getField(matcher.group(2)).getInt(null)));
            } catch (Exception x) {
                throw new IllegalArgumentException("Parameter specification", x);
            }
        }
        _params = params.toArray(new Param[params.size()]);
        _sql = matcher.replaceAll("?");
    }
        return this;
    }

    protected <T extends PreparedStatement> T load(T statement, DataObject object) throws SQLException {
//System.err.println("PreparedStatement: loading " + _sql);
        if (object != null && _params.length > 0) {
            Class<? extends DataObject> type = object.getClass();

            for (int i = 0; i < _params.length; ++i) {
                try {
                    Field field = Beans.getKnownField(type, _params[i].name);
                    // NOTE: Class.isEnum() fails to return true if the field type is declared with a template parameter
                    if (Enum.class.isAssignableFrom(field.getType())) { // store as string or integer
                        if (Types.CHAR == _params[i].type || Types.VARCHAR == _params[i].type) {
                            statement.setObject(i+1, field.get(object).toString(), _params[i].type);
                        } else {
                            statement.setObject(i+1, ((Enum<?>)field.get(object)).ordinal(), _params[i].type);
                        }
                    } else if (Calendar.class.isAssignableFrom(field.getType())) {
                        statement.setObject(i+1, new java.sql.Date(((Calendar)field.get(object)).getTime().getTime()), _params[i].type);
                    } else {
                        statement.setObject(i+1, field.get(object), _params[i].type);
                    }
                } catch (NoSuchFieldException x) {
                    if (_params[i].nullable) {
                        statement.setNull(i+1, _params[i].type);
                    } else {
                        statement.close();
                        throw new SQLException("Failed to retrieve non-nullable '" + _params[i].name + "' from DataObject (" + type.getName() + ')', x);
                    }
                } catch (Exception x) {
                    statement.close();
                    throw new SQLException("Exception in retrieval of '" + _params[i].name + "' from DataObject (" + type.getName() + ')', x);
                }
            }
        }
        return statement;
    }

    /**
     * Executes an UPDATE or DELETE statement.
     *
     * @returns the number of rows affected
     */
    public int executeUpdate(Connection conn, DataObject object) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(_sql);
        try {
            load(statement, object);
            return statement.executeUpdate();
        } finally {
            statement.close();
        }
    }

    /**
     * Executes a batch UPDATE or DELETE statement.
     *
     * @returns the number of rows affected
     */
    public int executeUpdate(Connection conn, DataObject[] objects) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(_sql);
        try {
            for (DataObject object: objects) {
                load(statement, object);
                statement.addBatch();
            }
            int count = getAffectedRowCount(statement.executeBatch());
            return count;
        } finally {
            statement.close();
        }
    }

    /**
     * Executes a batch UPDATE or DELETE statement.
     *
     * @returns the number of rows affected
     */
    public int executeUpdate(Connection conn, Collection<? extends DataObject> objects) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(_sql);
        try {
            for (DataObject object: objects) {
                load(statement, object);
                statement.addBatch();
            }
            int count = getAffectedRowCount(statement.executeBatch());
            return count;
        } finally {
            statement.close();
        }
    }

    /**
     * Executes an INSERT statement.
     *
     * @returns an array whose length indicates the number of rows inserted. If generatedKeys is true, the array
     *          contains the keys; otherwise the content of the array is not defined.
     */
    public long[] executeInsert(Connection conn, DataObject object, boolean generatedKeys) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(_sql, generatedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
        try {
            load(statement, object);
            long[] keys = new long[statement.executeUpdate()];
            if (generatedKeys) {
                ResultSet rs = statement.getGeneratedKeys();
                for (int i = 0; rs.next(); ++i) {
                    keys[i] = rs.getLong(1);
                }
                rs.close();
            }
            return keys;
        } finally {
            statement.close();
        }
    }

    /**
     * Executes a batch INSERT statement.
     *
     * @returns the number of rows inserted.
     */
    public int executeInsert(Connection conn, DataObject[] objects) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(_sql);
        try {
            for (DataObject object: objects) {
                load(statement, object);
                statement.addBatch();
            }
            return getAffectedRowCount(statement.executeBatch());
        } finally {
            statement.close();
        }
    }

    /**
     * Executes a callable statement.
     *
     * @returns the number of rows affected
     */
    public int executeProcedure(Connection conn, DataObject object) throws SQLException {
        CallableStatement statement = conn.prepareCall(_sql);
        try {
            load(statement, object);
            return statement.executeUpdate();
        } finally {
            statement.close();
        }
    }

    public StringBuilder print(StringBuilder sb) {
        sb.append('[');
        for (Param param: _params) {
            sb.append('<').append(param.name).append('>');
        }
        sb.append(']').append(_sql);
        return sb;
    }

    private static final Param[] NoParams = new Param[0];
    private static final Pattern PARAM_SYNTAX = Pattern.compile(":(\\w+\\??):(\\w+)");
    private /*final*/ Param[] _params;
    protected String _sql;

    private static int getAffectedRowCount(int[] results) {
        int count = 0;
        for (int affected: results) {
            switch (affected) {
            case Statement.SUCCESS_NO_INFO:
                count++;
                break;
            case Statement.EXECUTE_FAILED:
                break;
            default:
                count += affected;
                break;
            }
        }
        return count;
    }
}
