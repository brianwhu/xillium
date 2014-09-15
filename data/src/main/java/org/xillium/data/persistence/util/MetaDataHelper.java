package org.xillium.data.persistence.util;

import java.util.*;
import java.sql.*;


/**
 * Helper methods to retrieve metadata information from a DatabaseMetaData.
 */
public class MetaDataHelper {
    public static class ForeignKey extends HashSet<String> {
        public final String tableName;

        public ForeignKey(String name) {
            tableName = name;
        }
    }

    public static class Column {
        public final String name;
        public final String java;
        public final int type;
        public final int precision;
        public final int scale;
        public final boolean nullable;
        //public final Object defv;

        public Column(ResultSetMetaData meta, int index) throws Exception {
            name = meta.getColumnName(index);
            java = getClassName(meta, index);
            type = meta.getColumnType(index);
            precision = meta.getPrecision(index);
            scale = meta.getScale(index);
            nullable = meta.isNullable(index) == ResultSetMetaData.columnNullable;
/*
            if (d.charAt(0) == '\'' && d.charAt(d.length()-1) == '\'') {
                defv = d.substring(1, d.length()-1);
            } else if (d.indexOf('.') > -1) {
                defv = 
            }
*/
        }
    }

    public static class Table {
        public final String name;
        public final Set<String> primaryKey;
        public final Map<String, ForeignKey> foreignKeys;
        public final List<Column> columns;

        public Table(String n, Set<String> p, Map<String, ForeignKey> f, List<Column> c) {
            name = n;
            primaryKey = p;
            foreignKeys = f;
            columns = c;
        }
    }

/*
    private static final int COLUMNS_TB_COL_NAME = 4;
    private static final int COLUMNS_TB_COL_TYPE = 5;   // java.sql.Types.#
    private static final int COLUMNS_TB_COL_CLASS = 6;
    private static final int COLUMNS_TB_COL_SIZE = 7;
    private static final int COLUMNS_TB_COL_SCALE = 9;
    private static final int COLUMNS_TB_COL_NULL = 11;
    private static final int COLUMNS_TB_COL_DEFAULT = 13;
*/

    private static final int PRIMARY_PK_COL_NAME = 4;

    private static final int IMPORTED_PK_TAB_NAME = 3;
    private static final int IMPORTED_PK_COL_NAME = 4;

    private static final int IMPORTED_FK_TAB_NAME = 7;
    private static final int IMPORTED_FK_COL_NAME = 8;
    private static final int IMPORTED_FK_KEY_NAME = 12;

    /**
     * Returns a table's primary key columns as a Set of strings.
     */
    public static Set<String> getPrimaryKey(DatabaseMetaData metadata, String tableName) throws Exception {
        Set<String> columns = new HashSet<String>();
        ResultSet keys = metadata.getPrimaryKeys(metadata.getConnection().getCatalog(), metadata.getUserName(), tableName);
        while (keys.next()) {
            columns.add(keys.getString(PRIMARY_PK_COL_NAME));
        }
        keys.close();
        return columns;
    }

    /**
     * Returns a table's foreign keys and their columns as a Map from the key name to the ForeignKey object.
     * <p/>
     * A foreign key may not have a name. On such a database, 2 foreign keys must reference 2 different tables. Otherwise
     * there's no way to tell them apart and the foreign key information reported by DatabaseMetaData becomes ill-formed.
     */
    public static Map<String, ForeignKey> getForeignKeys(DatabaseMetaData metadata, String tableName) throws Exception {
        ResultSet keys = metadata.getImportedKeys(metadata.getConnection().getCatalog(), metadata.getUserName(), tableName);
        Map<String, ForeignKey> map = new HashMap<String, ForeignKey>();

        while (keys.next()) {
            String table = keys.getString(IMPORTED_PK_TAB_NAME);
            String name = keys.getString(IMPORTED_FK_KEY_NAME);
            if (name == null || name.length() == 0) name = "UNNAMED_FK_" + table;

            ForeignKey key = map.get(name);
            if (key == null) {
                map.put(name, key = new ForeignKey(table));
            }
            key.add(keys.getString(IMPORTED_FK_COL_NAME));
        }
        keys.close();
        return map;
    }

    /**
     * Returns a table's columns
     */
    public static List<Column> getColumns(DatabaseMetaData metadata, String tableName) throws Exception {
        List<Column> columns = new ArrayList<Column>();

        PreparedStatement stmt = metadata.getConnection().prepareStatement("SELECT * FROM " + tableName);
        ResultSetMetaData rsmeta = stmt.getMetaData();
        for (int i = 1, ii = rsmeta.getColumnCount(); i <= ii; ++i) {
            columns.add(new Column(rsmeta, i));
        }
        stmt.close();
        return columns;
    }

    /**
     * Returns a table's structure
     */
    public static Table getTable(DatabaseMetaData metadata, String tableName) throws Exception {
        return new Table(tableName, getPrimaryKey(metadata, tableName), getForeignKeys(metadata, tableName), getColumns(metadata, tableName));
    }

    /**
     * Returns a column's java class name.
     */
    public static String getClassName(ResultSetMetaData meta, int index) throws SQLException {
        switch (meta.getColumnType(index)) {
        case Types.NUMERIC:
            int precision = meta.getPrecision(index);
            if (meta.getScale(index) == 0) {
                if (precision > 18) {
                    return "java.math.BigInteger";
                } else if (precision > 9) {
                    return "java.lang.Long";
                } else if (precision > 4) {
                    return "java.lang.Integer";
                } else if (precision > 2) {
                    return "java.lang.Short";
                } else {
                    return "java.lang.Byte";
                }
            } else {
                if (precision > 16) {
                    return "java.math.BigDecimal";
                } else if (precision > 7) {
                    return "java.lang.Double";
                } else {
                    return "java.lang.Float";
                }
            }
        case Types.TIMESTAMP:
            if (meta.getScale(index) == 0) {
                return "java.sql.Date";
            } else {
                return "java.sql.Timestamp";
            }
        default:
            return meta.getColumnClassName(index);
        }
    }
}

