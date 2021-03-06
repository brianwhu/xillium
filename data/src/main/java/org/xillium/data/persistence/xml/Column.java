package org.xillium.data.persistence.xml;

import java.lang.reflect.*;
import org.xillium.base.beans.Beans;
import org.xillium.data.DataObject;


/**
 * A java bean mapped to a sql-rs:column element.
 */
public class Column<T extends DataObject> {
    //private static final Logger _logger = Logger.getLogger(Column.class.getName());

    public final Class<T> type;
    public final String name;
    public Object data;

    /**
     * Constructs a Column object.
     *
     * @param type - the Class of the row(!) object
     * @param name - the name of this column
     */
    public Column(Class<T> type, String name) {
        this.type = type;
        this.name = name;
    }

    public void set(String text) throws Exception {
        try {
            Field field = Beans.getKnownField(type, name);
            Class<?> ftype = Beans.boxPrimitive(field.getType());
            Method valueOf = null;
            try {
                valueOf = ftype.getMethod("valueOf", String.class);
            } catch (NoSuchMethodException x) {
                try {
                    valueOf = ftype.getMethod("valueOf", Class.class, String.class);
                } catch (NoSuchMethodException y) {
                    // give up
                }
            }

            if (valueOf == null) {
                if (String.class.equals(ftype)) {
                    data = text;
                } else {
                    try {
                        data = ftype.getConstructor(String.class).newInstance(text);
                    } catch (Exception x) {
                        // give up
                    }
                }
            } else {
                data = valueOf.getParameterTypes().length == 1 ? valueOf.invoke(null, text) : valueOf.invoke(null, valueOf.getReturnType(), text);
            }
        } catch (NoSuchFieldException x) {
            // ignore
        }
    }
}
