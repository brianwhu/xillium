package org.xillium.tool.data;

import java.util.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;

//import org.xillium.base.util.Bytes;
import org.xillium.base.util.Options;
import org.xillium.base.util.Pair;
import org.xillium.base.beans.*;
//import org.xillium.data.CachedResultSet;
//import org.xillium.data.DataObject;
//import org.xillium.data.DataBinder;
//import org.xillium.data.validation.*;
//import org.xillium.data.persistence.*;
//import org.xillium.core.conf.StorageConfiguration;


public class info {
    //private static final int COLUMN_NAME = 4;
    //private static final int COLUMN_TYPE = 5;   // java.sql.Types.#
    //private static final int COLUMN_SIZE = 7;
    private static final int PKEY_SEQ = 5;
    private static final int PKEY_NAME = 6;

    private static interface Table {
        public static int CATALOG = 1;// String => table catalog (may be null)
        public static int SCHEMA  = 2;//String => table schema (may be null)
        public static int NAME    = 3;//String => table name
        public static int TYPE    = 4;  //String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
        public static int REMARKS = 5;      // String => explanatory comment on the table
        //public static int TYPE_CAT = 6;     // String => the types catalog (may be null)
        //public static int TYPE_SCHEMA = 7;  // String => the types schema (may be null)
        //public static int TYPE_NAME = 8;    // String => type name (may be null)
        //public static int ID_NAME = 9;  // String => name of the designated "identifier" column of a typed table (may be null)
        //public static int ID_GEN  = 10;  // String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)
    }

    private static interface Column {
        //public static int CATALOG  = 1; // String => table catalog (may be null)
        //public static int SCHEMA = 2;   // String => table schema (may be null)
        //public static int TABLE_NAME String => table name
        public static int NAME = 4;     // String => column name
        public static int DATA_TYPE = 5;// int => SQL type from java.sql.Types
        public static int TYPE_NAME = 6;// String => Data source dependent type name, for a UDT the type name is fully qualified
        public static int SIZE = 7;     // int => column size.
        //public static int BUFFER_LENGTH is not used.
        public static int DECIMAL_DIGITS = 9;   // int => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable.
        public static int NUM_PREC_RADIX =10;   // int => Radix (typically either 10 or 2)
        public static int NULLABLE = 11;        // int => is NULL allowed.
                                                // columnNoNulls - might not allow NULL values
                                                // columnNullable - definitely allows NULL values
                                                // columnNullableUnknown - nullability unknown
        public static int REMARKS = 12;         // String => comment describing column (may be null)
        public static int DEFAULT = 13;         // String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null)
        //public static int SQL_DATA_TYPE int => unused
        //public static int SQL_DATETIME_SUB int => unused
        public static int CHAR_OCTET_LENGTH = 16;   // int => for char types the maximum number of bytes in the column
        public static int ORDINAL_POSITION = 17;    // int => index of column in table (starting at 1)
        public static int IS_NULLABLE = 18;         // String => ISO rules are used to determine the nullability for a column.
                                                // YES --- if the column can include NULLs
                                                // NO --- if the column cannot include NULLs
                                                // empty string --- if the nullability for the column is unknown
        public static int SCOPE_CATALOG = 19;   // String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
        public static int SCOPE_SCHEMA = 20;    // String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
        public static int SCOPE_TABLE = 21;     // String => table name that this the scope of a reference attribute (null if the DATA_TYPE isn't REF)
        public static int SOURCE_DATA_TYPE = 22;// short => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF)
        public static int IS_AUTOINCREMENT = 23;// String => Indicates whether this column is auto incremented
                                                    // YES --- if the column is auto incremented
                                                    // NO --- if the column is not auto incremented
                                                    // empty string --- if it cannot be determined whether the column is auto incremented
        public static int IS_GENERATED = 24;    // String => Indicates whether this is a generated column
                                                    // YES --- if this a generated column
                                                    // NO --- if this not a generated column
                                                    // empty string --- if it cannot be determined whether this is a generated column

    }

    public static class Settings {
        public String schema;
    }

    public static void main(String[] args) throws Exception {
        Options<Settings> options = new Options<>(new Settings());
        List<Pair<Options.Unrecognized, String>> invalid = new ArrayList<>();

        int index = options.parse(args, 0, invalid);
        if (args.length - index == 0) {
            System.err.println("Usage: data.info [ options ] data-source.xml [ list of tables ... ]");
            options.document(System.err);
            System.exit(0);
        }

        Set<String> restriction = null;
        if (args.length > index) {
            restriction = new HashSet<>();
            for (int i = index; i < args.length; ++i) {
                //restriction.add(args[i].toUpperCase());
                restriction.add(args[i]);
            }
        }

        BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory();
        XMLBeanAssembler assembler = new XMLBeanAssembler(factory);
        DataSource dataSource = (DataSource)("-".equals(args[0]) ? assembler.build(System.in) : assembler.build(args[0]));

        System.err.println("connecting ...");
        StringBuilder sb = new StringBuilder();
        try (Connection connection = dataSource.getConnection()) {
            System.err.println("connected at " + new java.util.Date());

            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = meta.getUserName();
            sb.append("UserName: ").append(schema).append('\n');

            sb.append("      storesLowerCaseIdentifiers:").append(meta.storesLowerCaseIdentifiers()).append('\n');
            sb.append("storesLowerCaseQuotedIdentifiers:").append(meta.storesLowerCaseQuotedIdentifiers()).append('\n');
            sb.append("      storesMixedCaseIdentifiers:").append(meta.storesMixedCaseIdentifiers()).append('\n');
            sb.append("storesMixedCaseQuotedIdentifiers:").append(meta.storesMixedCaseQuotedIdentifiers()).append('\n');
            sb.append("      storesUpperCaseIdentifiers:").append(meta.storesUpperCaseIdentifiers()).append('\n');
            sb.append("storesUpperCaseQuotedIdentifiers:").append(meta.storesUpperCaseQuotedIdentifiers()).append('\n');

            sb.append("Schemas visible to current user:\n");
            ResultSet schemas = meta.getSchemas();
            while (schemas.next()) {
                sb.append('\t').append(schemas.getString(1)).append('\n');
            }
            sb.append('\n');

            ResultSet tables = meta.getTables(catalog, schema, "%", null);
            while (tables.next()) {
                if (!tables.getString(Table.TYPE).equals("TABLE")) {
                    continue;
                }
                String table = tables.getString(Table.NAME);
                if (restriction != null && !restriction.contains(table)) continue;

                //String id = tables.getString(Table.ID_NAME);
                //String gen = tables.getString(Table.ID_GEN);
                
                sb.append(String.format("\n\t@@@%24s:", table)).append('\n');
                ResultSet columns = meta.getColumns(catalog, schema, table, "%");
                System.err.println("columns count " + columns.getMetaData().getColumnCount());
                while (columns.next()) {
                    String nullable = columns.getInt(Column.NULLABLE) == DatabaseMetaData.columnNoNulls ? "NOT NULL" : "NULL";
                    String defvalue = columns.getString(Column.DEFAULT);
                    if (defvalue == null) {
                        defvalue = "";
                    }
                    String auto = get(columns, Column.IS_AUTOINCREMENT);
                    String generated = get(columns, Column.IS_GENERATED);
                    sb.append(String.format("%32s: %4s %12s(%2d) %16s %8s %8s %8s",
                        columns.getString(Column.NAME),
                        columns.getString(Column.DATA_TYPE),
                        columns.getString(Column.TYPE_NAME),
                        columns.getInt(Column.SIZE),
                        defvalue,
                        nullable,
                        auto,
                        generated
                    )).append('\n');
                }
            }
        } finally {
            System.err.println("disconnected");
            System.out.println(sb.toString());
        }
    }

    private static String get(ResultSet rs, int column) {
        try {
            return Strings.toString(rs.getString(column));
        } catch (Exception x) {
            return "N/A";
        }
    }
}
