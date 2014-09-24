package org.xillium.core.conf;

import java.util.List;
import java.util.Map;
import java.util.logging.*;

import org.xillium.core.util.ServiceMilestone;
import org.xillium.core.Service;


/**
 * A service augmentation specification embodies a collection of service filters and milestone evaluations
 * as well as instructions to get them installed onto services in the system. Instances are to be defined
 * in service-configuration.xml of each service module, but may affect services in other modules as well.
 * Service augmentation happens collectively at the end of service platform assembly, after all
 * PlatformLifeCycleAware objects in all modules have been initialized.
 * <xmp>
 *  <bean class="org.xillium.core.conf.ServiceAugmentation">
 *      <constructor-arg index="0"><list>
 *          <bean class="org.xillium.core.conf.ServiceAugmentation.Spec">
 *              <constructor-arg index="0" value="service1"/><constructor-arg index="1" ref="filter1"/>
 *          </bean>
 *          <bean class="org.xillium.core.conf.ServiceAugmentation.Spec">
 *              <constructor-arg index="0" value="service2"/><constructor-arg index="1" value="milestone1"/><constructor-arg index="2" ref="evaluation1"/>
 *          </bean>
 *      </list></constructor-arg>
 *  </bean>
 * </xmp>
 * Service names in a specification can take any of the following forms.
 * <ul>
 * <li><code>*</code>, indicating all services defined in all modules</li>
 * <li><code>/</code>, indicating all services defined in the current module</li>
 * <li>a local service name, a name without '/', indicating a specific service defined in the current module</li>
 * <li>a fully qualified service name</li>
 * </ul>
 */
public class ServiceAugmentation {
    private static final Logger _logger = Logger.getLogger(ServiceAugmentation.class.getName());

    public static String GLOBAL_ALL_SERVICES = "*";
    public static String MODULE_ALL_SERVICES = "/";

    public static class Spec {
        public final String service;
        public final String milestone;
        public final Object augment;

        public Spec(String s, Service.Filter a) {
            service = s;
            milestone = null;
            augment = a;
        }

        public Spec(String s, String m, ServiceMilestone.Evaluation a) {
            service = s;
            milestone = m;
            augment = a;
        }

        @Override
        public String toString() {
            if (milestone != null) {
                return milestone + "@" + service + "::" + augment;
            } else {
                return                   service + "::" + augment;
            }
        }
    }

    private final List<Spec> _specifications;
    private String _module;

    public ServiceAugmentation(List<Spec> specifications) {
        _specifications = specifications;
    }

    public ServiceAugmentation name(String name) {
        _module = name;
        return this;
    }

    /**
     * Installs the filters and milestone evaluations defined in this ServiceAugmentation on the services.
     */
    public void install(Map<String, Service> services) {
        for (Spec spec: _specifications) {
            if (spec.service.equals(GLOBAL_ALL_SERVICES)) {
                for (Map.Entry<String, Service> service: services.entrySet()) {
                    try {
                        if (spec.milestone == null) {
                            if (service.getValue() instanceof Service.Extendable) {
                                ((Service.Extendable)service.getValue()).setFilter((Service.Filter)spec.augment);
                            }
                        } else {
                            ServiceMilestone.attach(service.getValue(), spec.milestone, (ServiceMilestone.Evaluation)spec.augment);
                        }
                    } catch (Exception x) {
                        _logger.log(Level.WARNING, service.getKey(), x);
                    }
                }
            } else if (spec.service.equals(MODULE_ALL_SERVICES)) {
                for (Map.Entry<String, Service> service: services.entrySet()) {
                    try {
                        if (spec.milestone == null) {
                            if (service.getKey().startsWith(_module + '/') && service.getValue() instanceof Service.Extendable) {
                                ((Service.Extendable)service.getValue()).setFilter((Service.Filter)spec.augment);
                            }
                        } else {
                            ServiceMilestone.attach(service.getValue(), spec.milestone, (ServiceMilestone.Evaluation)spec.augment);
                        }
                    } catch (Exception x) {
                        _logger.log(Level.WARNING, service.getKey(), x);
                    }
                }
            } else if (spec.service.indexOf('/') > -1) {
                try {
                    if (spec.milestone == null) {
                        ((Service.Extendable)services.get(spec.service)).setFilter((Service.Filter)spec.augment);
                    } else {
                        ServiceMilestone.attach(services.get(spec.service), spec.milestone, (ServiceMilestone.Evaluation)spec.augment);
                    }
                } catch (Exception x) {
                    _logger.log(Level.WARNING, spec.service, x);
                }
            } else {
                try {
                    if (spec.milestone == null) {
                        ((Service.Extendable)services.get(_module + '/' + spec.service)).setFilter((Service.Filter)spec.augment);
                    } else {
                        ServiceMilestone.attach(services.get(_module + '/' + spec.service), spec.milestone, (ServiceMilestone.Evaluation)spec.augment);
                    }
                } catch (Exception x) {
                    _logger.log(Level.WARNING, spec.service, x);
                }
            }
        }

        _specifications.clear();
        _module = null;
    }

}
