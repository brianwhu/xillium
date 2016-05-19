package org.xillium.core;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;
import javax.management.*;
import javax.servlet.*;
import javax.servlet.annotation.WebListener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.context.*;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.xillium.base.Functor;
import org.xillium.base.util.Bytes;
import org.xillium.base.util.Pair;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.core.conf.*;
import org.xillium.core.management.*;
import org.xillium.core.intrinsic.*;
import org.xillium.core.util.ModuleSorter;
import org.xillium.core.util.ServiceModule;


/**
 * Service Platform - initialization and termination.
 */
@WebListener
public final class ServicePlatform extends ManagedPlatform {
    private static final Logger _logger = Logger.getLogger(ServicePlatform.class.getName());

    private static final String VALIDATION_DIC = "validation-dictionary.xml";
    private static final String SERVICE_CONFIG = "service-configuration.xml";
    private static final String STORAGE_PREFIX = "storage-"; // still a special case as it depends on the Persistence bean
    private static final String XILLIUM_PREFIX = "xillium-";

    private static final String CONTROLLING = "controlling";
    private static final String PERSISTENCE = "persistence";

    private static class ServiceModuleInfo {
        Pair<Persistence, Persistence> persistence = new Pair<>(null, null); // root and latest
        List<ServiceAugmentation> augmentations = new ArrayList<>();
        Map<String, String> descriptions = new HashMap<>();
        List<Pair<String, PlatformAware>> plcas = new ArrayList<>(); // plca in regular modules
        Hashtable<String, String> attrs = new Hashtable<>(); // jmx object name attributes
    }

    private final Stack<ApplicationContext> _applc = new Stack<>();
    private final Stack<ObjectName> _manageables = new Stack<>();
    private final Stack<List<Pair<String, PlatformAware>>> _plca = new Stack<>();
    private static final org.xillium.data.validation.Reifier _dict = new org.xillium.data.validation.Reifier();

    private Set<String> _packaged, _extended;

/*
    public ServicePlatform() {
        _logger.log(Level.INFO, "Start HTTP Service Platform " + getClass().getName());
        _logger.log(Level.INFO, "java.util.logging.config.file=" + System.getProperty("java.util.logging.config.file"));
        _logger.log(Level.INFO, "java.util.logging.config.class=" + System.getProperty("java.util.logging.config.class"));
    }
*/

    static Pair<Service, Persistence> getService(String id) {
        return _registry.get(id);
    }

    static org.xillium.data.validation.Reifier getReifier() {
        return _dict;
    }

    /**
     * Tries to load an XML web application contenxt upon servlet context initialization. If a PlatformControl is detected
     * in the web application contenxt, registers it with the platform MBean server and stop. Otherwise, continues to load all
     * module contexts in the application.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        super.contextInitialized(event);
        _dict.addTypeSet(org.xillium.data.validation.StandardDataTypes.class);
        _packaged = _context.getResourcePaths("/WEB-INF/lib/");
        _extended = discover(System.getProperty("xillium.service.ExtensionsRoot"));

        // servlet mappings must be registered before this method returns
        Functor<Void, ServiceModule> functor = new Functor<Void, ServiceModule>() {
            public Void invoke(ServiceModule module) {
                _context.getServletRegistration("dispatcher").addMapping("/" + module.simple + "/*");
                return null;
            }
        };
        ServiceModule.scan(_context, _packaged, functor, _logger);
        ServiceModule.scan(_context, _extended, functor, _logger);

        try {
            XmlWebApplicationContext wac = new XmlWebApplicationContext();
            wac.setServletContext(_context);
            wac.refresh();
            wac.start();

            try {
                // let the life cycle control take over
                PlatformControl controlling = wac.getBean(CONTROLLING, PlatformControl.class);
                ManagementFactory.getPlatformMBeanServer().registerMBean(
                    controlling.bind(this, wac, Thread.currentThread().getContextClassLoader()),
                    new ObjectName(controlling.getClass().getPackage().getName(), "type", controlling.getClass().getSimpleName())
                );
                controlling.initialize();
            } catch (BeansException x) {
                // go ahead with platform realization
                realize(wac, null);
            } catch (Exception x) {
                _logger.warning(Throwables.getFirstMessage(x));
            }
        } catch (BeanDefinitionStoreException x) {
            _logger.warning(Throwables.getFirstMessage(x));
        }
    }

    /**
     * Reloads the platform with the given application context at its root.
     *
     * <p>If a PlatformControl is detected in the root web application context, module application contexts are <i>not</i> to be
     * loaded automatically when the servlet context is initialized. Consequently, this method can be called later to load all
     * module application contexts.</p>
     *
     * <p>This method can be called also to reload the platform, after the platform is unloaded by a call to unload().</p>
     */
    public void realize(ApplicationContext wac, ConfigurableApplicationContext child) {
        if (WebApplicationContextUtils.getWebApplicationContext(_context) == null) {
            _context.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
        } else {
            _logger.warning("Already realized");
            return;
        }

        if (child != null) wac = child;

        ServiceModuleInfo info = new ServiceModuleInfo();
        if (wac.containsBean(PERSISTENCE)) { // persistence may not be there if persistent storage is not required
            info.persistence.first = info.persistence.second = (Persistence)wac.getBean(PERSISTENCE);
            _persistences.put("-", info.persistence.first);
        }

        _logger.log(Level.CONFIG, "install packaged modules");
        wac = install(wac, sort(_context, _packaged), info);

        _logger.log(Level.CONFIG, "install extension modules");
        wac = install(wac, sort(_context, _extended), info);

        _logger.log(Level.CONFIG, "install service augmentations");
        for (ServiceAugmentation fi: info.augmentations) {
            fi.install(_registry);
        }

        String hide = System.getProperty("xillium.service.HideDescription");
        if (hide == null || hide.length() == 0) {
            _registry.put("x!/desc", new Pair<Service, Persistence>(new DescService(info.descriptions), info.persistence.first));
            _registry.put("x!/list", new Pair<Service, Persistence>(new ListService(_registry), info.persistence.first));
        }
        _registry.put("x!/ping", new Pair<Service, Persistence>(new PingService(), info.persistence.first));

        if (System.getProperty("xillium.persistence.DisablePrecompilation") == null) {
            for (Persistence persistence: _persistences.values()) {
                if (persistence.getTransactionManager() != null) {
                    persistence.doReadWrite(null, new Persistence.Task<Void, Void>() {
                        public Void run(Void facility, Persistence persistence) throws Exception {
                            _logger.info("parametric statements compiled: " + persistence.compile());
                            return null;
                        }
                    });
                } else {
                    _logger.warning("Persistence precompilation is ON (default) but TransactionManager is not configured");
                }
            }
        }
    }

    /**
     * Tries to destroy all module application contexts and the web application contenxt upon servlet context destruction.
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        destroy();
    }

    /**
     * Unloads the platform.
     *
     * <p>If this method is never called, all application contexts will be unloaded when the servlet context is destroyed.</p>
     */
    public void destroy() {
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(_context);
        if (wac != null) {
            _context.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        } else {
            _logger.warning("Nothing more to destroy");
            return;
        }

        _logger.info("<<<< Service Platform(" + _application + ") destruction starting");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        while (!_manageables.empty()) {
            ObjectName on = _manageables.pop();
            _logger.info("<<<<<<<< MBean(" + on + ") unregistration starting");
            try {
                mbs.unregisterMBean(on);
            } catch (Exception x) {
                _logger.log(Level.WARNING, on.toString());
            }
            _logger.info("<<<<<<<< MBean(" + on + ") unregistration complete");
        }
        while (!_plca.empty()) {
            List<Pair<String, PlatformAware>> plcas = _plca.pop();
            for (Pair<String, PlatformAware> plca: plcas) {
                _logger.info("<<<<<<<< PlatformAware(" + plca.first + '/' + plca.second.getClass().getName() + ") termination starting");
                plca.second.terminate(_application, plca.first);
                _logger.info("<<<<<<<< PlatformAware(" + plca.first + '/' + plca.second.getClass().getName() + ") termination complete");
            }
        }

        // finally, deregisters JDBC driver manually to prevent Tomcat 7 from complaining about memory leaks
        Enumeration<java.sql.Driver> drivers = java.sql.DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            java.sql.Driver driver = drivers.nextElement();
            _logger.info("<<<<<<<< JDBC Driver(" + driver + ") deregistration starting");
            try {
                java.sql.DriverManager.deregisterDriver(driver);
            } catch (java.sql.SQLException x) {
                _logger.log(Level.WARNING, "Error deregistering driver " + driver, x);
            }
            _logger.info("<<<<<<<< JDBC Driver(" + driver + ") deregistration complete");
        }

        for (Iterator<String> it = _registry.keySet().iterator(); it.hasNext();) {
            if (!it.next().startsWith("x!/")) it.remove();
        }
        _registry.remove("x!/ping");
        _registry.remove("x!/list");
        _registry.remove("x!/desc");
        while (!_applc.empty()) {
            destroy(_applc.pop());
        }
        destroy(wac);

        _logger.info("<<<< Service Platform(" + _application + ") destruction complete");
    }

    private void destroy(ApplicationContext ac) {
        if (ac instanceof Lifecycle) {
            try { ((Lifecycle)ac).stop(); } catch (Exception x) { _logger.log(Level.WARNING, "Error stopping context", x); }
        }
        if (ac instanceof DisposableBean) {
            try { ((DisposableBean)ac).destroy(); } catch (Exception x) { _logger.log(Level.WARNING, "Error destroying context", x); }
        }
    }

    // install service modules in the ModuleSorter.Sorted
    private ApplicationContext install(ApplicationContext wac, ModuleSorter.Sorted sorted, ServiceModuleInfo info) {
        // scan special modules, configuring and initializing PlatformAware objects as each module is loaded
        wac = install(wac, sorted.specials(), info, true);

        // scan regular modules, collecting all PlatformAware objects
        install(wac, sorted.regulars(), info, false);

        if (info.plcas.size() > 0) {
            _logger.info("configure PlatformAware objects in regular modules");
            for (Pair<String, PlatformAware> plca: info.plcas) {
                _logger.info("Configuring REGULAR PlatformAware " + plca.second.getClass().getName());
                plca.second.configure(_application, plca.first);
            }

            _logger.info("initialize PlatformAware objects in regular modules");
            for (Pair<String, PlatformAware> plca: info.plcas) {
                _logger.info("Initalizing REGULAR PlatformAware " + plca.second.getClass().getName());
                plca.second.initialize(_application, plca.first);
            }

            _plca.push(info.plcas);
            info.plcas = new ArrayList<Pair<String, PlatformAware>>();
        } else {
            _logger.info("No PlatformAware objects in regular modules");
        }

        return wac;
    }

    private ApplicationContext install(ApplicationContext wac, Iterator<ServiceModule> it, ServiceModuleInfo info, boolean isSpecial) {
        try {
            BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory(ValidationConfiguration.class, _dict);
            XMLBeanAssembler assembler = new XMLBeanAssembler(factory);

            while (it.hasNext()) {
                ServiceModule module = it.next();
                try {
                    JarInputStream jis = new JarInputStream(
                        module.path.startsWith("/") ? _context.getResourceAsStream(module.path) : new URL(module.path).openStream()
                    );
                    try {
                        _logger.config("Scanning module " + module.name + ", loading appl context and resources, special=" + isSpecial);

                        JarEntry entry;
                        ByteArrayInputStream serviceConfiguration = null;
                        List<ByteArrayInputStream> resources = new ArrayList<>();
                        boolean usingStorage = false;
                        while ((entry = jis.getNextJarEntry()) != null) {
                            String jarname = entry.getName();
                            if (jarname == null) continue;

                            if (SERVICE_CONFIG.equals(jarname)) {
                                _logger.config("ServiceConfiguration:" + module.path + ":" + jarname);
                                serviceConfiguration = new ByteArrayInputStream(Bytes.read(jis));
                            } else if (jarname.startsWith(STORAGE_PREFIX) && jarname.endsWith(".xml")) {
                                _logger.config("StorageConfiguration:" + module.path + ":" + jarname);
                                usingStorage = true;
                                resources.add(new ByteArrayInputStream(Bytes.read(jis)));
                            } else if (VALIDATION_DIC.equals(jarname)) {
                                _logger.config("ValidationDictionary:" + module.path + ":" + jarname);
                                resources.add(new ByteArrayInputStream(Bytes.read(jis)));
                            } else if (jarname.startsWith(XILLIUM_PREFIX) && jarname.endsWith(".xml")) {
                                _logger.config("ApplicationResources:" + module.path + ":" + jarname);
                                resources.add(new ByteArrayInputStream(Bytes.read(jis)));
                            }
                        }
                        if (serviceConfiguration != null) {
                            if (isSpecial) {
                                wac = load(wac, module, serviceConfiguration, info);
                            } else {
                                _applc.push(load(wac, module, serviceConfiguration, info));
                            }
                        }

                        if (info.persistence.second != null) {
                            factory.setBurnedIn(StorageConfiguration.class, info.persistence.second.getStatementMap(), module.simple);
                        } else if (usingStorage) {
                            throw new MissingResourceException("No persistence for storage, module: " + module.name, null, null);
                        }
                        factory.setBurnedIn(TextResources.class, module.simple);
                        for (ByteArrayInputStream stream: resources) {
                            assembler.build(stream);
                        }
                    } finally {
                        jis.close();
                    }
                    if (isSpecial && info.plcas.size() > 0) {
                        for (Pair<String, PlatformAware> plca: info.plcas) {
                            _logger.config("Configuring SPECIAL PlatformAware " + plca.second.getClass().getName());
                            plca.second.configure(_application, module.simple);
                        }

                        for (Pair<String, PlatformAware> plca: info.plcas) {
                            _logger.config("Initalizing SPECIAL PlatformAware " + plca.second.getClass().getName());
                            plca.second.initialize(_application, module.simple);
                        }
                        _plca.push(info.plcas);
                        info.plcas = new ArrayList<Pair<String, PlatformAware>>();
                    }
                } catch (IOException x) {
                    _logger.log(Level.WARNING, "Error during jar inspection, ignored", x);
                }
            }
        } catch (Exception x) {
            throw new RuntimeException("Failed to construct an XMLBeanAssembler", x);
        }

        _logger.config("Done with service modules scanning (" + (isSpecial ? "SPECIAL" : "REGULAR") + ')');
        return wac;
    }

    @SuppressWarnings("unchecked")
    private ApplicationContext load(ApplicationContext parent, ServiceModule module, InputStream stream, ServiceModuleInfo info) {
        GenericApplicationContext gac = new GenericApplicationContext(parent);
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(gac);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        reader.loadBeanDefinitions(new InputStreamResource(stream));
        gac.refresh();
        gac.start();

        _logger.config("Loading service modules from ApplicationContext " + gac.getId());

        // looking for a local Persistence bean and storing it in info.persistence.second; falling back to the root Persistence bean
        if (gac.containsLocalBean(PERSISTENCE)) {
            _persistences.put(module.simple, info.persistence.second = (Persistence)gac.getBean(PERSISTENCE));
        } else {
            info.persistence.second = info.persistence.first;
        }

        for (String id: gac.getBeanNamesForType(Service.class)) {
            String fullname = module.simple + '/' + id;

            try {
                Class<? extends DataObject> request = ((DynamicService)gac.getBean(id)).getRequestType();
                _logger.config("Service '" + fullname + "' request description captured: " + request.getName());
                info.descriptions.put(fullname, "json:" + DataObject.Util.describe((Class<? extends DataObject>)request));
            } catch (ClassCastException x) {
                try {
                    Class<?> request = Class.forName(gac.getBeanDefinition(id).getBeanClassName()+"$Request");
                    if (DataObject.class.isAssignableFrom(request)) {
                        _logger.config("Service '" + fullname + "' request description captured: " + request.getName());
                        info.descriptions.put(fullname, "json:" + DataObject.Util.describe((Class<? extends DataObject>)request));
                    } else {
                        _logger.warning("Service '" + fullname + "' defines a Request type that is not a DataObject");
                        info.descriptions.put(fullname, "json:{}");
                    }
                } catch (Exception t) {
                    _logger.config("Service '" + fullname + "' does not expose its request structure");
                    info.descriptions.put(fullname, "json:{}");
                }
            }

            _logger.config("Service '" + fullname + "' class=" + gac.getBean(id).getClass().getName());
            _registry.put(fullname, new Pair<Service, Persistence>((Service)gac.getBean(id), info.persistence.second));
        }

        // Service augmentations

        for (String id: gac.getBeanNamesForType(ServiceAugmentation.class)) {
            info.augmentations.add(gac.getBean(id, ServiceAugmentation.class).name(module.simple));
        }

        // Platform life cycle aware objects

        for (String id: gac.getBeanNamesForType(PlatformAware.class)) {
            info.plcas.add(new Pair<String, PlatformAware>(module.simple, (PlatformAware)gac.getBean(id)));
        }

        // Manageable object registration: objects are registered under "type=class-name,name=context-path/module-name/bean-id"

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (String id: gac.getBeanNamesForType(Manageable.class)) {
            try {
                Manageable manageable = (Manageable)gac.getBean(id);
                info.attrs.put("type", manageable.getClass().getSimpleName());
                info.attrs.put("name", _application + '/' + module.name + '/' + id);
                ObjectName name = new ObjectName(module.domain, info.attrs);
                manageable.assignObjectName(name);
                _logger.config("Registering MBean '" + id + "' as " + name);
                mbs.registerMBean(manageable, name);
                _manageables.push(name);
            } catch (Exception x) {
                _logger.log(Level.WARNING, "MBean '" + id + "' failed to register", x);
            }
        }

        _logger.config("Done with service modules in ApplicationContext " + gac.getId());
        return gac;
    }
}
