package lab.data.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;

import org.xillium.base.beans.*;
import org.xillium.data.CachedResultSet;
import org.xillium.data.DataBinder;
import org.xillium.data.DataObject;
import org.xillium.data.persistence.*;
import org.xillium.data.validation.Dictionary;
import lab.DataUtil;

public class DataAccess {
    public static void main(String[] args) throws Exception {
        int index = 0;
/*
        Trace trace = null;

        if (args.length > 1 && args[0].equals("-v")) {
            trace = new StandardTrace().setLevel(Level.INFO);
            ++index;
        } else {
            trace = new NullTrace();
        }
*/
        XMLBeanAssembler assembler = new XMLBeanAssembler(new DefaultObjectFactory());
        DataSource dataSource = (DataSource)assembler.build(args[index++]);
        while (args[index].endsWith(".xml")) {
            assembler.build(args[index++]);
        }
        ParametricQuery DepartmentsByLocation = (ParametricQuery)StorageConfiguration.getParametricStatement("DepartmentsByLocation");
        ParametricQuery EmployeesWithSalaryAbove = (ParametricQuery)StorageConfiguration.getParametricStatement("EmployeesWithSalaryAbove");
        ParametricQuery EmployeesWithSalaryAbove2 = (ParametricQuery)StorageConfiguration.getParametricStatement("EmployeesWithSalaryAbove2");

        DataBinder binder = new DataBinder();
        DataUtil.loadFromArgs(binder, args, index);

        Connection conn = dataSource.getConnection();
        try {
            Data data = new Dictionary().collect(new Data(), binder);
            System.out.println(Beans.toString(data));

            //ResultSet rset = DepartmentsByLocation.intoResultSet(conn, data);
            DepartmentsByLocation.executeSelect(conn, data, new ParametricQuery.ResultSetWorker<Object>() {
                public Object process(ResultSet rset) throws Exception {
                    CachedResultSet crs = new CachedResultSet(rset);
                    System.out.println(Beans.toString(crs));
                    System.out.println(new JSONBuilder(1024).serialize(crs));
                    return null;
                }
            });

            //rset = EmployeesWithSalaryAbove.intoResultSet(conn, data);
            EmployeesWithSalaryAbove.executeSelect(conn, data, new ParametricQuery.ResultSetWorker<Object>() {
                public Object process(ResultSet rset) throws Exception {
                    CachedResultSet crs = new CachedResultSet(rset);
                    System.out.println(Beans.toString(crs));
                    System.out.println(new JSONBuilder(1024).serialize(crs));
                    return null;
                }
            });

            EmployeesWithSalaryAbove2.executeSelect(conn, data, new ParametricQuery.ResultSetWorker<Object>() {
                public Object process(ResultSet rset) throws Exception {
                    CachedResultSet crs = new CachedResultSet(rset);
                    System.out.println(Beans.toString(crs));
                    System.out.println(new JSONBuilder(1024).serialize(crs));
                    return null;
                }
            });
        } finally {
            conn.close();
        }
    }

    public static class Data implements DataObject {
        public Integer aSalary;
        public Integer aLocationId;
    }
}
