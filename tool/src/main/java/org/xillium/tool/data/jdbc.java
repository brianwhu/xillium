package org.xillium.tool.data;

import java.util.*;
import java.util.logging.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.xillium.base.util.Bytes;
import org.xillium.base.util.Pair;
import org.xillium.base.beans.*;
import org.xillium.data.CachedResultSet;
import org.xillium.data.DataObject;
import org.xillium.data.DataBinder;
import org.xillium.data.validation.*;
import org.xillium.data.persistence.*;
import org.xillium.core.conf.StorageConfiguration;


public class jdbc {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: data.jdbc data-source.xml anonymous-sql-block.sql [ arguments ... ]");
            System.err.println("       data.jdbc data-source.xml statements.xml:list,of,names [ arguments ... ]");
            System.exit(0);
        }

        Logger.getLogger(XMLBeanAssembler.class.getName()).setLevel(Level.FINE);
        BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory();
        XMLBeanAssembler assembler = new XMLBeanAssembler(factory);
        DataSource dataSource = (DataSource)("-".equals(args[0]) ? assembler.build(System.in) : assembler.build(args[0]));

        List<Pair<ParametricStatement, Class<? extends DataObject>>> stmts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        int separator = args[1].indexOf(".xml:");
        if (separator > 0) {
            Map<String, ParametricStatement> map = new HashMap<String, ParametricStatement>();
            factory.setBurnedIn(StorageConfiguration.class, map, "-");
            assembler.build(args[1].substring(0, separator+4));
            for (String name: args[1].substring(separator+5).split(" *, *")) {
                ParametricStatement ps = map.get("-/" + name);
                stmts.add(new Pair<ParametricStatement, Class<? extends DataObject>>(ps, ps.getDataObjectClass("xillium.t.d.call")));
                names.add(name);
            }
        } else {
            ParametricStatement ps = args[1].startsWith("report") || args[1].indexOf("query.sql") > -1 ? new ParametricQuery() : new ParametricStatement();
            ps.set(new String(Bytes.read(new FileInputStream(args[1]), true)));
            stmts.add(new Pair<ParametricStatement, Class<? extends DataObject>>(ps, ps.getDataObjectClass("xillium.t.d.call")));
            names.add(args[1]);
        }

        System.err.println("connecting ...");
        try (Connection conn = dataSource.getConnection()) {
            System.err.println("connected at " + new java.util.Date());
            for (int i = 0; i < stmts.size(); ++i) {
        System.out.print("statement "); System.out.println(names.get(i));
                ParametricStatement ps = stmts.get(i).first;
                Class<? extends DataObject> c = stmts.get(i).second;
                CachedResultSet b = null;
                if (c != DataObject.Empty.class) {
                    DataObject t = c.newInstance();
                    if (args.length == 2) {
                        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                        System.out.print("Request = ");
                        System.out.println(mapper.writeValueAsString(mapper.readTree(DataObject.Util.describe(c))));
                    } else if (args.length == 3 && args[2].indexOf('=') < 0) {
                        t = new org.xillium.data.validation.Reifier().collect(t, new DataBinder().load(args[2]));
        System.out.println(" calling with"); System.out.println(Beans.toString(t));
                        b = execute(ps, t, conn);
        System.out.println("returned with"); System.out.println(Beans.toString(t));
                    } else if (args[2].indexOf('=') < 0) {
                        DataBinder binder = new DataBinder().load(args[2]);
                        t = new org.xillium.data.validation.Reifier().collect(t, binder.load(args, 3));
        System.out.println(" calling with"); System.out.println(Beans.toString(t));
                        b = execute(ps, t, conn);
        System.out.println("returned with"); System.out.println(Beans.toString(t));
                    } else {
                        t = new org.xillium.data.validation.Reifier().collect(t, new DataBinder().load(args, 2));
        System.out.println("calling with {"); System.out.print(Beans.toString(t)); System.out.println("}");
                        b = execute(ps, t, conn);
        System.out.println("returned with {"); System.out.print(Beans.toString(t)); System.out.println("}");
                    }
                } else {
        System.out.println(" calling w/o parameters");
                    b = execute(ps, (DataObject)null, conn);
                }
                if (b != null) {
        System.out.println(Beans.toString(b));
                }
            }
        } finally {
            System.err.println("disconnected");
        }
    }

    private static final CachedResultSet execute(ParametricStatement ps, DataObject data, Connection conn) throws Exception {
        long time = System.currentTimeMillis();
        try {
            if (ps instanceof ParametricQuery) {
                return ((ParametricQuery)ps).executeSelect(conn, data, CachedResultSet.BUILDER);
            } else {
                try {
                    ps.executeProcedure(conn, data);
                } catch (Exception x) {
                    if (x.getMessage().indexOf("Driver not capable") > -1) {
                        int affected = ps.executeUpdate(conn, data);
                        System.out.println("affected rows: " + affected);
                    }
                }
                return null;
            }
        } finally {
            System.out.println("Time: " + (System.currentTimeMillis() - time) + " ms");
        }
    }
}
