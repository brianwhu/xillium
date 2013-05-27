package org.xillium.data.persistence.xml;

//import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;
import org.xillium.base.beans.Beans;
import org.xillium.data.DataObject;


/**
 * A java bean mapped to a sql-rs:data element, the root element of an sql-rs XML document.
 */
public class Row<T extends DataObject> {
    private static final Logger _logger = Logger.getLogger(Row.class.getName());

    public final T data;

    /**
     * Constructs a Row object. All but the last argument are to be passed in from a BurnedInArgumentObjectFactory.
     *
     * @param type - a subclass of DataObject that defines public fields that match the columns in the XML document.
     */
    public Row(Class<T> type) throws Exception {
        this.data = type.newInstance();
    }

    public void add(Column<T> column) throws Exception {
        try {
            Field field = Beans.getKnownField(data.getClass(), column.name);
            Beans.setValue(data, Beans.getKnownField(data.getClass(), column.name), column.data);
        } catch (Exception x) {
            // ignore
        }
    }
}
