package org.xillium.core;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;
import java.util.regex.*;
import javax.management.*;
import javax.servlet.*;
//import javax.servlet.annotation.*;
import javax.servlet.http.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.context.support.*;
import org.xillium.base.etc.Arrays;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.persistence.crud.CrudConfiguration;
import org.xillium.core.conf.*;
import org.xillium.core.management.*;
import org.xillium.core.intrinsic.*;
import org.xillium.core.util.ModuleSorter;


/**
 * Platform Service Dispatcher.
 *
 * This servlet dispatches inbound HTTP calls to registered services based on request URI. A valid request URI is in the form of
 * <pre>
 *      /[context]/[module]/[service]?[params]=...
 * </pre>
 * When a request URI matches the above pattern, this servlet looks up a Service instance registered under the name 'module/service'.
 * <p/>
 */
//@WebServlet(urlPatterns="/", asyncSupported=true)
public class HttpServiceDispatcher extends HttpServlet {
    private static final String DOMAIN_NAME = "Xillium-Domain-Name";
    private static final String MODULE_NAME = "Xillium-Module-Name";
    private static final String MODULE_BASE = "Xillium-Module-Base";

    private static final String VALIDATION_DIC = "validation-dictionary.xml";
    private static final String SERVICE_CONFIG = "service-configuration.xml";
    private static final String STORAGE_PREFIX = "storage-"; // still a special case as it depends on the Persistence bean
    private static final String XILLIUM_PREFIX = "xillium-";

    private static final Pattern URI_REGEX = Pattern.compile("/[^/?]+/([^/?]+/[^/?]+)"); // '/context/module/service'
    private static final Pattern SQL_CONSTRAINT = Pattern.compile("\\([A-Z_]+\\.([A-Z_]+)\\)");
    private static final File TEMPORARY = null;
    private static final Logger _logger = Logger.getLogger(HttpServiceDispatcher.class.getName());

    private static class PlatformLifeCycleAwareDef {
        PlatformLifeCycleAware bean;
        String module;

        PlatformLifeCycleAwareDef(PlatformLifeCycleAware b, String m) {
            bean = b;
            module = m;
        }
    }

    private final Stack<ObjectName> _manageables = new Stack<ObjectName>();
    private final Stack<List<PlatformLifeCycleAwareDef>> _plca = new Stack<List<PlatformLifeCycleAwareDef>>();
    private final Map<String, Service> _services = new HashMap<String, Service>();
    private final org.xillium.data.validation.Dictionary _dict = new org.xillium.data.validation.Dictionary();

    // Servlet context path without the leading '/'
    private String _application;

    // Wired in spring application context
    private Persistence _persistence;

    public HttpServiceDispatcher() {
        _logger.log(Level.INFO, "START HTTP service dispatcher " + getClass().getName());
        _logger.log(Level.INFO, "java.util.logging.config.class=" + System.getProperty("java.util.logging.config.class"));
    }

    /**
     * Initializes the servlet, loading and initializing xillium modules.
     */
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        _application = context.getContextPath();
        if (_application.charAt(0) == '/') _application = _application.substring(1);

        ApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        _dict.addTypeSet(org.xillium.data.validation.StandardDataTypes.class);
        if (wac.containsBean("persistence")) { // persistence may not be there if persistent storage is not required
            _persistence = (Persistence)wac.getBean("persistence");
        }

        // if intrinsic services are wanted
        Map<String, String> descriptions = new HashMap<String, String>();

        ModuleSorter.Sorted sorted = sortServiceModules();

        // scan special modules, configuring and initializing PlatformLifeCycleAware objects as each module is loaded
        wac = scanServiceModules(sorted.specials(), wac, descriptions, null);

        // scan regular modules, collecting all PlatformLifeCycleAware objects
        List<PlatformLifeCycleAwareDef> plcas = new ArrayList<PlatformLifeCycleAwareDef>();
        scanServiceModules(sorted.regulars(), wac, descriptions, plcas);

        _logger.info("configure PlatformLifeCycleAware objects in regular modules");
        for (PlatformLifeCycleAwareDef plca: plcas) {
            _logger.info("Configuring REGULAR PlatformLifeCycleAware " + plca.bean.getClass().getName());
            plca.bean.configure(_application, plca.module);
        }

        _logger.info("initialize PlatformLifeCycleAware objects in regular modules");
        for (PlatformLifeCycleAwareDef plca: plcas) {
            _logger.info("Initalizing REGULAR PlatformLifeCycleAware " + plca.bean.getClass().getName());
            plca.bean.initialize(_application, plca.module);
        }

        _plca.push(plcas);

        String hide = System.getProperty("xillium.service.HideDescription");
        if (hide == null || hide.length() == 0) {
            _services.put("x!/desc", new DescService(descriptions));
            _services.put("x!/list", new ListService(_services));
        }
        _services.put("x!/ping", new PingService(wac));
    }

    public void destroy() {
        _logger.info("Terminating service dispatcher: " + _application);
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        while (!_manageables.empty()) {
            ObjectName on = _manageables.pop();
            _logger.info("unregistering MBean " + on);
            try {
                mbs.unregisterMBean(on);
            } catch (Exception x) {
                _logger.log(Level.WARNING, on.toString());
            }
        }
        while (!_plca.empty()) {
            List<PlatformLifeCycleAwareDef> plcas = _plca.pop();
            for (PlatformLifeCycleAwareDef plca: plcas) {
                _logger.info("terminate PlatformLifeCycleAware: " + plca.module + '/' + plca.bean.getClass().getName());
                plca.bean.terminate(_application, plca.module);
            }
        }

        // finally, manually deregisters JDBC driver, which prevents Tomcat 7 from complaining about memory leaks
        Enumeration<java.sql.Driver> drivers = java.sql.DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            java.sql.Driver driver = drivers.nextElement();
            try {
                java.sql.DriverManager.deregisterDriver(driver);
                _logger.info("Deregistering jdbc driver: " + driver);
            } catch (java.sql.SQLException x) {
                _logger.log(Level.WARNING, "Error deregistering driver " + driver, x);
            }
        }
    }

    /**
     * Dispatcher entry point
     */
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String id;

        if (req.getParameterValues(Service.REQUEST_TARGET_PATH) != null) {
            id = req.getParameterValues(Service.REQUEST_TARGET_PATH)[0];
        } else {
            Matcher m = URI_REGEX.matcher(req.getRequestURI());
            if (m.matches()) {
                id = m.group(1);
            } else {
                _logger.warning("Request not recognized: " + req.getRequestURI());
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        Service service = _services.get(id);
        if (service == null) {
            _logger.warning("Request not recognized: " + req.getRequestURI());
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        List<File> upload = new ArrayList<File>();
        DataBinder binder = new DataBinder();

        try {
            if (ServletFileUpload.isMultipartContent(req)) {
                try {
                    FileItemIterator it = new ServletFileUpload().getItemIterator(req);
                    while (it.hasNext()) {
                        FileItemStream item = it.next();
                        String name = item.getFieldName();
                        InputStream stream = item.openStream();
                        if (item.isFormField()) {
                            binder.put(name, Streams.asString(stream));
                        } else {
                            // File field with file name in item.getName()
                            String original = item.getName();
                            int dot = original.lastIndexOf('.');
                            // store the file in a temporary place
                            File file = File.createTempFile("xillium", dot > 0 ? original.substring(dot) : null, TEMPORARY);
                            OutputStream out = new FileOutputStream(file);
                            byte[] buffer = new byte[1024*1024];
                            int length;
                            while ((length = stream.read(buffer)) >= 0) out.write(buffer, 0, length);
                            out.close();
                            binder.put(name, original);
                            binder.put(name + ":path", file.getAbsolutePath());
                            upload.add(file);
                        }
                    }
                } catch (FileUploadException x) {
                    throw new RuntimeException("Failed to parse multipart content", x);
                }
            } else {
                Enumeration<String> en = req.getParameterNames();
                while (en.hasMoreElements()) {
                    String name = en.nextElement();
                    String[] values = req.getParameterValues(name);
                    if (values.length == 1) {
                        binder.put(name, values[0]);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < values.length; ++i) {
                            sb.append('{').append(values[i]).append('}');
                        }
                        binder.put(name, sb.toString());
                    }
                }
            }

            // auto-parameters
            binder.put(Service.REQUEST_CLIENT_ADDR, req.getRemoteAddr());
            binder.put(Service.REQUEST_CLIENT_PORT, String.valueOf(req.getRemotePort()));
            binder.put(Service.REQUEST_SERVER_PORT, String.valueOf(req.getServerPort()));
            binder.put(Service.REQUEST_HTTP_METHOD, req.getMethod());
            binder.put(Service.REQUEST_SERVER_PATH, _application);
            binder.put(Service.REQUEST_TARGET_PATH, id);
            binder.putNamedObject(Service.REQUEST_HTTP_COOKIE, req.getCookies());
            if (req.isSecure()) binder.put(Service.REQUEST_HTTP_SECURE, Service.REQUEST_HTTP_SECURE);

            if (id.endsWith(".html")) {
                // TODO provide a default, error reporting page template
            }

            // pre-service filter

            if (service instanceof Service.Extended) {
                try {
                    ((Service.Extended)service).filtrate(binder);
                } catch (AuthenticationRequiredException x) {
                    if (binder.get(Service.REQUEST_HTTP_STATUS) != null) {
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    } else {
                        throw x;
                    }
                } catch (AuthorizationException x) {
                    if (binder.get(Service.REQUEST_HTTP_STATUS) != null) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    } else {
                        throw x;
                    }
                }
            }

            // authorization

            if (service instanceof Service.Secured) {
                try {
                    ((Service.Secured)service).authorize(id, binder, _persistence);
                } catch (AuthenticationRequiredException x) {
                    if (binder.get(Service.REQUEST_HTTP_STATUS) != null) {
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    } else {
                        throw x;
                    }
                } catch (AuthorizationException x) {
                    if (binder.get(Service.REQUEST_HTTP_STATUS) != null) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    } else {
                        throw x;
                    }
                }
            }

            binder = service.run(binder, _dict, _persistence);

            // post-service filter

            if (service instanceof Service.Extended) {
                try { ((Service.Extended)service).successful(binder); } catch (Throwable t) { _logger.log(Level.WARNING, "successful()", t); }
            }

            // post-service action (deprecated)
            try {
                Runnable task = (Runnable)binder.getNamedObject(Service.SERVICE_POST_ACTION);
                if (task != null) {
                    task.run();
                }
            } catch (Throwable t) {
                _logger.warning("In post-service processing caught " + t.getClass() + ": " + t.getMessage());
            }
        } catch (org.springframework.transaction.TransactionException x) {
            String message = Throwables.getFirstMessage(x);
            Matcher matcher = SQL_CONSTRAINT.matcher(message);
            if (matcher.find()) {
                message = CrudConfiguration.icve.get(matcher.group(1));
                if (message == null) {
                    message = matcher.group(1);
                }
            }
            serviceAborted(binder, id, service, x, message);
        } catch (Throwable x) {
            serviceAborted(binder, id, service, x, Throwables.getFirstMessage(x));
        } finally {
            // post-service filter
            if (service instanceof Service.Extended) {
                try { ((Service.Extended)service).complete(binder); } catch (Throwable t) { _logger.log(Level.WARNING, "complete()", t); }
            }

            res.setHeader("Access-Control-Allow-Headers", "origin,x-prototype-version,x-requested-with,accept");
            res.setHeader("Access-Control-Allow-Origin", "*");

            try {
                // HTTP headers
                @SuppressWarnings("unchecked")
                Map<String, String> headers = binder.getNamedObject(Service.SERVICE_HTTP_HEADER, Map.class);
                if (headers != null) {
                    try {
                        for (Map.Entry<String, String> e: headers.entrySet()) {
                            res.setHeader(e.getKey(), e.getValue());
                        }
                    } catch (Exception x) {}
                }

                // return status only?
                String status = binder.get(Service.SERVICE_HTTP_STATUS);
                if (status != null) {
                    try { res.setStatus(Integer.parseInt(status)); } catch (Exception x) {}
                } else {
                    String page = binder.get(Service.SERVICE_PAGE_TARGET);

                    if (page == null) {
                        binder.clearAutoValues();
                        res.setContentType("application/json;charset=utf-8");
                        String json = binder.get(Service.SERVICE_JSON_TUNNEL);

                        if (json == null) {
    /*
                            JSONBuilder jb = new JSONBuilder(binder.estimateMaximumBytes()).append('{');

                            jb.quote("params").append(":{ ");
                            Iterator<String> it = binder.keySet().iterator();
                            while (it.hasNext()) {
                                String key = it.next();
                                String val = binder.get(key);
                                if (val == null) {
                                    jb.quote(key).append(":null");
                                } else if (val.startsWith("json:")) {
                                    jb.quote(key).append(':').append(val.substring(5));
                                } else {
                                    jb.serialize(key, val);
                                }
                                jb.append(',');
                            }
                            jb.replaceLast('}').append(',');

                            jb.quote("tables").append(":{ ");
                            Set<String> rsets = binder.getResultSetNames();
                            it = rsets.iterator();
                            while (it.hasNext()) {
                                String name = it.next();
                                jb.quote(name).append(":");
                                binder.getResultSet(name).toJSON(jb);
                                jb.append(',');
                            }
                            jb.replaceLast('}');

                            jb.append('}');

                            json = jb.toString();
    */
                            json = binder.toJSON();
                        }

                        res.getWriter().append(json).flush();
                    } else {
                        _logger.fine("\t=> " + getServletContext().getResource(page));
                        req.setAttribute(Service.SERVICE_DATA_BINDER, binder);
                        getServletContext().getRequestDispatcher(page).include(req, res);
                    }
                }
            } finally {
                for (File tmp: upload) {
                    try { tmp.delete(); } catch (Exception x) {}
                }
            }
        }
    }

    private ModuleSorter.Sorted sortServiceModules() throws ServletException {
        ModuleSorter sorter = new ModuleSorter();
        ServletContext context = getServletContext();

        try {
            Set<String> jars = context.getResourcePaths("/WEB-INF/lib/");
            _logger.config("There are " + jars.size() + " resource paths");
            for (String jar : jars) {
                try {
                    //_logger.info("... " + jar);
                    JarInputStream jis = new JarInputStream(context.getResourceAsStream(jar));
                    try {
                        String name = jis.getManifest().getMainAttributes().getValue(MODULE_NAME);
                        if (name != null) {
                            sorter.add(new ModuleSorter.Entry(name, jis.getManifest().getMainAttributes().getValue(MODULE_BASE), jar));
                        }
                    } finally {
                        jis.close();
                    }
                } catch (IOException x) {
                    // ignore this jar
                    _logger.log(Level.WARNING, "Error during jar inspection, ignored", x);
                }
            }
        } catch (Exception x) {
            throw new ServletException("Failed to sort service modules", x);
        }

        return sorter.sort();
    }

    private ApplicationContext scanServiceModules(Iterator<ModuleSorter.Entry> it, ApplicationContext wac, Map<String, String> descs, List<PlatformLifeCycleAwareDef> plcas) throws ServletException {
        ServletContext context = getServletContext();

        boolean isSpecial = plcas == null;
        if (isSpecial) {
            plcas = new ArrayList<PlatformLifeCycleAwareDef>();
        }

        try {
            BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory(ValidationConfiguration.class, _dict);
            XMLBeanAssembler assembler = new XMLBeanAssembler(factory);

            while (it.hasNext()) {
                ModuleSorter.Entry module = it.next();
                try {
                    JarInputStream jis = new JarInputStream(context.getResourceAsStream(module.path));
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
                                serviceConfiguration = new ByteArrayInputStream(Arrays.read(jis));
                            } else if (_persistence != null && jarname.startsWith(STORAGE_PREFIX) && jarname.endsWith(".xml")) {
                                _logger.config("StorageConfiguration:" + module.path + ":" + jarname);
                                assembler.build(new ByteArrayInputStream(Arrays.read(jis)));
                            } else if (VALIDATION_DIC.equals(jarname)) {
                                _logger.config("ValidationDictionary:" + module.path + ":" + jarname);
                                assembler.build(new ByteArrayInputStream(Arrays.read(jis)));
                            } else if (jarname.startsWith(XILLIUM_PREFIX) && jarname.endsWith(".xml")) {
                                _logger.config("ApplicationResources:" + module.path + ":" + jarname);
                                assembler.build(new ByteArrayInputStream(Arrays.read(jis)));
                            }
                        }
                        if (serviceConfiguration != null) {
                            if (isSpecial) {
                                wac = loadServiceModule(wac, domain, module.name, serviceConfiguration, descs, plcas);
                            } else {
                                loadServiceModule(wac, domain, module.name, serviceConfiguration, descs, plcas);
                            }
                        }
                    } finally {
                        jis.close();
                    }
                    if (isSpecial) {
                        for (PlatformLifeCycleAwareDef plca: plcas) {
                            _logger.config("Configuring SPECIAL PlatformLifeCycleAware " + plca.bean.getClass().getName());
                            plca.bean.configure(_application, module.name);
                        }

                        for (PlatformLifeCycleAwareDef plca: plcas) {
                            _logger.config("Initalizing SPECIAL PlatformLifeCycleAware " + plca.bean.getClass().getName());
                            plca.bean.initialize(_application, module.name);
                        }
                        //plcas.clear();
                        _plca.push(plcas);
                        plcas = new ArrayList<PlatformLifeCycleAwareDef>();
                    }
                } catch (IOException x) {
                    // ignore this jar
                    _logger.log(Level.WARNING, "Error during jar inspection, ignored", x);
                }
            }
        } catch (Exception x) {
            throw new ServletException("Failed to construct an XMLBeanAssembler", x);
        }

        _logger.config("Done with service modules scanning (" + (isSpecial ? "SPECIAL" : "REGULAR") + ')');
        return wac;
    }

    @SuppressWarnings("unchecked")
    private ApplicationContext loadServiceModule(ApplicationContext wac, String domain, String name, InputStream stream, Map<String, String> desc, List<PlatformLifeCycleAwareDef> plcas) {
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
                desc.put(fullname, "json:" + DataObject.Util.describe((Class<? extends DataObject>)request));
            } catch (ClassCastException x) {
                try {
                    Class<?> request = Class.forName(gac.getBeanDefinition(id).getBeanClassName()+"$Request");
                    if (DataObject.class.isAssignableFrom(request)) {
                        _logger.config("Service '" + fullname + "' request description captured: " + request.getName());
                        desc.put(fullname, "json:" + DataObject.Util.describe((Class<? extends DataObject>)request));
                    } else {
                        _logger.warning("Service '" + fullname + "' defines a Request type that is not a DataObject");
                        desc.put(fullname, "json:{}");
                    }
                } catch (Exception t) {
                    _logger.warning("Service '" + fullname + "' does not expose its request structure" + t.getClass());
                    desc.put(fullname, "json:{}");
                }
            }

            _logger.config("Service '" + fullname + "' class=" + gac.getBean(id).getClass().getName());
            _services.put(fullname, (Service)gac.getBean(id));
        }

        for (String id: gac.getBeanNamesForType(PlatformLifeCycleAware.class)) {
            plcas.add(new PlatformLifeCycleAwareDef((PlatformLifeCycleAware)gac.getBean(id), name));
        }

        // Manageable object registration: objects are registered under "bean-id/context-path"

        String contextPath = getServletContext().getContextPath();
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

    private static void serviceAborted(DataBinder binder, String id, Service service, Throwable x, String message) {
        binder.put(Service.FAILURE_MESSAGE, message);

        // post-service exception handler
        if (service instanceof Service.Extended) {
            try { ((Service.Extended)service).aborted(binder, x); } catch (Throwable t) { _logger.log(Level.WARNING, "aborted()", t); }
        }

        String sst = binder.get(Service.SERVICE_STACK_TRACE);
        String pst = System.getProperty("xillium.service.PrintStackTrace");
        if (sst != null || pst != null) {
            CharArrayWriter sw = new CharArrayWriter();
            x.printStackTrace(new PrintWriter(sw));
            String stack = sw.toString();
            if (sst != null) binder.put(Service.FAILURE_STACK, stack);
            if (pst != null) _logger.warning(x.getClass().getSimpleName() + " caught in (" + id + "): " + message + '\n' + stack);
        }
    }

    private static final long serialVersionUID = 1L;
}
