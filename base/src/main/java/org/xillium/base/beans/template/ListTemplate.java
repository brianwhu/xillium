package org.xillium.base.beans.template;

import java.util.*;


public abstract class ListTemplate<T> {
    public final List<T> list = new ArrayList<T>();

    public void add(T element) {
        list.add(element);
    }
}
