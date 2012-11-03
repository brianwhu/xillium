package org.xillium.data;

import java.util.*;
import java.sql.*;
import org.xillium.base.beans.Beans;
import org.xillium.data.persistence.ParametricQuery;


/**
 * A data binder
 */
public class DataBinder extends HashMap<String, String> implements ParametricQuery.ResultSetWorker<DataBinder>{
    private final Map<String, CachedResultSet> _rsets = new HashMap<String, CachedResultSet>();
    private final Map<String, Object> _named = new HashMap<String, Object>();

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
                    put(Beans.toLowerCamelCase(meta.getColumnName(i), '_'), rset.getObject(i).toString());
                }
            }
            return this;
        } finally {
            rset.close();
        }
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
}
