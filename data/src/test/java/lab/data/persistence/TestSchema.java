package lab.data.persistence;

import java.util.*;
import java.util.logging.*;
import java.sql.*;
import javax.sql.DataSource;
import javax.annotation.Resource;

//import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.*;

import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;


/**
 * Testing Persistence
 */
@ContextConfiguration(locations={"/application-context.xml"})
//@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
@Transactional
public class TestSchema extends AbstractTransactionalTestNGSpringContextTests {
	private static final int COLUMN_NAME = 4;
	private static final int COLUMN_TYPE = 5;	// java.sql.Types.#
	private static final int COLUMN_SIZE = 7;
	private static final int PKEY_SEQ = 5;
	private static final int PKEY_NAME = 6;

    private Logger _logger = Logger.getLogger(TestSchema.class.getName());

    @Resource
    private DataSource dataSource;

    //private PersistenceStrategies strategies;

    public TestSchema() throws Exception {
/*
        strategies = (PersistenceStrategies)new XMLBeanAssembler(new DefaultObjectFactory()).build(
            TestSchema.class.getResourceAsStream("/persistence-strategies.xml")
        );
        _logger.info(Beans.toString(strategies));
*/
    }


    @Test(groups={ "basic" })
    public void testAccess() {
        _logger.info("DataSource = " + dataSource);
    }

    @Test(groups={ "basic" })
    public void testStrategies() {
        //_logger.info("PersistenceStrategies = " + strategies);
    }

    @Test(groups={ "schema" })
	public void testSchema() throws Exception {
        StringBuilder sb = new StringBuilder();

        Connection connection = dataSource.getConnection();

		try {
			DatabaseMetaData meta = connection.getMetaData();
			String catalog = connection.getCatalog();
            String schema = meta.getUserName();

            ResultSet tables = meta.getTables(catalog, schema, "%", null);
            while (tables.next()) {
                if (!tables.getString(4).equals("TABLE")) {
                    continue;
                }
                String table = tables.getString(3);

                sb.append(String.format("\n\t@@@%24s:", table)).append('\n');
                ResultSet columns = meta.getColumns(catalog, schema, table, "%");
                while (columns.next()) {
                    String nullable = columns.getInt(11) == 0 ? "NOT NULL" : "";
                    String defvalue = columns.getString(13);
                    if (defvalue == null) {
                        defvalue = "";
                    }
                    sb.append(String.format("%32s: %4s %12s(%s) %s%s",
                        columns.getString(COLUMN_NAME), columns.getString(COLUMN_TYPE), columns.getString(6), columns.getInt(COLUMN_SIZE), defvalue, nullable
                    )).append('\n');
/*
                    Property property = new Property();
                    property.name = columns.getString(4);
                    property.type = columns.getString(6);
                    property.tid = Integer.parseInt(columns.getString(5));
                    property.defValue = defvalue;
                    property.nullable = !nullable.startsWith("NOT");
    //model.properties.put(property.name, property);
*/
                }

                sb.append(String.format("%24s:", "Primary Keys")).append('\n');
                ResultSet keys = meta.getPrimaryKeys(catalog, schema, table);
                while (keys.next()) {
                    sb.append(String.format("%32s: %4s (%s)", keys.getString(COLUMN_NAME), keys.getString(PKEY_SEQ), keys.getString(PKEY_NAME))).append('\n');
                }

                sb.append(String.format("%24s:", "Referencing")).append('\n');
                keys = meta.getImportedKeys(catalog, schema, table);
                while (keys.next()) {
                    sb.append(String.format("%32s: %4s (%s %s [%s])", keys.getString(3), keys.getString(4),
                        keys.getString(7), keys.getString(8), keys.getString(9))).append('\n');
                }

                sb.append(String.format("%24s:", "Referenced Keys")).append('\n');
                keys = meta.getExportedKeys(catalog, schema, table);
                while (keys.next()) {
                    sb.append(String.format("%32s: %4s (%s)", keys.getString(7), keys.getString(8), keys.getString(9))).append('\n');
                }

                sb.append(String.format("%24s:", "Indices")).append('\n');
                ResultSet indices = meta.getIndexInfo(catalog, schema, table, false, true);
                while (indices.next()) {
                    int type = indices.getInt(7);
                    sb.append(String.format("%32s: %4s %12s(%s) %s", indices.getString(6), type, indexType(type), indices.getString(8), indices.getString(9))).append('\n');
                }
    /*
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String command = null;
                DataObject data = new DataObject(model);
                while ((command = reader.readLine()) != null) {
                }
    */
            }
        } finally {
            connection.close();
        }

        _logger.info(sb.toString());
	}

    @Test(groups={ "schema" })
	public void testStorageModel() throws Exception {
        //strategies.prepare(dataSource);
        //_logger.info(strategies.print(new StringBuilder()).toString());
    }

	private static String indexType(int type) {
		switch (type) {
		case DatabaseMetaData.tableIndexStatistic:
			return "STAT";
		case DatabaseMetaData.tableIndexClustered:
			return "CLUSTERED";
		case DatabaseMetaData.tableIndexHashed:
			return "HASHED";
		case DatabaseMetaData.tableIndexOther:
			return "OTHER";
		}
		return "UNKONWN";
	}

	public static class Property {
		String name;
		String type;
		int tid;
		Object defValue;
		int primary;	// order in primary key
		boolean nullable;
	}

	public static class Model {
		Map<String, Property> properties = new HashMap<String, Property>();
	}
}

