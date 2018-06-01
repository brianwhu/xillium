package lab.data.persistence;

import javax.sql.DataSource;
import javax.annotation.Resource;

import java.util.*;
import java.sql.*;

import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.*;

import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.xillium.data.persistence.crud.CrudCommand;


@ContextConfiguration(locations={"/application-context.xml"})
//@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
@Transactional(readOnly=true)
public class CrudModelTest extends AbstractTransactionalTestNGSpringContextTests {

    @Resource
    private DataSource dataSource;

    private static final String username = "yep";
    private static final String tablenames = "MEMBERSHIP,MEMBERPREF,PURCHASE";
    private static final String tablenamesD = "*MEMBERSHIP,MEMBERPREF,*PURCHASE";
    private final Map<String, String> negative = new HashMap<String, String>();
    private final Map<String, String> positive = new HashMap<String, String>();
    private final DataBinder binder = new DataBinder();
    private Connection connection;

    @BeforeClass(groups={"crud", "crud-create", "crud-retrieve", "crud-update", "crud-delete", "crud-search"})
    public void beforeClass() {
        negative.put("EMAIL", "!'me@mail.com'");
        negative.put("LEVEL", "8");
        positive.put("EMAIL", "'steve@mail.com'");
        positive.put("LEVEL", "6");
        binder.put("level", "12");
        binder.put("telephone", "12");
    }

    @BeforeMethod(groups={"crud", "crud-create", "crud-retrieve", "crud-update", "crud-delete", "crud-search"})
    public void beforeMethod() throws Exception {
        connection = dataSource.getConnection();
    }

    @AfterMethod(groups={"crud", "crud-create", "crud-retrieve", "crud-update", "crud-delete", "crud-search"})
    public void afterMethod() throws Exception {
        connection.close();
    }

    @Test(groups={"crud", "crud-create"})
    public void crudCREATE() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.CREATE));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-create"})
    public void crudCREATExLEVEL() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.CREATE, new String[] { "LEVEL" }));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-create"})
    public void crudCREATExPositive() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.CREATE, positive));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-create"})
    public void crudCREATExNegative() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.CREATE, negative));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-create"})
    public void crudCREATExLEVELxPositive() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.CREATE, new String[] { "LEVEL" },
            positive
        ));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-create"})
    public void crudCREATExLEVELxNegative() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.CREATE, new String[] { "LEVEL" },
            negative
        ));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-retrieve"})
    public void crudRETRIEVE() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.RETRIEVE));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-retrieve"})
    public void crudRETRIEVEdominant() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenamesD, new CrudCommand.Action(CrudCommand.Operation.RETRIEVE));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-retrieve"})
    public void crudRETRIEVExNegative() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.RETRIEVE, negative));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-retrieve"})
    public void crudRETRIEVExPositive() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.RETRIEVE, positive));
        printAndValidate(command, connection);
    }

    //HSQLDB BUG http://sourceforge.net/p/hsqldb/bugs/1359/
    //@Test(groups={"crud", "crud-update"})
    public void crudUPDATE() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.UPDATE));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-update"})
    public void crudUPDATEw_FIRST_NAME_LAST_NAME() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.UPDATE, new String[] {
            "FIRST_NAME", "LAST_NAME"
        }));
        printAndValidate(command, connection);
    }

    //HSQLDB BUG http://sourceforge.net/p/hsqldb/bugs/1359/
    //@Test(groups={"crud", "crud-update"})
    public void crudUPDATEwLEVEL_TELEPHONE_FIRST_NAME() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.UPDATE, new String[] {
			"LEVEL", "TELEPHONE", "FIRST_NAME"
		}));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-update"})
    public void crudUPDATEwLEVEL_TELEPHONE_FIRST_NAMExPositive() throws Exception {
        CrudCommand
        command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.UPDATE, new String[] {
			"LEVEL", "TELEPHONE", "FIRST_NAME"
		}, positive));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-update"})
    public void crudUPDATEwLEVEL_TELEPHONE_FIRST_NAMExNegative() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.UPDATE, new String[] {
            "LEVEL", "TELEPHONE", "FIRST_NAME"
        }, negative));
        printAndValidate(command, connection);
    }

    //HSQLDB BUG http://sourceforge.net/p/hsqldb/bugs/1359/
    //@Test(groups={"crud", "crud-update"})
    public void crudUPDATExNegative() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.UPDATE, negative));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-delete"})
    public void crudDELETE() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.DELETE));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-delete"})
    public void crudDELETExPositive() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.DELETE, positive));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-delete"})
    public void crudDELETExNegative() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.DELETE, negative));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-search"})
    public void crudSEARCHwTELEPHONE_LEVEL_FIRST_NAME() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenamesD, new CrudCommand.Action(CrudCommand.Operation.SEARCH, new String[] {
            "TELEPHONE", "LEVEL", "*FIRST_NAME"
        }));
        printAndValidate(command, connection);
        System.out.println("CRUD to execute: " + command.choose(binder));
    }

    @Test(groups={"crud", "crud-search"})
    public void crudSEARCHcustomOps() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.SEARCH, new String[] {
            "TELEPHONE", "LEVEL<=", "*FIRST_NAME"
        }));
        printAndValidate(command, connection);
        System.out.println("CRUD to execute: " + command.choose(binder));
    }

    @Test(groups={"crud", "crud-search"})
    public void crudSEARCHwTELEPHONE_LEVEL_FIRST_NAME_EMAILxPositive() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.SEARCH, new String[] {
            "TELEPHONE", "LEVEL", "FIRST_NAME", "EMAIL"
        }, positive));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-search"})
    public void crudSEARCHwTELEPHONE_LEVEL_FIRST_NAME_EMAILxNegative() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.SEARCH, new String[] {
            "TELEPHONE", "LEVEL", "FIRST_NAME", "EMAIL"
        }, negative));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-search"})
    public void crudSEARCH() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.SEARCH));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-search"})
    public void crudSEARCHxNegative() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.SEARCH, negative));
        printAndValidate(command, connection);
    }

    @Test(groups={"crud", "crud-search"})
    public void crudSEARCHxPositive() throws Exception {
        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.SEARCH, positive));
        printAndValidate(command, connection);
    }

    private static void printAndValidate(CrudCommand command, Connection connection) throws Exception {
        System.out.println("------------------------------------------------------");
        System.out.println("------------------------------------------------------");
        System.out.println(command.getDescription());
        System.out.println("------------------------------------------------------");
        System.out.println("------------------------------------------------------");
        System.out.println("[class: " + command.getRequestType().getName() + ']');
        System.out.println(DataObject.Util.describe(command.getRequestType()));
        for (ParametricStatement statement: command.getStatements()) {
            System.out.println(statement.print(new StringBuilder()));
            try {
                connection.prepareStatement(statement.getSQL()).close();
                System.out.println("   VERIFIED");
            } catch (Exception x) {
                System.out.println("***ERROR: " + x.getMessage());
                throw x;
            }
        }
    }
}

