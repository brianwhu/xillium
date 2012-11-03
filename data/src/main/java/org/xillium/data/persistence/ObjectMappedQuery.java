package org.xillium.data.persistence;

import org.xillium.base.beans.Beans;
import org.xillium.data.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.lang.reflect.Field;


/**
 * Retrieving Java objects from result sets.
 *
 * Enums could be stored as String values. In such case this class converts String values into Java enums.
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

	private class ResultSetMapper implements ParametricQuery.ResultSetWorker<Collector<T>> {
        private final Collector<T> _collector;

        public ResultSetMapper(Collector<T> c) {
            _collector = c;
        }

		public Collector<T> process(ResultSet rs) throws SQLException, InstantiationException, IllegalAccessException {
			try {
                if (_c2fs == null) {
                    synchronized(ObjectMappedQuery.this) {
                        if (_c2fs == null) {
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
                            _c2fs = list;
                        }
                    }
				}

				//List<T> rows = new ArrayList<T>();
				while (rs.next()) {
					T object = _type.newInstance();
					for (Column2Field c2f: _c2fs) {
						c2f.field.setAccessible(true);
                        Object value = rs.getObject(c2f.index);
                        if (value == null) {
                            if (c2f.field.getType().isAssignableFrom(Number.class)) {
                                value = BigDecimal.ZERO;
                            } else continue;
                        }
                        try {
                            c2f.field.set(object, value);
                        } catch (IllegalArgumentException x) {
                            Class ftype = c2f.field.getType();
                            if (value instanceof Number) {
                                // size of "value" bigger than that of "field"?
                                try {
									Number number = (Number)value;
                                    if (Double.TYPE == ftype || Double.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, number.doubleValue());
                                    } else if (Float.TYPE == ftype || Float.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, number.floatValue());
                                    } else if (Long.TYPE == ftype || Long.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, number.longValue());
                                    } else if (Integer.TYPE == ftype || Integer.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, number.intValue());
                                    } else if (Short.TYPE == ftype || Short.class.isAssignableFrom(ftype)) {
                                        c2f.field.set(object, number.shortValue());
                                    } else {
                                        c2f.field.set(object, number.byteValue());
                                    }
                                } catch (Throwable t) {
                                    throw new IllegalArgumentException(t);
                                }
                            } else if (value instanceof java.sql.Timestamp) {
                                try {
                                    c2f.field.set(object, new java.sql.Date(((java.sql.Timestamp)value).getTime()));
                                } catch (Throwable t) {
                                    throw new IllegalArgumentException(t);
                                }
                            } else if ((value instanceof String) && Enum.class.isAssignableFrom(ftype)) {
                                try {
                                    c2f.field.set(object, Enum.valueOf(ftype, (String)value));
                                } catch (Throwable t) {
                                    throw new IllegalArgumentException(t);
                                }
                            } else {
                                throw new IllegalArgumentException(x);
                            }
                        }
					}
					//rows.add(object);
                    _collector.add(object);
				}
				//return rows;
                return _collector;
			} finally {
				rs.close();
			}
		}
	}

    private static class ListCollector<T> extends ArrayList<T> implements Collector<T> {
    }

    private static class SingleObjectCollector<T> implements Collector<T> {
        public T value;
        public boolean add(T object) {
            value = object;
            return true;
        }
    }

	private final Class<T> _type;
	//private final ResultSetMapper _lister = new ResultSetMapper(new ListCollector<T>());
    private volatile List<Column2Field> _c2fs; // lazily initialized with double checked locking

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

    public ObjectMappedQuery(String classname) throws IllegalArgumentException {
		try {
            _type = (Class<T>)Class.forName(classname);
        } catch (ClassNotFoundException x) {
            throw new IllegalArgumentException(x);
        }
    }

	/**
	 * Execute the query and returns the results as a list of objects.
	 */
    public List<T> getResults(Connection conn, DataObject object) throws Exception {
		return (List<T>)super.executeSelect(conn, object, new ResultSetMapper(new ListCollector<T>()));
    }

	/**
	 * Execute the query and returns the results as a list of objects.
	 */
    public T getObject(Connection conn, DataObject object) throws Exception {
		return ((SingleObjectCollector<T>)super.executeSelect(conn, object, new ResultSetMapper(new SingleObjectCollector<T>()))).value;
    }

	/**
	 * Execute the query and passes the results to a Collector.
	 */
    public Collector<T> getResults(Connection conn, DataObject object, Collector<T> collector) throws Exception {
		return super.executeSelect(conn, object, new ResultSetMapper(collector));
    }
}
