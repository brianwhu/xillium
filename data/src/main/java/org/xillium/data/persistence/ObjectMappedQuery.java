package org.xillium.data.persistence;

import org.xillium.base.beans.Beans;
import org.xillium.data.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.lang.reflect.Field;


/**
 * A prepared SQL statement with named parameters.
 */
public class ObjectMappedQuery<T extends DataObject> extends ParametricQuery {
    private static class Column2Field {
        Field field;
        int index;

        Column2Field(int index, Field field) {
            this.index = index;
            this.field = field;
        }
    }

	private class ResultSetMapper implements ParametricQuery.ResultSetWorker<List<T>> {
        private volatile List<Column2Field> _list; // lazily initialized with double checked locking

		public List<T> process(ResultSet rs) throws SQLException, InstantiationException, IllegalAccessException {
			try {
                if (_list == null) {
                    synchronized(this) {
                        if (_list == null) {
                            List<Column2Field> list = new ArrayList<Column2Field>();
                            ResultSetMetaData meta = rs.getMetaData();
                            for (int i = 1, ii = meta.getColumnCount()+1; i < ii; ++i) {
                                try {
                                    String name = Beans.toLowerCamelCase(meta.getColumnName(i), '_');
                                    list.add(new Column2Field(i, _type.getField(name)));
                                } catch (NoSuchFieldException x) {
                                    // ignored
                                }
                            }
                            _list = list;
                        }
                    }
				}

				List<T> rows = new ArrayList<T>();
				while (rs.next()) {
					T object = _type.newInstance();
					for (Column2Field c2f: _list) {
						c2f.field.setAccessible(true);
                        Object value = rs.getObject(c2f.index);
                        try {
                            c2f.field.set(object, value);
                        } catch (IllegalArgumentException x) {
                            // Oracle && BigDecimal/Timestamp?
                            if (value instanceof BigDecimal) {
                                try {
                                    Class ftype = c2f.field.getType();
                                    if (Double.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, ((BigDecimal)value).doubleValue());
                                    } else if (Float.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, ((BigDecimal)value).floatValue());
                                    } else if (Long.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, ((BigDecimal)value).longValue());
                                    } else if (Integer.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, ((BigDecimal)value).intValue());
                                    } else {
                                        c2f.field.set(object, ((BigDecimal)value).shortValue());
                                    }
                                } catch (Throwable t) {
                                    throw x;
                                }
                            } else if (value instanceof java.sql.Timestamp) {
                                try {
                                    c2f.field.set(object, new java.sql.Date(((java.sql.Timestamp)value).getTime()));
                                } catch (Throwable t) {
                                    throw x;
                                }
                            } else {
                                throw x;
                            }
                        }
					}
					rows.add(object);
				}
				return rows;
			} finally {
				rs.close();
			}
		}
	}

	private final Class<T> _type;
	private final ResultSetMapper _mapper = new ResultSetMapper();

    public ObjectMappedQuery(Param[] parameters, String sql, Class<T> type) throws IllegalArgumentException {
		super(parameters, sql);
		_type = type;
    }

    public ObjectMappedQuery(String parameters,  String classname) throws IllegalArgumentException {
        super(parameters);
		try {
            _type = (Class<T>)Class.forName(classname);
        } catch (ClassNotFoundException x) {
            throw new IllegalArgumentException(x);
        }
    }

    public List<T> getResults(Connection conn, DataObject object) throws Exception {
		return super.executeSelect(conn, object, _mapper);
    }
}
