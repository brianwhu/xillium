package org.xillium.data.presentation;

import java.lang.reflect.*;
//import java.util.*;
//import org.xillium.data.validation.*;


/**
 * A straightforward field value retriever
 */
public class FieldRetriever {
    public final Field field;

    public FieldRetriever(Field field) {
        this.field = field;
    }

    public Object get(Object object) throws IllegalArgumentException, IllegalAccessException {
        return field.get(object);
    }
}

