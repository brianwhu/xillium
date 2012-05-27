package lab.data.persistence;

import javax.sql.DataSource;
import javax.annotation.Resource;

import java.util.*;
import java.sql.*;
import java.lang.reflect.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.*;

import org.xillium.base.beans.Beans;
import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.xillium.data.persistence.crud.CrudCommand;


@ContextConfiguration(locations={"/application-context.xml"})
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=false)
@Transactional(readOnly=true)
public class CrudModelTest extends AbstractTransactionalTestNGSpringContextTests {

    @Resource
    private DataSource dataSource;

    @Test(groups={"crud"})
    public void crud() throws Exception {
        String username = "yep";
        String tablenames = "MEMBERSHIP,MEMBERPREF,PURCHASE";
        Connection connection = dataSource.getConnection();

        CrudCommand command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.CREATE));
		System.out.println("INSERT: ------------------------------------------------");
        System.out.println(command.getRequestType().getName());
		System.out.println(DataObject.Util.describe(command.getRequestType()));
		for (ParametricStatement statement: command.getStatements()) {
			System.out.println(statement.print(new StringBuilder()));
		}

        command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.RETRIEVE));
		System.out.println("SELECT: ------------------------------------------------");
        System.out.println(command.getRequestType().getName());
		System.out.println(DataObject.Util.describe(command.getRequestType()));
		for (ParametricStatement statement: command.getStatements()) {
			System.out.println(statement.print(new StringBuilder()));
		}

        command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.UPDATE, new String[] {
			"LEVEL", "TELEPHONE", "FIRST_NAME"
		}));
		System.out.println("UPDATE: ------------------------------------------------");
        System.out.println(command.getRequestType().getName());
		System.out.println(DataObject.Util.describe(command.getRequestType()));
		for (ParametricStatement statement: command.getStatements()) {
			System.out.println(statement.print(new StringBuilder()));
		}

        command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.DELETE));
		System.out.println("DELETE: ------------------------------------------------");
        System.out.println(command.getRequestType().getName());
		System.out.println(DataObject.Util.describe(command.getRequestType()));
		for (ParametricStatement statement: command.getStatements()) {
			System.out.println(statement.print(new StringBuilder()));
		}

        command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.SEARCH, new String[] {
            "TELEPHONE", "LEVEL", "FIRST_NAME"
        }));
		System.out.println("SEARCH: ------------------------------------------------");
        System.out.println(command.getRequestType().getName());
		System.out.println(DataObject.Util.describe(command.getRequestType()));
		for (ParametricStatement statement: command.getStatements()) {
			System.out.println(statement.print(new StringBuilder()));
		}

        command = new CrudCommand(connection, username, tablenames, new CrudCommand.Action(CrudCommand.Operation.SEARCH));
		System.out.println("SEARCH: 2 ----------------------------------------------");
        System.out.println(command.getRequestType().getName());
		System.out.println(DataObject.Util.describe(command.getRequestType()));
		for (ParametricStatement statement: command.getStatements()) {
			System.out.println(statement.print(new StringBuilder()));
		}

        connection.close();
    }
}

