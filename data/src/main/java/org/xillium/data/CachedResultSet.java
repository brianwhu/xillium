package org.xillium.data;

import java.sql.*;
import java.util.*;
import java.lang.reflect.Field;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
import org.xillium.base.beans.JSONBuilder;
import org.xillium.data.persistence.*;
import org.xillium.data.presentation.*;


/**
 * A cached result set that is detached from any database connectivity.
 */
public class CachedResultSet {
    public static final Builder BUILDER = new Builder();

    /**
     * The name of columns in the result set.
     */
    public final String[] columns;

    /**
     * The rows of the result set with fields matching the column definition.
     */
    public final List<Object[]> rows;

    /**
     * A ResultSetWorker implementation that builds a CachedResultSet.
     */
    public static class Builder implements ResultSetWorker<CachedResultSet> {
        public CachedResultSet process(ResultSet rs) throws SQLException {
            return new CachedResultSet(rs);
        }
    }

    // for JAXB, Jackson, and possibly other libraries that use reflection to instantiate objects
    private CachedResultSet() {
        columns = null;
        rows = null;
    }
    
    /**
     * Retrieves the rows from a freshly obtained JDBC result set into a CachedResultSet.
     * Closes the JDBC result set after retrieval.
     */
    public CachedResultSet(ResultSet rset) throws SQLException {
        try {
            ResultSetMetaData metaData = rset.getMetaData();
            int width = metaData.getColumnCount();

            this.columns = new String[width];
            for (int i = 0; i < width; ++i) {
                columns[i] = Strings.toLowerCamelCase(metaData.getColumnLabel(i+1), '_');
            }

            if (rset.next()) {
                this.rows = new ArrayList<Object[]>();
                do {
                    Object[] row = new Object[width];
                    for (int i = 0; i < width; ++i) {
                        row[i] = rset.getObject(i+1);
                    }
                    rows.add(row);
                } while (rset.next());
            } else {
                this.rows = null;
            }
        } finally {
            rset.close();
        }
    }

    /**
     * Retrieves the rows from a collection of Objects, where the objects' (of type T) <i>instance fields</i> are taken as result set columns.
     */
    public <T> CachedResultSet(Collection<T> collection) throws Exception {
        this(collection, false);
    }

    /**
     * Retrieves the rows from a collection of Objects, where the objects' (of type T) <i>instance fields</i> are taken as result set columns.
     * Also performs presentation transformations if any is defined on the fields.
     */
    public <T> CachedResultSet(Collection<T> collection, boolean forPresentation) throws Exception {
        FieldRetriever[] retrievers = null;

        this.rows = new ArrayList<Object[]>();
        for (T object: collection) {
            if (retrievers == null) {
                if (forPresentation) {
                    retrievers = FieldFormatter.getFieldRetriever(Beans.getKnownInstanceFields(object.getClass()));
                } else {
                    retrievers = FieldRetriever.getFieldRetriever(Beans.getKnownInstanceFields(object.getClass()));
                }
            }
            Object[] row = new Object[retrievers.length];
            for (int i = 0; i < retrievers.length; ++i) {
                row[i] = retrievers[i].get(object);
            }
            rows.add(row);
        }

        int width = retrievers != null ? retrievers.length : 0;
        this.columns = new String[width];
        for (int i = 0; i < width; ++i) {
            columns[i] = retrievers[i].field.getName();
        }
    }

    /**
     * Retrieves the rows from a collection of Objects, where the objects' (of type T) <i>bean properties</i> are taken as result set columns.
     */
    public <T> CachedResultSet(Collection<T> collection, String... columns) {
        this.columns = columns;
        this.rows = new ArrayList<Object[]>();
        for (T object: collection) {
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; ++i) {
                try {
                    Object value = columns[i].equals("-") ? object : Beans.invoke(object, "get"+Strings.capitalize(columns[i]));
                    if (value != null) {
                        if (value.getClass().isArray()) {
                            row[i] = org.xillium.base.etc.Arrays.join(value, ';');
                        } else {
                            row[i] = value.toString();
                        }
                    } else {
                        row[i] = null;
                    }
                } catch (Exception x) {
                    row[i] = null;
                }
            }
            rows.add(row);
        }
    }

    /**
     * Retrieves the rows from a collection of Objects, where a subset of the objects' (of type T) <i>public fields</i> are taken as result set columns.
     */
    public static <T> CachedResultSet chooseFields(Collection<T> collection, String... columns) {
        CachedResultSet rs = new CachedResultSet(columns, new ArrayList<Object[]>());
        for (T object: collection) {
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; ++i) {
                try {
                    Object value = Beans.getKnownField(object.getClass(), columns[i]).get(object);
                    if (value != null) {
                        if (value.getClass().isArray()) {
                            row[i] = org.xillium.base.etc.Arrays.join(value, ';');
                        } else {
                            row[i] = value.toString();
                        }
                    } else {
                        row[i] = null;
                    }
                } catch (Exception x) {
                    row[i] = null;
                }
            }
            rs.rows.add(row);
        }

        return rs;
    }

    /**
     * Retrieves the contents of this result set as a List of DataObjects.
     */
    public <T extends DataObject> List<T> asList(Class<T> type) throws InstantiationException, IllegalAccessException {
        Map<String, Field> fields = new HashMap<String, Field>();
        for (String column: columns) try { fields.put(column, Beans.getKnownField(type, column)); } catch (Exception x) {}

        List<T> list = new ArrayList<T>();
        for (Object[] row: rows) {
            T object = type.newInstance();
            for (int i = 0; i < row.length; ++i) {
                Field field = fields.get(columns[i]);
                if (field != null) {
                    Beans.setValue(object, field, row[i]);
                }
            }
            list.add(object);
        }

        return list;
    }

    /**
     * Explicitly creates a CachedResultSet.
     */
    public CachedResultSet(String[] columns, List<Object[]> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    /**
     * Renames the columns of a CachedResultSet.
     */
    public CachedResultSet rename(String... names) {
        for (int i = 0; i < names.length; ++i) {
            this.columns[i] = names[i];
        }
        return this;
    }

    /**
     * Builds a name-to-column index for quick access to data by columm names.
     */
    public Map<String, Integer> buildIndex() {
        Map<String, Integer> index = new HashMap<String, Integer>();
        for (int i = 0; i < columns.length; ++i) {
            index.put(columns[i], i);
        }
        return index;
    }

    /**
     * Inside an object: serialized the cached result set into JSON.
     */
    public JSONBuilder toJSON(JSONBuilder jb) {
        return jb.append('{').serialize("columns", columns).append(',').serialize("rows", rows).append('}');
    }
}
