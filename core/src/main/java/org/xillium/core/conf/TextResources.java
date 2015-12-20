package org.xillium.core.conf;

import java.util.*;


/**
 * A collection of TextResource objects registered under unique names.
 */
public class TextResources {
    public static Map<String, String> map;
    private final String _name;

    public TextResources(String namespace) {
        _name = namespace;
    }

    public void addTextResource(TextResource resource) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(_name + '/' + resource.name, resource.text);
    }
}
