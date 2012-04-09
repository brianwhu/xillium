package lab.data.persistence;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.*;
import javax.sql.DataSource;

import org.xillium.base.*;
import org.xillium.base.beans.*;
import org.xillium.data.CachedResultSet;
import org.xillium.data.DataBinder;
import org.xillium.data.DataObject;
import org.xillium.data.persistence.*;
import org.xillium.data.validation.Dictionary;
import lab.DataUtil;

public class Model {
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
        Connection conn = dataSource.getConnection();

        //DataBinder binder = new DataBinder();
        //DataUtil.loadFromArgs(binder, args, index);

        try {
            String[] names = new String[args.length - index];
            System.arraycopy(args, index, names, 0, args.length - index);
//            DataModel model = new DataModel(conn, "DEPARTMENT_ID", "sequence", names);
//            System.out.println(model.print(new StringBuilder()));
        } finally {
            conn.close();
        }
    }

    public static class Employee implements DataObject {
        public Integer employeeId;
        public String firstName;
        public String lastName;
        public String email;
        public String phoneNumber;
        public Timestamp hireDate;
        public String jobId;
        public Integer salary;
        public Float commissionPct;
        public Integer managerId;
        public Integer departmentId;
    }

    public static class Department implements DataObject {
        public Integer departmentId;
        public String departmentName;
        public Integer managerId;
        public Integer locationId;
    }
}
