package org.xillium.data;

import java.util.*;


/**
 * A data binder
 */
public class DataBinder extends HashMap<String, String> {
    private final Map<String, CachedResultSet> _rsets = new HashMap<String, CachedResultSet>();
    private final Map<String, Object> _named = new HashMap<String, Object>();

    public CachedResultSet putResultSet(String name, CachedResultSet rs) {
        return _rsets.put(name, rs);
    }

    public CachedResultSet getResultSet(String name) {
        return _rsets.get(name);
    }

    public Set<String> getResultSetNames() {
        return _rsets.keySet();
    }

    public Object putNamedObject(String name, Object object) {
        return _named.put(name, object);
    }

    public Object getNamedObject(String name) {
        return _named.get(name);
    }

    public int estimateMaximumBytes() {
        int count = this.size();
        for (String key: _rsets.keySet()) {
            CachedResultSet crs = _rsets.get(key);
            count += crs.columns.length*crs.rows.size();
        }
        return count * 64;
    }
}
