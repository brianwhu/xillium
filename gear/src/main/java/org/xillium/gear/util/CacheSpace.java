package org.xillium.gear.util;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Callable;

import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
import org.xillium.data.DataObject;
import org.xillium.data.persistence.ObjectMappedQuery;
import org.xillium.data.persistence.Persistence;
import org.xillium.gear.model.coordinate;


/**
 * CacheSpace is an n-dimensional cartesian space to hold data objects, which can be assigned to either points or subspaces.
 * <p>Subspace-level data objects provide subspace-wide global values for all points within, but are overridden by values from
 * data objects assigned to points. A CacheSpace is always arranged so that a fully populated data object be assigned to the
 * whole space, ensuring that data values exist at every point in the space.</p>
 * Data objects in a CacheSpace must define 2 kinds of data members: coordinates and values.
 * <ol>
 * <li> Coordinate members must be of String type, and collectively form an ordered sequence that constitues the coordinate
 * system. Each coordinate member is annotated with <code>@coordinate</code> annotation specifying its order in the
 * coordinate system.</li>
 * <li> Value members must be of object types so that missing values can be clearly denoted by nulls.</li>
 * </ol>
 */
public class CacheSpace<T extends DataObject> {
    /**
     * A "match-any" coordinate wildcard.
     */
    public static final String ANY = "-";


    /**
     * Constructs a CacheSpace from a data object retriever. The retriever gives the data object class and the means to
     * load data objects from external resources.
     *
     * @param retriever a data object retriever whose type parameter {@code T} is the class of the data objects
     */
    public CacheSpace(Callable<List<T>> retriever) {
        _toplevel = new TopLevel<T>(deriveDataClass(retriever), retriever);
    }

    /**
     * Constructs a CacheSpace from a data object class. Data objects are retrieved from a database table whose name matches
     * that of the data object class.
     *
     * @param persistence a Persistence object
     * @param type the type of data objects
     */
    public CacheSpace(Persistence persistence, Class<T> type) {
        _toplevel = new TopLevel<T>(type, new DatabaseObjectRetriever<T>(persistence, type));
    }

    /**
     * Looks up a data object at a particular coordinate.
     *
     * @param keys the keys that form the coordinate
     */
    public T get(String... keys) {
        return _toplevel.get(keys, 0);
    }

    /**
     * Counts the number of data objects in the space.
     */
    public int count() {
        return _toplevel.count();
    }

    /**
     * (Re)loads data objects from an external source.
     */
    public void reload() {
        try {
            if (_toplevel.specifics != null) _toplevel.specifics.clear();
            _toplevel.global = null;

            for (T object: _toplevel.retriever.call()) {
                _toplevel.insert(object, _toplevel.coordinates, 0);
            }
            if (_toplevel.global == null) throw new IllegalStateException("***GlobalDataObjectMissing");

            _toplevel.spread();
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

    private final TopLevel<T> _toplevel;

    @SuppressWarnings("unchecked")
    private static <T extends DataObject> Class<T> deriveDataClass(Callable<List<T>> c) {
return (Class<T>)((ParameterizedType)((ParameterizedType)c.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]).getActualTypeArguments()[0];
    }

    private static class DatabaseObjectRetriever<T extends DataObject> implements Callable<List<T>> {
        private final Persistence _persistence;
        private final Persistence.Task<List<T>, Void> _task;

        public DatabaseObjectRetriever(Persistence persistence, Class<T> type) {
            final ObjectMappedQuery<T> statement = new ObjectMappedQuery<T>(type);
            statement.set("SELECT * FROM " + Strings.splitCamelCase(type.getSimpleName(), "_").toUpperCase());

            _persistence = persistence;
            _task = new Persistence.Task<List<T>, Void>() {
                public List<T> run(Void v, Persistence persistence) throws Exception {
                    return statement.getResults(persistence.getConnection(), null);
                }
            };
        }

        public List<T> call() {
            return _persistence.doReadOnly(null, _task);
        }
    }

    private static class Block<T extends DataObject> {
        public T global;
        public Map<Object, Block<T>> specifics;

        public T get(String[] keys, int index) {
            Block<T> block = specifics == null ? null : specifics.get(keys[index]);
            return block != null ? block.get(keys, index + 1) : global;
        }

        public void insert(T object, Field[] coordinates, int index) throws IllegalAccessException {
            if (index == coordinates.length) {
                global = object;
            } else {
                Object key = coordinates[index].get(object);
                if (ANY.equals(key)) {
                    global = object;
                } else {
                    if (specifics == null) specifics = new HashMap<Object, Block<T>>();
                    Block<T> next = specifics.get(key);
                    if (next == null) {
                        specifics.put(key, next = new Block<T>());
                    }
                    next.insert(object, coordinates, index + 1);
                }
            }
        }

        public void spread() {
            if (specifics != null) for (Block<T> block: specifics.values()) {
                if (block.global == null) {
                    block.global = global;
                } else {
                    Beans.fill(block.global, global);
                }
                block.spread();
            }
        }

        public int count() {
            int c = 1;
            if (specifics != null) for (Block<T> block: specifics.values()) {
                c += block.count();
            }
            return c;
        }
    }

    private static class TopLevel<T extends DataObject> extends Block<T> {
        public final Callable<List<T>> retriever;
        public final Field[] coordinates;

        public TopLevel(Class<T> type, Callable<List<T>> r) {
            retriever = r;
            TreeMap<Integer, Field> sorted = new TreeMap<Integer, Field>();
            for (Field field: type.getFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                coordinate c = field.getAnnotation(coordinate.class);
                if (c == null) continue;
                sorted.put(c.value(), field);
            }
            coordinates = sorted.values().toArray(new Field[sorted.size()]);
        }
    }
}

