package org.xillium.data;

import java.util.*;
//import java.sql.ResultSet;


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
}
