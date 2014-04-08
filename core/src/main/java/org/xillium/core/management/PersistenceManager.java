package org.xillium.core.management;

import java.io.*;
import java.util.*;
import java.sql.Connection;
import javax.sql.DataSource;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.persistence.*;


/**
 * PersistenceManager provides Persistence management.
 * <ul>
 * <li>l(list)      gets a list of all statement names</li>
 * <li>t(tell)      tells the details of a statement</li>
 * <li>d(define)    defines a statement</li>
 * <li>x(exec)      executes a statement</li>
 * </ul>
 */
public class PersistenceManager {
    private static final String MESSAGE = "persistence.exception";

    private final DataBinder _binder;
    private final Map<String, ParametricStatement> _statements;
    private final DataSource _source;
    private boolean _verbose;

    public PersistenceManager(DataBinder binder, Map<String, ParametricStatement> statements, DataSource source) {
        _binder = binder;
        _statements = statements;
        _source = source;
    }

    public PersistenceManager v(boolean verbose) {
        _verbose = verbose;
        return this;
    }

    public PersistenceManager l() {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (String name: _statements.keySet()) {
            rows.add(new Object[]{ name });
        }
        Collections.sort(rows, new Comparator<Object[]>() {
            public int compare(Object[] o1, Object[] o2) {
                return ((String)o1[0]).compareTo((String)o2[0]);
            }
        });
        _binder.putResultSet("statements", new CachedResultSet(new String[]{ "name" }, rows));
        return this;
    }

    public PersistenceManager t(String name) {
        ParametricStatement ps = _statements.get(name);
        if (ps != null) {
            try {
                Class<? extends DataObject> c = ps.getDataObjectClass(name.replace('/', '.'));
                _binder.put("interface", DataObject.Util.describe(c, "json:"));
                _binder.put("statement", ps.getSQL());
            } catch (Exception x) {
                _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
            }
        } else {
            _binder.put("statement", "");
        }
        return this;
    }

    public PersistenceManager d(String name, String xml) {
        try {
            XMLBeanAssembler assembler = new XMLBeanAssembler(new DefaultObjectFactory());
            _statements.put(name, (ParametricStatement)assembler.build(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public PersistenceManager x(String name) {
        try {
            ParametricStatement ps = _statements.get(name);
            Class<? extends DataObject> c = ps.getDataObjectClass(name.replace('/', '.'));
            DataObject data = c != null ? new org.xillium.data.validation.Dictionary().collect(c.newInstance(), _binder) : null;
            Connection conn = _source.getConnection();
            try {
                if (ps instanceof ParametricQuery) {
                    _binder.putResultSet("results", ((ParametricQuery)ps).executeSelect(conn, data, CachedResultSet.BUILDER));
                } else {
                    ps.executeProcedure(conn, data);
                    if (data != null) _binder.put(data);
                }
            } finally {
                conn.close();
            }
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }
}
