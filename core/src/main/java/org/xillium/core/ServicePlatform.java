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
import javax.servlet.http.*;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.xillium.base.util.Bytes;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.xml.*;
import org.xillium.core.conf.*;
import org.xillium.core.management.*;
import org.xillium.core.intrinsic.*;
import org.xillium.core.util.ModuleSorter;


/**
 * Service Platform - initialization and termination.
 */
@WebListener
public class ServicePlatform extends ManagedPlatform {

    private static final String VALIDATION_DIC = "validation-dictionary.xml";
    private static final String SERVICE_CONFIG = "service-configuration.xml";
    private static final String STORAGE_PREFIX = "storage-"; // still a special case as it depends on the Persistence bean
    private static final String XILLIUM_PREFIX = "xillium-";
    private static final Logger _logger = Logger.getLogger(ServicePlatform.class.getName());

    private static class PlatformLifeCycleAwareDef {
        PlatformLifeCycleAware bean;
        String module;

        PlatformLifeCycleAwareDef(PlatformLifeCycleAware b, String m) {
            bean = b;
            module = m;
        }
    }

    private static class ServiceModuleInfo {
        List<FiltersInstallation> filters = new ArrayList<FiltersInstallation>();
        Map<String, String> descriptions = new HashMap<String, String>();
        List<PlatformLifeCycleAwareDef> plcas;
    }


    private final Stack<ObjectName> _manageables = new Stack<ObjectName>();
    private final Stack<List<PlatformLifeCycleAwareDef>> _plca = new Stack<List<PlatformLifeCycleAwareDef>>();
    private static final org.xillium.data.validation.Dictionary _dict = new org.xillium.data.validation.Dictionary();

    // Wired in spring application context
    private Persistence _persistence;

    public ServicePlatform() {
        _logger.log(Level.INFO, "Start HTTP Service Platform " + getClass().getName());
        _logger.log(Level.INFO, "java.util.logging.config.file=" + System.getProperty("java.util.logging.config.file"));
        _logger.log(Level.INFO, "java.util.logging.config.class=" + System.getProperty("java.util.logging.config.class"));
    }

    static Service getService(String id) {
        return _registry.get(id);
    }

    static org.xillium.data.validation.Dictionary getDictionary() {
        return _dict;
    }

    /**
     * Initializes the servlet, loading and initializing xillium modules.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        super.contextInitialized(event);
        ServletContext context = event.getServletContext();

        ApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(context);
        _dict.addTypeSet(org.xillium.data.validation.StandardDataTypes.class);
        if (wac.containsBean("persistence")) { // persistence may not be there if persistent storage is not required
            _persistence = (Persistence)wac.getBean("persistence");
            _statements = _persistence.getStatementMap();
        }

        ServiceModuleInfo info = new ServiceModuleInfo();

        _logger.log(Level.CONFIG, "install packaged modules");
        wac = installServiceModules(context, wac, sort(context, context.getResourcePaths("/WEB-INF/lib/")), info);

        _logger.log(Level.CONFIG, "install extension modules");
        wac = installServiceModules(context, wac, sort(context, discover(System.getProperty("xillium.service.ExtensionsRoot"))), info);

        _logger.log(Level.CONFIG, "install service filters");
        for (FiltersInstallation fi: info.filters) {
            fi.install(_registry);
        }

        String hide = System.getProperty("xillium.service.HideDescription");
        if (hide == null || hide.length() == 0) {
            _registry.put("x!/desc", new DescService(info.descriptions));
            _registry.put("x!/list", new ListService(_registry));
        }
        _registry.put("x!/ping", new PingService());

        if (_persistence != null && System.getProperty("xillium.persistence.DisablePrecompilation") == null) {
            _persistence.doReadOnly(null, new Persistence.Task<Void, Void>() {
                public Void run(Void facility, Persistence persistence) throws Exception {
                    _logger.info("parametric statements compiled: " + persistence.compile());
                    return null;
                }
            });
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        _logger.info("<<<< Service Dispatcher(" + _application + ") destruction starting");
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
            List<PlatformLifeCycleAwareDef> plcas = _plca.pop();
            for (PlatformLifeCycleAwareDef plca: plcas) {
                _logger.info("<<<<<<<< PlatformLifeCycleAware(" + plca.module + '/' + plca.bean.getClass().getName() + ") termination starting");
                plca.bean.terminate(_application, plca.module);
                _logger.info("<<<<<<<< PlatformLifeCycleAware(" + plca.module + '/' + plca.bean.getClass().getName() + ") termination complete");
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
        _logger.info("<<<< Service Dispatcher(" + _application + ") destruction complete");

        super.contextDestroyed(event);
    }

    private ApplicationContext installServiceModules(ServletContext context, ApplicationContext wac, ModuleSorter.Sorted sorted, ServiceModuleInfo info) {
        // scan special modules, configuring and initializing PlatformLifeCycleAware objects as each module is loaded
        info.plcas = null;
        wac = scanServiceModules(context, sorted.specials(), wac, info);

        // scan regular modules, collecting all PlatformLifeCycleAware objects
        info.plcas = new ArrayList<PlatformLifeCycleAwareDef>();
        scanServiceModules(context, sorted.regulars(), wac, info);

        _logger.info("configure PlatformLifeCycleAware objects in regular modules");
        for (PlatformLifeCycleAwareDef plca: info.plcas) {
            _logger.info("Configuring REGULAR PlatformLifeCycleAware " + plca.bean.getClass().getName());
            plca.bean.configure(_application, plca.module);
        }

        _logger.info("initialize PlatformLifeCycleAware objects in regular modules");
        for (PlatformLifeCycleAwareDef plca: info.plcas) {
            _logger.info("Initalizing REGULAR PlatformLifeCycleAware " + plca.bean.getClass().getName());
            plca.bean.initialize(_application, plca.module);
        }

        _plca.push(info.plcas);

        return wac;
    }

    private ApplicationContext scanServiceModules(ServletContext context, Iterator<ModuleSorter.Entry> it, ApplicationContext wac, ServiceModuleInfo info) {
        boolean isSpecial = info.plcas == null;
        if (isSpecial) {
            info.plcas = new ArrayList<PlatformLifeCycleAwareDef>();
        }

        try {
            BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory(ValidationConfiguration.class, _dict);
            XMLBeanAssembler assembler = new XMLBeanAssembler(factory);

            while (it.hasNext()) {
                ModuleSorter.Entry module = it.next();
                try {
                    JarInputStream jis = new JarInputStream(
                        module.path.startsWith("/") ? context.getResourceAsStream(module.path) : new URL(module.path).openStream()
                    );
                    try {
                        String domain = jis.getManifest().getMainAttributes().getValue(DOMAIN_NAME);
                        _logger.config("Scanning module " + module.name + ", special=" + isSpecial);
                        if (_persistence != null) {
                            factory.setBurnedIn(StorageConfiguration.class, _persistence.getStatementMap(), module.name);
                        }

                        // go through service resources, loading service configuration at last
                        JarEntry entry;
                        ByteArrayInputStream serviceConfiguration = null;
                        while ((entry = jis.getNextJarEntry()) != null) {
                            String jarname = entry.getName();
                            if (jarname == null) continue;

                            if (SERVICE_CONFIG.equals(jarname)) {
                                _logger.config("ServiceConfiguration:" + module.path + ":" + jarname);
                                serviceConfiguration = new ByteArrayInputStream(Bytes.read(jis));
                            } else if (_persistence != null && jarname.startsWith(STORAGE_PREFIX) && jarname.endsWith(".xml")) {
                                _logger.config("StorageConfiguration:" + module.path + ":" + jarname);
                                assembler.build(new ByteArrayInputStream(Bytes.read(jis)));
                            } else if (VALIDATION_DIC.equals(jarname)) {
                                _logger.config("ValidationDictionary:" + module.path + ":" + jarname);
                                assembler.build(new ByteArrayInputStream(Bytes.read(jis)));
                            } else if (jarname.startsWith(XILLIUM_PREFIX) && jarname.endsWith(".xml")) {
                                _logger.config("ApplicationResources:" + module.path + ":" + jarname);
                                assembler.build(new ByteArrayInputStream(Bytes.read(jis)));
                            }
                        }
                        if (serviceConfiguration != null) {
                            if (isSpecial) {
                                wac = load(context, wac, domain, module.name, serviceConfiguration, info);
                            } else {
                                load(context, wac, domain, module.name, serviceConfiguration, info);
                            }
                        }
                    } finally {
                        jis.close();
                    }
                    if (isSpecial) {
                        for (PlatformLifeCycleAwareDef plca: info.plcas) {
                            _logger.config("Configuring SPECIAL PlatformLifeCycleAware " + plca.bean.getClass().getName());
                            plca.bean.configure(_application, module.name);
                        }

                        for (PlatformLifeCycleAwareDef plca: info.plcas) {
                            _logger.config("Initalizing SPECIAL PlatformLifeCycleAware " + plca.bean.getClass().getName());
                            plca.bean.initialize(_application, module.name);
                        }
                        _plca.push(info.plcas);
                        info.plcas = new ArrayList<PlatformLifeCycleAwareDef>();
                    }
                    context.getServletRegistration("dispatcher").addMapping("/" + module.name + "/*");
                } catch (IOException x) {
                    // ignore this jar
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
    private ApplicationContext load(ServletContext context, ApplicationContext wac, String domain, String name, InputStream stream, ServiceModuleInfo info) {
        GenericApplicationContext gac = new GenericApplicationContext(wac);
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(gac);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        reader.loadBeanDefinitions(new InputStreamResource(stream));
        gac.refresh();

        _logger.config("Loading service modules from ApplicationContext " + gac.getId());

        for (String id: gac.getBeanNamesForType(Service.class)) {
            String fullname = name + '/' + id;

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
            _registry.put(fullname, (Service)gac.getBean(id));
        }

        // Filter installations

        for (String id: gac.getBeanNamesForType(FiltersInstallation.class)) {
            info.filters.add(gac.getBean(id, FiltersInstallation.class).name(name));
        }

        // Platform life cycle aware objects

        for (String id: gac.getBeanNamesForType(PlatformLifeCycleAware.class)) {
            info.plcas.add(new PlatformLifeCycleAwareDef((PlatformLifeCycleAware)gac.getBean(id), name));
        }

        // Manageable object registration: objects are registered under "bean-id/context-path"

        String contextPath = context.getContextPath();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (String id: gac.getBeanNamesForType(Manageable.class)) {
            try {
                _logger.config("Registering MBean '" + id + "', domain=" + domain);
                ObjectName on = new ObjectName(domain == null ? "org.xillium.core.management" : domain, "type", id + contextPath);
                Manageable manageable = (Manageable)gac.getBean(id);
                manageable.assignObjectName(on);
                mbs.registerMBean(manageable, on);
                _manageables.push(on);
            } catch (Exception x) {
                _logger.log(Level.WARNING, "MBean '" + id + "' failed to register", x);
            }
        }

        _logger.config("Done with service modules in ApplicationContext " + gac.getId());
        return gac;
    }
}
