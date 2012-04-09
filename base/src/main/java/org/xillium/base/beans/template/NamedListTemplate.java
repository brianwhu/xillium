package org.xillium.base.beans.template;

import java.util.*;


public abstract class NamedListTemplate<T> {
    public final String name;
    public final List<T> list = new ArrayList<T>();

    /**
     * Creates a new Insert that starts with the list of data objects.
     */
    public NamedListTemplate(String name) {
        this.name = name;
    }

    public void add(T element) {
        this.list.add(element);
    }
}
