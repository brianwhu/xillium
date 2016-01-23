package org.xillium.data;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.io.*;
import java.util.*;
import java.sql.*;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
import org.xillium.base.beans.JSONBuilder;
import org.xillium.base.util.Multimap;
import org.xillium.data.persistence.ResultSetWorker;


/**
 * A data binder
 */
public class DataBinder extends HashMap<String, String> implements ResultSetWorker<DataBinder> {
    /**
     * The recommended name for a "response" DataBinder when placed within another DataBinder.
     *
     * @see #DataBinder(DataBinder, String)
     */
    public static final String RESPONSE = "[RESPONSE]";

    private final Map<String, CachedResultSet> _rsets = new HashMap<String, CachedResultSet>();
    private final Map<String, Object> _named = new HashMap<String, Object>();
    private final DataBinder _lower;

    /**
     * Creates a standalone DataBinder.
     */
    public DataBinder() {
        _lower = null;
    }

    /**
     * Creates a DataBinder that sits on top of another binder.
     *
     * @param binder an existing binder
     */
    public DataBinder(DataBinder binder) {
        _lower = binder;
    }

    /**
     * Creates a DataBinder that sits on top of and at the same time is placed as a named object inside another binder.
     *
     * @param binder an existing binder
     * @param name the name of this new binder inside the existing binder
     */
    public DataBinder(DataBinder binder, String name) {
        _lower = binder;
        binder.putNamedObject(name, this);
    }

    public DataBinder getLower() {
        return _lower;
    }

    /**
     * Finds a string value from all data binders, starting from the current binder searching downwards.
     *
     * @param name the name of the value
     * @return the first value found
     */
    public String find(String name) {
        String value = null;
        for (DataBinder top = this; top != null && (value = top.get(name)) == null; top = top.getLower());
        return value;
    }

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
     * Puts a named object into this binder returning the original object under the name, if any.
     */
    @SuppressWarnings("unchecked")
    public <T, V> T putNamedObject(String name, V object) {
        return (T)_named.put(name, object);
    }

    /**
     * Retrieves a named object from this binder.
     */
    @SuppressWarnings("unchecked")
    public <T> T getNamedObject(String name) {
        return (T)_named.get(name);
    }

    /**
     * Retrieves a named object of a specific type from this binder.
     */
    public <T> T getNamedObject(String name, Class<T> type) {
        return type.cast(_named.get(name));
    }

    /**
     * Retrieves the set of all named object names.
     */
    public Set<String> getNamedObjectNames() {
        return _named.keySet();
    }

    /**
     * Introduces a HashMap under the given name if one does not exist yet.
     */
    public <K, V> Map<K, V> map(String name, Class<K> ktype, Class<V> vtype) {
        Map<K, V> map = getNamedObject(name);
        if (map == null) putNamedObject(name, map = new HashMap<K, V>());
        return map;
    }

    /**
     * Introduces a Multimap under the given name if one does not exist yet.
     */
    public <K, V> Multimap<K, V> mul(String name, Class<K> ktype, Class<V> vtype) {
        Multimap<K, V> map = getNamedObject(name);
        if (map == null) putNamedObject(name, map = new Multimap<K, V>());
        return map;
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
                        put(Strings.toLowerCamelCase(meta.getColumnLabel(i), '_'), value.toString());
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
     * Fills the data binder with a subset of non-static, non-transient fields of an Object, excluding null values.
     *
     * @param names - the names of the fields in the subset
     */
    public <T extends DataObject> DataBinder put(T object, String... names) throws Exception {
        Class<?> type = object.getClass();
        Object value;
        for (String name: names) {
            Field field = Beans.getKnownField(type, name);
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers()) && (value = field.get(object)) != null) {
                put(field.getName(), value.toString());
            }
        }
        return this;
    }

    /**
     * Removes all auto-values (those whose names start and end with '#').
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
        appendParams(jb).append(',');
        appendTables(jb);
        jb.append('}');

        return jb.toString();
    }

    public JSONBuilder appendParams(JSONBuilder jb) {
        boolean json = false;

        jb.append("\"params\":{ ");
        Iterator<String> it = keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String val = get(key);
            if (val == null) {
                jb.quote(key).append(":null");
            } else if (val.startsWith("json:")) {
                json = true;
                continue;
            } else {
                jb.serialize(key, val);
            }
            jb.append(',');
        }
        jb.replaceLast('}');

    if (json) {
        jb.append(",\"values\":{ ");
        it = keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String val = get(key);
            if (val != null && val.startsWith("json:")) {
                jb.quote(key).append(':').append(val.substring(5)).append(',');
            }
        }
        jb.replaceLast('}');
    }

        return jb;
    }

    public JSONBuilder appendTables(JSONBuilder jb) {
        jb.append("\"tables\":{ ");
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


    /**
     * A data binder decoder.
     */
    public static interface Decoder {
        /**
         * Decodes an InputStream and collects data parameters into a DataBinder.
         *
         * @return the InputStream
         */
        public InputStream decode(DataBinder binder, InputStream stream) throws Exception;
    }

    /**
     * A data binder encoder.
     */
    public static interface Encoder {
        /**
         * Returns the MIME content type of this encoder's output.
         *
         * @return the MIME content type
         */
        public String getContentType(DataBinder binder);

        /**
         * Encodes a DataBinder and writes the bytes into an OutputStream.
         *
         * @return the OutputStream
         */
        public <T extends OutputStream> T encode(T sink, DataBinder binder) throws Exception;
    }

    /**
     * An object equipped with a DataBinder.Decoder.
     */
    public static interface WithDecoder {
        /**
         * Returns a DataBinder.Decoder
         *
         * @return the Decoder
         */
        public Decoder getDataBinderDecoder();
    }

    /**
     * An object equipped with a DataBinder.Encoder.
     */
    public static interface WithEncoder {
        /**
         * Returns a DataBinder.Encoder
         *
         * @return the Encoder.
         */
        public Encoder getDataBinderEncoder();
    }

    private static final long serialVersionUID = -4575511652015221913L;
}
