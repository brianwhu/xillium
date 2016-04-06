package org.xillium.data.persistence;

import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
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

    private class  ResultSetMapper<C extends Collector<T>> implements ResultSetWorker<C> {
        private final C _collector;

        public ResultSetMapper(C collector) {
            _collector = collector;
        }

        @SuppressWarnings("unchecked")
        public C process(ResultSet rs) throws SQLException, InstantiationException, IllegalAccessException {
            try {
                if (_c2fs == null) {
                    synchronized(ObjectMappedQuery.this) {
                        if (_c2fs == null) {
                            List<Column2Field> list = new ArrayList<Column2Field>();
                            ResultSetMetaData meta = rs.getMetaData();
                            for (int i = 1, ii = meta.getColumnCount()+1; i < ii; ++i) {
                                try {
                                    String name = Strings.toLowerCamelCase(meta.getColumnLabel(i), '_');
                                    list.add(new Column2Field(i, Beans.getKnownField(_type, name)));
                                } catch (NoSuchFieldException x) {
                                    // ignored
                                }
                            }
                            _c2fs = list;
                        }
                    }
                }

                while (rs.next()) {
                    T object = _type.newInstance();
                    for (Column2Field c2f: _c2fs) {
                        Beans.setValue(object, c2f.field, rs.getObject(c2f.index));
                    }
                    if (!_collector.add(object)) break;
                }
                return _collector;
            } finally {
                rs.close();
            }
        }
    }

    private static class SingleObjectCollector<T> implements Collector<T> {
        public T value;
        public boolean add(T object) {
            value = object;
            return true;
        }
    }

    private final Class<T> _type;
    private volatile List<Column2Field> _c2fs; // lazily initialized with double checked locking

    public ObjectMappedQuery(Param[] parameters, String sql, Class<T> type) throws IllegalArgumentException {
        super(parameters, sql);
        _type = type;
    }

    @SuppressWarnings("unchecked")
    public ObjectMappedQuery(String parameters,  String classname) throws IllegalArgumentException {
        super(parameters);
        try {
            _type = (Class<T>)Class.forName(classname, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException x) {
            throw new IllegalArgumentException(x);
        }
    }

    /**
     * Constructs an ObjectMappedQuery that retrieves objects of a given type.
     *
     * @param classname the name of the object class
     */
    @SuppressWarnings("unchecked")
    public ObjectMappedQuery(String classname) throws IllegalArgumentException {
        try {
            _type = (Class<T>)Class.forName(classname, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException x) {
            throw new IllegalArgumentException(x);
        }
    }

    /**
     * Constructs an ObjectMappedQuery that retrieves objects of a given type.
     *
     * @param type the object class
     */
    public ObjectMappedQuery(Class<T> type) {
        _type = type;
    }

    /**
     * Executes the query and returns the results as a list of objects.
     */
    public List<T> getResults(Connection conn, DataObject object) throws Exception {
        return executeSelect(conn, object, new ResultSetMapper<ArrayListCollector<T>>(new ArrayListCollector<T>()));
    }

    /**
     * Executes the query and returns the first row of the results as a single object.
     */
    public T getObject(Connection conn, DataObject object) throws Exception {
        return executeSelect(conn, object, new ResultSetMapper<SingleObjectCollector<T>>(new SingleObjectCollector<T>())).value;
    }

    /**
     * Executes the query and passes the results to a Collector.
     */
    public Collector<T> getResults(Connection conn, DataObject object, Collector<T> collector) throws Exception {
        return executeSelect(conn, object, new ResultSetMapper<Collector<T>>(collector));
    }
}
