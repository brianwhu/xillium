package org.xillium.core.conf;

import java.util.List;
import java.util.Map;
import java.util.logging.*;

import org.xillium.base.etc.Pair;
import org.xillium.core.Service;


/**
 * This class encapsulates a collection of filter installation specifications. Instances are to be defined
 * in service-configuration.xml of each service module, but may affect services in other modules as well.
 * Filter installation happens collectively at the end of service platform assembly, after all
 * PlatformLifeCycleAware objects in all modules have been initialized.
 * <xmp>
 *  <bean class="org.xillium.core.conf.FiltersInstallation">
 *      <constructor-arg index="0"><list>
 *          <bean class="org.xillium.base.etc.Pair"><constructor-arg index="0" value="service1"/><constructor-arg index="1" ref="filter1"/></bean>
 *          <bean class="org.xillium.base.etc.Pair"><constructor-arg index="0" value="service2"/><constructor-arg index="1" ref="filter1"/></bean>
 *      </list></constructor-arg>
 *  </bean>
 * </xmp>
 * Service names in a filter installation specification can take any of the following forms.
 * <ul>
 * <li><code>*</code>, indicating all services defined in all modules</li>
 * <li><code>/</code>, indicating all services defined in the current module</li>
 * <li>a local service name, a name without '/', indicating a specific service defined in the current module</li>
 * <li>a fully qualified service name</li>
 * </ul>
 * <p/>
 */
public class FiltersInstallation {
    private static final Logger _logger = Logger.getLogger(FiltersInstallation.class.getName());
    private final List<Pair<String, Service.Filter>> _filters;
    private String _module;

    public static String GLOBAL_ALL_SERVICES = "*";
    public static String MODULE_ALL_SERVICES = "/";

    public FiltersInstallation(List<Pair<String, Service.Filter>> filters) {
        _filters = filters;
    }

    public FiltersInstallation name(String name) {
        _module = name;
        return this;
    }

    /**
     * Installs the filters defined in this FiltersInstallation on the services.
     */
    public void install(Map<String, Service> services) {
        for (Pair<String, Service.Filter> filter: _filters) {
            if (filter.first.equals(GLOBAL_ALL_SERVICES)) {
                for (Map.Entry<String, Service> service: services.entrySet()) {
                    try {
                        if (service.getValue() instanceof Service.Extendable) {
                            ((Service.Extendable)service.getValue()).setFilter(filter.second);
                        }
                    } catch (Exception x) {
                        _logger.log(Level.WARNING, service.getKey(), x);
                    }
                }
            } else if (filter.first.equals(MODULE_ALL_SERVICES)) {
                for (Map.Entry<String, Service> service: services.entrySet()) {
                    try {
                        if (service.getKey().startsWith(_module + '/') && service.getValue() instanceof Service.Extendable) {
                            ((Service.Extendable)service.getValue()).setFilter(filter.second);
                        }
                    } catch (Exception x) {
                        _logger.log(Level.WARNING, service.getKey(), x);
                    }
                }
            } else if (filter.first.indexOf('/') > -1) {
                try {
                    ((Service.Extendable)services.get(filter.first)).setFilter(filter.second);
                } catch (Exception x) {
                    _logger.log(Level.WARNING, filter.first, x);
                }
            } else {
                try {
                    ((Service.Extendable)services.get(_module + '/' + filter.first)).setFilter(filter.second);
                } catch (Exception x) {
                    _logger.log(Level.WARNING, filter.first, x);
                }
            }
        }

        _filters.clear();
        _module = null;
    }
}
