package org.xillium.data.presentation;

import java.lang.reflect.*;
import org.xillium.data.Transformer;
//import java.util.*;
//import org.xillium.data.validation.*;


/**
 * An object formatter.
 */
public class FieldFormatter extends FieldRetriever {
    private final Transformer _trans;

    public FieldFormatter(Field field, Transformer trans) {
        super(field);
        _trans = trans;
    }

    public Object get(Object object) throws IllegalArgumentException, IllegalAccessException {
        return _trans.transform(field.get(object));
    }

    public static FieldRetriever[] getFieldRetriever(Field[] fields) throws InstantiationException, IllegalAccessException {
        FieldRetriever[] retrievers = new FieldRetriever[fields.length];
        for (int i = 0; i < fields.length; ++i) {
            presentation present = fields[i].getAnnotation(presentation.class);
            if (present != null) {
                retrievers[i] = new FieldFormatter(fields[i], present.value().newInstance());
            } else {
                retrievers[i] = new FieldRetriever(fields[i]);
            }
        }
        return retrievers;
    }
}

