package org.xillium.data;

import java.lang.reflect.Field;
import java.io.*;
import java.util.*;
import java.sql.*;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
import org.xillium.base.beans.JSONBuilder;
import org.xillium.data.persistence.ResultSetWorker;


/**
 * A data binder
 */
@SuppressWarnings("serial")
public class DataBinder extends HashMap<String, String> implements ResultSetWorker<DataBinder> {
    private final Map<String, CachedResultSet> _rsets = new HashMap<String, CachedResultSet>();
    private final Map<String, Object> _named = new HashMap<String, Object>();

    /**
     * Puts a new string value into this binder, but using an alternative value if the given one is null.
     */
    public String put(String name, String value, String alternative) {
        return put(name, value != null ? value : alternative);
    }

    /**
     * Puts a new result set into this binder.
     */
    public CachedResultSet putResultSet(String name, CachedResultSet rs) {
        return _rsets.put(name, rs);
    }

    /**
     * Retrieves a result set from this binder.
     */
    public CachedResultSet getResultSet(String name) {
        return _rsets.get(name);
    }

    /**
     * Retrieves the set of all result set names.
     */
    public Set<String> getResultSetNames() {
        return _rsets.keySet();
    }

    /**
     * Puts a named object into this binder.
     */
    public Object putNamedObject(String name, Object object) {
        return _named.put(name, object);
    }

    /**
     * Retrieves a named object from this binder.
     */
    public Object getNamedObject(String name) {
        return _named.get(name);
    }

    /**
     * Fills the data binder with columns in the current row of a result set.
     */
    @Override
    public DataBinder process(ResultSet rset) throws Exception {
        try {
            if (rset.next()) {
                ResultSetMetaData meta = rset.getMetaData();
                int width = meta.getColumnCount();
                for (int i = 1; i <= width; ++i) {
                    Object value = rset.getObject(i);
                    if (value != null) {
                        put(Strings.toLowerCamelCase(meta.getColumnName(i), '_'), value.toString());
                    }
                }
            } else {
                throw new NoSuchElementException("NoSuchRow");
            }
            return this;
        } finally {
            rset.close();
        }
    }

    /**
     * Fills the data binder with non-static, non-transient fields of an Object, excluding null values.
     */
    public DataBinder put(Object object) throws Exception {
        for (Field field: Beans.getKnownInstanceFields(object.getClass())) {
            Object value = field.get(object);
            if (value != null) {
                put(field.getName(), value.toString());
            }
        }
        return this;
    }

    /**
     * Removes all auto-values (those whose name starts and ends with '#').
     *
     * @return the number of auto-values removed
     */
    public int clearAutoValues() {
        int count = 0;
        Iterator<Map.Entry<String, String>> it = entrySet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            if (key.charAt(0) == '#' && key.charAt(key.length()-1) == '#') {
                it.remove();
                ++count;
            }
        }
        return count;
    }

    /**
     * Loads parameters from an array of strings each in the form of name=value.
     */
    public DataBinder load(String[] args, int offset) {
        for (int i = offset; i < args.length; ++i) {
            int equal = args[i].indexOf('=');
            if (equal > 0) {
                put(args[i].substring(0, equal), args[i].substring(equal + 1));
            } else {
                throw new RuntimeException("***InvalidParameter{" + args[i] + '}');
            }
        }
        return this;
    }

    /**
     * Loads parameters from a property file.
     */
    public DataBinder load(String filename) throws IOException {
        Properties props = new Properties();
        Reader reader = new FileReader(filename);
        props.load(reader);
        reader.close();
        return load(props);
    }

    /**
     * Loads parameters from a Properties object.
     */
    public DataBinder load(Properties props) {
        Enumeration<?> enumeration = props.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String)enumeration.nextElement();
            put(key, props.getProperty(key));
        }
        return this;
    }

    /**
     * Provides a very rough estimate of how big a JSON representative of this binder might be.
     */
    public int estimateMaximumBytes() {
        int count = this.size();
        for (String key: _rsets.keySet()) {
            CachedResultSet crs = _rsets.get(key);
            if (crs.rows != null) {
                count += crs.columns.length*crs.rows.size();
            }
        }
        return count * 64;
    }

    /**
     * Returns a JSON string representing the contents of this data binder, excluding named objects.
     */
    public String toJSON() {
        JSONBuilder jb = new JSONBuilder(estimateMaximumBytes()).append('{');

/*
        jb.quote("params").append(":{ ");
        Iterator<String> it = binder.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String val = binder.get(key);
            if (val == null) {
                jb.quote(key).append(":null");
            } else if (val.startsWith("json:")) {
                jb.quote(key).append(':').append(val.substring(5));
            } else {
                jb.serialize(key, val);
            }
            jb.append(',');
        }
        jb.replaceLast('}').append(',');
*/
        jb.quote("params").append(':');
        appendParams(jb).append(',');

/*
        jb.quote("tables").append(":{ ");
        Set<String> rsets = binder.getResultSetNames();
        it = rsets.iterator();
        while (it.hasNext()) {
            String name = it.next();
            jb.quote(name).append(":");
            binder.getResultSet(name).toJSON(jb);
            jb.append(',');
        }
        jb.replaceLast('}');
*/
        jb.quote("tables").append(':');
        appendTables(jb);

        jb.append('}');

        return jb.toString();
    }

    public JSONBuilder appendParams(JSONBuilder jb) {
        jb.append("{ ");
        Iterator<String> it = keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (key.charAt(0) == '#' && key.charAt(key.length()-1) == '#') continue;
            String val = get(key);
            if (val == null) {
                jb.quote(key).append(":null");
            } else if (val.startsWith("json:")) {
                jb.quote(key).append(':').append(val.substring(5));
            } else {
                jb.serialize(key, val);
            }
            jb.append(',');
        }
        jb.replaceLast('}');

        return jb;
    }

    public JSONBuilder appendTables(JSONBuilder jb) {
        jb.append("{ ");
        Set<String> rsets = getResultSetNames();
        Iterator<String> it = rsets.iterator();
        while (it.hasNext()) {
            String name = it.next();
            jb.quote(name).append(":");
            getResultSet(name).toJSON(jb);
            jb.append(',');
        }
        jb.replaceLast('}');

        return jb;
    }
}
