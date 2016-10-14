package org.xillium.data.persistence;

import org.xillium.base.Trifunctor;
import org.xillium.base.text.GuidedTransformer;
import org.xillium.base.beans.Beans;
import org.xillium.data.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.MemberValue;


/**
 * A ParametricStatement is a prepared SQL statement supporting named bind parameters.
 *
 * TODO: remove nullable setting and checking, and let the database check column nullability
 */
public class ParametricStatement {
    public static class Param {
        public static final int IN = 0x0001;
        public static final int OUT = 0x0002;

        public final boolean nullable;
        public final String name;
        public final int type;
        public final int direction;

        /**
         * Constructs a named formal parameter with name <code>n</code> and type <code>t</code>.
         *
         * <p>A single-character prefix of either '-' or '+' before the name marks the parameter as either an OUT
         * or IN/OUT parameter, respectively.  Without any prefix, a parameter is regarded as an IN parameter.</p>
         *
         * <p>A single-character suffix of '?' after the name marks the parameter as accepting null values. Without
         * the suffix the parameter is non-null.</p>
         *
         * <p>Neither the prefix nor the suffix is part of the actual parameter name.</p>
         */
        public Param(String n, int t) {
            if (n.charAt(0) == '-') {
                n = n.substring(1);
                direction = OUT;
            } else if (n.charAt(0) == '+') {
                n = n.substring(1);
                direction = IN | OUT;
            } else {
                direction = IN;
            }
            if (n.endsWith("?")) {
                nullable = true;
                name = n.substring(0, n.length()-1);
            } else {
                nullable = false;
                name = n;
            }
            type = t;
        }

        public String toString() {
            return name+",nullable:"+nullable+",type:"+type+",dir:"+direction;
        }
    };

    /**
     * Constructs a ParametricStatement with the given list of parameters and the SQL string. The SQL string
     * may contain bind variable placeholders('?').
     */
    public ParametricStatement(Param[] parameters, String sql) throws IllegalArgumentException {
        _params = parameters;
        set(sql);
    }

    /**
     * Constructs a ParametricStatement with the given the SQL string. The SQL string may contain named bind
     * variables in the format
     * <blockquote>
     *      ':' variable-name ':' jdbc-type-name
     * </blockquote>
     */
    public ParametricStatement(String parameters) throws IllegalArgumentException {
        if (parameters != null && parameters.trim().length() > 0) {
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
                    throw new IllegalArgumentException("Parameter specification: missing type in " + params[i] + ", parameters = " + parameters);
                }
            }
        } else {
            _params = NoParams;
        }
    }

    /**
     * Constructs an empty ParametricStatement.
     */
    public ParametricStatement() {
    }

    /**
     * Assigns a SQL string to this ParametricStatement.
     */
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
        _sql = transformer.invoke(new StringBuilder(), params, sql).toString();
        _params = params.toArray(new Param[params.size()]);
    }
        return this;
    }

    /**
     * Returns the list of parameters of this ParametricStatement.
     */
    public Param[] getParameters() {
        return _params;
    }

    /**
     * Returns the SQL string in this ParametricStatement.
     */
    public String getSQL() {
        return _sql;
    }

    /**
     * Returns a DataObject class appropriate for this statement. If this statement defines no in/out parameters, this method returns DataObject.Empty.class.
     *
     * @param namespace - a namespace to be used as part of the class name. It must be a legal Java package name.
     * @return a DataObject class representing the calling interface of this ParametricStatement
     */
    @SuppressWarnings("unchecked")
    public Class<? extends DataObject> getDataObjectClass(String namespace) throws Exception {
        Class<? extends DataObject> c = DataObject.Empty.class;

        String cname = getClass().getName().replace("persistence", "p.d." + namespace.toLowerCase()) + Integer.toHexString(System.identityHashCode(_sql));
        try {
            c = (Class<? extends DataObject>)Class.forName(cname);
            if (!DataObject.class.isAssignableFrom(c)) {
                throw new ClassCastException(cname + " is not an implementation of DataObject");
            }
        } catch (ClassNotFoundException x) {
            if (_params != null && _params.length > 0) {
                Map<String, ParametricStatement.Param> params = new HashMap<String, ParametricStatement.Param>();
                for (ParametricStatement.Param param: _params) {
                    params.put(param.name, param);
                }

                ClassPool pool = ClassPool.getDefault();
                // this line is necessary for web applications (web container class loader in play)
                pool.appendClassPath(new LoaderClassPath(org.xillium.data.DataObject.class.getClassLoader()));

                CtClass cc = pool.makeClass(cname);
                cc.addInterface(pool.getCtClass("org.xillium.data.DataObject"));
                ConstPool cp = cc.getClassFile().getConstPool();

                for (ParametricStatement.Param param: params.values()) {
                    CtField field = new CtField(pool.getCtClass(sqlTypeName(param.type)), param.name, cc);
                    field.setModifiers(java.lang.reflect.Modifier.PUBLIC);
                    if ((param.direction & ParametricStatement.Param.IN) != 0 && !param.nullable) {
                        AnnotationsAttribute attr = new AnnotationsAttribute(cp, AnnotationsAttribute.visibleTag);
                        //addAnnotation(attr, cp, "org.xillium.data.validation.required");
                        attr.addAnnotation(new Annotation("org.xillium.data.validation.required", cp));
                        field.getFieldInfo().addAttribute(attr);
                    }
                    cc.addField(field);
                }

                c = cc.toClass(DataObject.class.getClassLoader(), DataObject.class.getProtectionDomain());
            }
        }

        return c;
    }

    protected <T extends PreparedStatement> T load(T statement, DataObject object) throws SQLException {
//System.err.println("PreparedStatement: loading " + _sql);
        if (object != null && _params.length > 0) {
            Class<? extends DataObject> type = object.getClass();

            for (int i = 0; i < _params.length; ++i) {
                if ((_params[i].direction & Param.IN) == 0) continue;
                try {
                    Field field = Beans.getKnownField(type, _params[i].name);
                    Object value = field.get(object);
                    if (value != null) {
                        // NOTE: Class.isEnum() fails to return true if the field type is declared with a template parameter
                        if (Enum.class.isAssignableFrom(field.getType())) { // store as string or integer
                            if (Types.CHAR == _params[i].type || Types.VARCHAR == _params[i].type) {
                                statement.setObject(i+1, value.toString(), _params[i].type);
                            } else {
                                statement.setObject(i+1, ((Enum<?>)value).ordinal(), _params[i].type);
                            }
                        } else if (Calendar.class.isAssignableFrom(field.getType())) {
                            statement.setObject(i+1, new java.sql.Date(((Calendar)value).getTime().getTime()), _params[i].type);
                        } else {
                            statement.setObject(i+1, value, _params[i].type);
                        }
                    } else {
                        //throw new NoSuchFieldException(_params[i].name + ": null");
                        statement.setNull(i+1, _params[i].type);
                    }
                } catch (NoSuchFieldException x) {
                    //if (_params[i].nullable) {
                    // LET database check the nullability of this column
                        statement.setNull(i+1, _params[i].type);
                    //} else {
                        //statement.close();
                        //throw new SQLException("Failed to retrieve non-nullable '" + _params[i].name + "' from DataObject (" + type.getName() + ')', x);
                    //}
                } catch (Exception x) {
                    statement.close();
                    throw new SQLException("Exception in retrieval of '" + _params[i].name + "' from " + type.getName() + ": " + x.getMessage(), x);
                }
            }
        }
        return statement;
    }

    /**
     * Executes an UPDATE or DELETE statement.
     *
     * @return the number of rows affected
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
     * @return the number of rows affected
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
     * @return the number of rows affected
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
     * @return an array whose length indicates the number of rows inserted. If generatedKeys is true, the array
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
     * Executes an INSERT statement without requesting for generated keys. This method is more lightweight than
     * {@link #executeInsert(Connection, DataObject, boolean)} as it does not create an array for the generated keys.
     *
     * @return the number of rows inserted.
     */
    public int executeInsert(Connection conn, DataObject object) throws SQLException {
        PreparedStatement statement = conn.prepareStatement(_sql, Statement.NO_GENERATED_KEYS);
        try {
            load(statement, object);
            return statement.executeUpdate();
        } finally {
            statement.close();
        }
    }

    /**
     * Executes a batch INSERT statement.
     *
     * @return the number of rows inserted.
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
     * Executes a batch INSERT statement.
     *
     * @return the number of rows inserted.
     */
    public int executeInsert(Connection conn, Collection<? extends DataObject> objects) throws SQLException {
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
     * Executes a callable statement that performs updates.
     *
     * @return the number of rows affected
     */
    public int executeProcedure(Connection conn, DataObject object) throws SQLException {
        CallableStatement statement = conn.prepareCall(_sql);
        try {
            for (int i = 0; i < _params.length; ++i) {
                if ((_params[i].direction & Param.OUT) == 0) continue;
                statement.registerOutParameter(i+1, _params[i].type);
            }
            load(statement, object);
            statement.execute();

            int result = statement.getUpdateCount();

            if (object != null) {
                Class<? extends DataObject> type = object.getClass();
                for (int i = 0; i < _params.length; ++i) {
                    if ((_params[i].direction & Param.OUT) == 0) continue;
                    try {
                        Beans.setValue(object, Beans.getKnownField(type, _params[i].name), statement.getObject(i+1));
                    } catch (NoSuchFieldException x) {
                        // ignore
                    } catch (Exception x) {
                        throw new SQLException("Exception in storage of '" + _params[i].name + "' into DataObject (" + type.getName() + ')', x);
                    }
                }
            }
            return result;
        } finally {
            statement.close();
        }
    }

    public void setTag(String t) {
        _tag = t;
    }

    public String getTag() {
        return _tag;
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
    private static final Pattern PARAM_SYNTAX = Pattern.compile(":([-+]?\\p{Alpha}\\w*\\??):(\\p{Alpha}\\w*)");
    private static final Pattern QUOTE_SYNTAX = Pattern.compile("'([^']*)'");
    private static final Pattern COMM1_SYNTAX = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final GuidedTransformer<List<Param>> transformer = new GuidedTransformer<List<Param>>(QUOTE_SYNTAX,
        GuidedTransformer.Action.COPY,
        new GuidedTransformer<List<Param>>(COMM1_SYNTAX,
            GuidedTransformer.Action.COPY,
            new GuidedTransformer<List<Param>>(PARAM_SYNTAX,
                new Trifunctor<StringBuilder, StringBuilder, List<Param>, Matcher>() {
                    public StringBuilder invoke(StringBuilder sb, List<Param> params, Matcher matcher) {
                        try {
                            params.add(new Param(matcher.group(1), java.sql.Types.class.getField(matcher.group(2)).getInt(null)));
                        } catch (Exception x) {
                            throw new IllegalArgumentException("Parameter specification :" + matcher.group(1) + ':' + matcher.group(2), x);
                        }
                        return sb.append("?");
                    }
                },
                GuidedTransformer.Action.COPY
            )
        )
    );

    private /*final*/ Param[] _params;
    protected String _sql;
    protected String _tag;

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

    private static String sqlTypeName(int type) {
        switch (type) {
        case Types.NUMERIC:
            return "java.math.BigDecimal";
        case Types.INTEGER:
            return "java.lang.Integer";
        case Types.TINYINT:
            return "java.lang.Byte";
        case Types.CHAR:
        case Types.VARCHAR:
            return "java.lang.String";
        case Types.DATE:
            return "java.sql.Date";
        case Types.TIMESTAMP:
            return "java.sql.Timestamp";
        default:
            return "java.lang.Object";
        }
    }
}
