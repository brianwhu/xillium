package org.xillium.core.conf;

import java.util.*;


/**
 * A collection of TextResource objects registered under unique names.
 */
public class TextResources {
    public static Map<String, String> map;

    public void addTextResource(TextResource resource) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(resource.name, resource.text);
    }
}
