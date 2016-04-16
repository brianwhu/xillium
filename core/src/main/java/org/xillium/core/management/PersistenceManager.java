package org.xillium.core.management;

import java.io.*;
import java.util.*;
import java.sql.Connection;
import javax.sql.DataSource;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.xillium.core.Persistence;


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
    private static final Set<String> _customized = new HashSet<String>();

    private final DataBinder _binder;
    private final Map<String, Persistence> _persistences;
    private boolean _verbose;

    public PersistenceManager(DataBinder binder, Map<String, Persistence> persistences) {
        _binder = binder;
        _persistences = persistences;
    }

    public PersistenceManager v(boolean verbose) {
        _verbose = verbose;
        return this;
    }

    public PersistenceManager l() {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (String simple: _persistences.keySet()) {
            Persistence persistence = _persistences.get(simple);
            for (String name: persistence.getStatementMap().keySet()) {
                rows.add(new Object[]{ name });
            }
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
        ParametricStatement ps = _persistences.get(Strings.substringBefore(name, '/')).getParametricStatement(name);
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
            Persistence persistence = _persistences.get(Strings.substringBefore(name, '/'));
            XMLBeanAssembler assembler = new XMLBeanAssembler(new DefaultObjectFactory());
            if (!_customized.contains(name) && persistence.getParametricStatement(name) == null) {
                _customized.add(name);
            }
            persistence.getStatementMap().put(name, (ParametricStatement)assembler.build(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public PersistenceManager u(String name) {
        try {
            if (_customized.contains(name)) {
                _persistences.get(Strings.substringBefore(name, '/')).getStatementMap().remove(name);
                _customized.remove(name);
            } else {
                _binder.put(MESSAGE, "can't undefine internal");
            }
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public PersistenceManager x(final String name) {
        try {
            Persistence persistence = _persistences.get(Strings.substringBefore(name, '/'));
            ParametricStatement ps = persistence.getParametricStatement(name);

            Class<? extends DataObject> c = ps.getDataObjectClass(name.replace('/', '.'));
            DataObject data = c != null ? new org.xillium.data.validation.Reifier().collect(c.newInstance(), _binder) : null;
            if (ps instanceof ParametricQuery) {
                persistence.doReadOnly(data, new Persistence.Task<Void, DataObject>() {
                    public Void run(DataObject data, Persistence p) throws Exception {
                        _binder.putResultSet("results", p.executeSelect(name, data, CachedResultSet.BUILDER));
                        return null;
                    }
                });
            } else {
                persistence.doReadWrite(data, new Persistence.Task<Void, DataObject>() {
                    public Void run(DataObject data, Persistence p) throws Exception {
                        p.executeProcedure(name, data);
                        if (data != null) _binder.put(data);
                        return null;
                    }
                });
            }
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }
}
