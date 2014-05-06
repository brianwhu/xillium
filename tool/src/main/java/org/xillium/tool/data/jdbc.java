package org.xillium.tool.data;

import java.util.*;
import java.util.logging.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.xillium.base.util.Bytes;
import org.xillium.base.beans.*;
import org.xillium.data.DataObject;
import org.xillium.data.DataBinder;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.*;
import org.xillium.core.conf.StorageConfiguration;


public class jdbc {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: data.jdbc data-source.xml anonymous-sql-block.sql [ arguments ... ]");
            System.err.println("Usage: data.jdbc data-source.xml anonymous-sql-block.xml:name [ arguments ... ]");
            System.exit(0);
        }

        Logger.getLogger(XMLBeanAssembler.class.getName()).setLevel(Level.FINE);
        BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory();
        XMLBeanAssembler assembler = new XMLBeanAssembler(factory);
        DataSource dataSource = (DataSource)assembler.build(args[0]);

        ParametricStatement ps = null;
        int separator = args[1].indexOf(".xml:");
        if (separator > 0) {
            Map<String, ParametricStatement> map = new HashMap<String, ParametricStatement>();
            factory.setBurnedIn(StorageConfiguration.class, map, "-");
            assembler.build(args[1].substring(0, separator+4));
            ps = map.get("-/" + args[1].substring(separator+5));
        } else {
            ps = new ParametricStatement();
            ps.set(new String(Bytes.read(new FileInputStream(args[1]), true)));
        }

        Class<? extends DataObject> c = ps.getDataObjectClass("xillium.t.d.call");
        if (c != DataObject.Empty.class) {
            DataObject t = c.newInstance();
            if (args.length == 2) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                System.out.print("Request = "); System.out.println(mapper.writeValueAsString(mapper.readTree(DataObject.Util.describe(c))));
            } else if (args.length == 3 && args[2].indexOf('=') < 0) {
                t = new org.xillium.data.validation.Dictionary().collect(t, new DataBinder().load(args[2]));
System.out.println(" calling with"); System.out.println(Beans.toString(t));
                execute(ps, t, dataSource);
System.out.println("returned with"); System.out.println(Beans.toString(t));
            } else if (args[2].indexOf('=') < 0) {
                DataBinder binder = new DataBinder().load(args[2]);
                t = new org.xillium.data.validation.Dictionary().collect(t, binder.load(args, 3));
System.out.println(" calling with"); System.out.println(Beans.toString(t));
                execute(ps, t, dataSource);
System.out.println("returned with"); System.out.println(Beans.toString(t));
            } else {
                t = new org.xillium.data.validation.Dictionary().collect(t, new DataBinder().load(args, 2));
System.out.println(" calling with"); System.out.println(Beans.toString(t));
                execute(ps, t, dataSource);
System.out.println("returned with"); System.out.println(Beans.toString(t));
            }
        } else {
System.out.println(" calling w/o parameters");
            execute(ps, (DataObject)null, dataSource);
        }
    }

    private static final void execute(ParametricStatement ps, DataObject data, DataSource source) throws SQLException {
        System.err.println("connecting ...");
        Connection conn = source.getConnection();
        System.err.println("connected");
        try {
            ps.executeProcedure(conn, data);
        } finally {
            conn.close();
            System.err.println("disconnected");
        }
    }
}
