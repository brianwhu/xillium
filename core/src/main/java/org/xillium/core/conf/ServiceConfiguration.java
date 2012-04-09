package org.xillium.core.conf;

import org.xillium.core.*;
//import org.xillium.data.persistence.*;
import java.util.*;


/**
 * A collection of Service registered under unique names.
 */
public class ServiceConfiguration {
    private final Map<String, Service.Bean> _map;
    private final String _namespace;

    public ServiceConfiguration(Map<String, Service.Bean> registry, String namespace) {
        _map = registry;
        _namespace = namespace;
    }

    public void addService(Service service) throws Exception {
        _map.put(_namespace + '/' + service.name, (Service.Bean)Class.forName(service.className).newInstance());
    }
}
