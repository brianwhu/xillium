package org.xillium.data;

import java.sql.*;
import java.util.*;
import org.xillium.base.beans.Beans;
//import org.xillium.data.validation.*;
import org.xillium.data.persistence.*;


/**
 * A flattened copy of a result set
 */
public class CachedResultSet {
    public final String name;
    public final String[] columns;
    public final List<Object[]> rows;

    public static class Builder implements ParametricQuery.ResultSetWorker<CachedResultSet> {
        public Builder(String name) {
            this.name = name;
        }

        public CachedResultSet process(ResultSet rs) throws SQLException {
            return new CachedResultSet(name, rs);
        }

        private final String name;
    }

    // for JAXB
    private CachedResultSet() {
        name = null;
        columns = null;
        rows = null;
    }

    /**
     * Retrieves the rows from a freshly obtained JDBC result set into a CachedResultSet.
     *
     * Closes the JDBC result set after retrieval.
     */
    public CachedResultSet(String name, ResultSet rset) throws SQLException {
        this.name = name;
        try {
            ResultSetMetaData metaData = rset.getMetaData();
            int width = metaData.getColumnCount();

            this.columns = new String[width];
            for (int i = 0; i < width; ++i) {
                columns[i] = Beans.toCamelCase(metaData.getColumnName(i+1).toLowerCase(), '_');
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
}
