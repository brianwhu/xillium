package org.xillium.base.beans.template;

import java.util.*;


public abstract class MapTemplate<T extends Onymous> {
    public final Map<String, T> map = new HashMap<String, T>();

    public void add(T element) {
        map.put(element.getName(), element);
    }

    public T get(String name) {
        return map.get(name);
    }
}
