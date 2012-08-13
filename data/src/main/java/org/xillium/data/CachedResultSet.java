package org.xillium.data;

import java.sql.*;
import java.util.*;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.JSONBuilder;
import org.xillium.data.persistence.*;


/**
 * A flattened copy of a result set
 */
public class CachedResultSet {
    public static final Builder BUILDER = new Builder();
    public final String[] columns;
    public final List<Object[]> rows;

    public static class Builder implements ParametricQuery.ResultSetWorker<CachedResultSet> {
        public CachedResultSet process(ResultSet rs) throws SQLException {
            return new CachedResultSet(rs);
        }
    }

    // for JAXB
    private CachedResultSet() {
        columns = null;
        rows = null;
    }

    /**
     * Retrieves the rows from a freshly obtained JDBC result set into a CachedResultSet.
     *
     * Closes the JDBC result set after retrieval.
     */
    public CachedResultSet(ResultSet rset) throws SQLException {
        try {
            ResultSetMetaData metaData = rset.getMetaData();
            int width = metaData.getColumnCount();

            this.columns = new String[width];
            for (int i = 0; i < width; ++i) {
                columns[i] = Beans.toLowerCamelCase(metaData.getColumnName(i+1), '_');
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
     * Explicitly creates a CachedResultSet.
     */
    public CachedResultSet(String[] columns, List<Object[]> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    /**
     * Inside an object: serialized the cached result set into JSON.
     */
    public JSONBuilder toJSON(JSONBuilder jb) {
        return jb.append('{').serialize("columns", columns).append(',').serialize("rows", rows).append('}');
    }
}
