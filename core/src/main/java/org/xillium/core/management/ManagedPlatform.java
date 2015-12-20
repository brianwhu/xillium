package org.xillium.core.management;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.*;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.*;
import java.util.jar.*;
import java.util.regex.Pattern;
import javax.management.*;
import javax.script.*;
import javax.servlet.*;
import org.xillium.base.beans.Throwables;
import org.xillium.base.util.Pair;
import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.xillium.data.validation.*;
import org.xillium.core.*;
import org.xillium.core.util.*;


/**
 * Managed platform
 */
public abstract class ManagedPlatform extends ManagementService implements ServletContextListener {
    private static final Logger _logger = Logger.getLogger(ManagedPlatform.class.getName());

    protected static final String DOMAIN_NAME = "Xillium-Domain-Name";
    protected static final String MODULE_NAME = "Xillium-Module-Name";
    protected static final String SIMPLE_NAME = "Xillium-Simple-Name";
    protected static final String MODULE_BASE = "Xillium-Module-Base";

    public static final String INSTANCE = "x!/mgmt";

    protected static final Map<String, Pair<Service, Persistence>> _registry = new HashMap<>();
    protected final Map<String, Persistence> _persistences = new HashMap<>();
    protected ServletContext _context;
    protected String _application;

    public static enum Signal {
        SystemProperties,
        SystemDiagnosis,
        ObjectManagement,
        PersistenceCheck,
    }

    public static class Request implements DataObject {
        public Signal signal;
        public String parameter;
        public boolean verbose;
    }

    /**
     * Initializes the servlet context.
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        _registry.put(INSTANCE, new Pair<Service, Persistence>(this, null));

        _context = event.getServletContext();
        _application = _context.getContextPath();
        _logger.config("application: " + _application);
        if (_application.charAt(0) == '/') _application = _application.substring(1);

        try { ManagementFactory.getPlatformMBeanServer().setAttribute(
            new ObjectName("Catalina:host=localhost,name=AccessLogValve,type=Valve"), new Attribute("condition", "intrinsic")
        ); } catch (Exception x) {}
    }

    /**
     * Returns the name of the platform.
     */
    public String getName() {
        return _application;
    }

    /*#
     * Discovers service modules from a local directory. The current thread's context class loader is updated to incorporate
     * all discovered JARs.
     *
     * @param path - a directory in the local file system where extension modules can be found
     * @return a Set of URLs as strings
     */
    protected Set<String> discover(String path) {
        Set<String> modules = new HashSet<String>();

        File root;
        if (path != null && (root = new File(path)).isDirectory()) {
            try {
                _logger.info("Attempting to discover service modules under " + root.getCanonicalPath());
                List<URL> urls = new ArrayList<URL>();
                discover(urls, modules, root, new FileFilter(){ public boolean accept(File f){ return f.isDirectory() || f.getName().endsWith(".jar"); }});
                if (urls.size() > 0) {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    if (loader instanceof ServiceModuleClassLoader) {
                        ((ServiceModuleClassLoader)loader).incorporate(urls.toArray(new URL[urls.size()]));
                    } else {
                        Thread.currentThread().setContextClassLoader(new ServiceModuleClassLoader(urls.toArray(new URL[urls.size()]), loader));
                    }
                }
            } catch (Exception x) {
                throw new RuntimeException("FailureInLoadingExtensions", x);
            }
        }

        return modules;
    }

    private void discover(List<URL> urls, Collection<String> modules, File directory, FileFilter filter) throws Exception {
        for (File file: directory.listFiles(filter)) {
            if (file.isDirectory()) {
                discover(urls, modules, file, filter);
            } else {
                URL url = new URL("file", null, file.getCanonicalPath());
                urls.add(url);
                modules.add(url.toString());
                _logger.info("discovered JAR " + url);
            }
        }
    }

    /*#
     * Detects service modules in a given set of JARs, and returns them in a sorted list.
     */
    protected ModuleSorter.Sorted sort(ServletContext context, Set<String> jars) {
        ModuleSorter sorter = new ModuleSorter();

        try {
            _logger.config("There are " + jars.size() + " resource paths");

            for (String jar : jars) {
                try {
                    _logger.config("... " + jar);
                    JarInputStream jis = new JarInputStream(jar.startsWith("/") ? context.getResourceAsStream(jar) : new URL(jar).openStream());
                    try {
                        Attributes attrs = jis.getManifest().getMainAttributes();
                        String d = attrs.getValue(DOMAIN_NAME), n = attrs.getValue(MODULE_NAME), s = attrs.getValue(SIMPLE_NAME);
                        if (d != null && n != null && s != null) {
                            sorter.add(new ModuleSorter.Entry(d, n, s, attrs.getValue(MODULE_BASE), jar));
                        }
                    } finally {
                        jis.close();
                    }
                } catch (IOException x) {
                    // report this failure and move on
                    _logger.log(Level.WARNING, "Error during jar inspection, ignored", x);
                } catch (Exception x) {
                    // ignore this jar
                    _logger.config("Unknown resource ignored");
                }
            }
        } catch (Exception x) {
            throw new RuntimeException("Failed to sort service modules", x);
        }

        return sorter.sort();
    }


    @Override
    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
        if (isLocal(binder)) {
            String redirect = binder.remove(REDIRECT);
            if (redirect != null && redirect.length() > 0) {
                RemoteService.call(
                    redirect, binder.get(REQUEST_TARGET_PATH), binder, REQUEST_CLIENT_PHYS+'='+binder.get(REQUEST_CLIENT_PHYS)
                ).store(binder);
            } else try {
                final Request request = dict.collect(new Request(), binder);
                if (request.parameter != null) request.parameter = URLDecoder.decode(request.parameter, "UTF-8");
                binder.put("application", _application);

                if (request.signal != null) switch (request.signal) {
                case SystemProperties:
                    String path = System.getProperty("xillium.system.PropertiesFile");
                    if (path != null) {
                        binder.put("xillium.system.PropertiesFile", path);
                        File file = new File(path);
                        if (file.canRead()) {
                            Reader is = new InputStreamReader(new FileInputStream(file), "UTF-8");
                            try {
                                int updates = 0;
                                Properties properties = new Properties();
                                properties.load(is);
                                for (String name: properties.stringPropertyNames()) {
                                    if (SYSTEM_PROPERTY_REALM.matcher(name).matches()) continue; // certain properties are off limit
                                    String value = properties.getProperty(name);
                                    if (value != null && value.length() > 0) {
                                        System.setProperty(name, value);
                                    } else {
                                        System.clearProperty(name);
                                    }
                                    ++updates;
                                }
                                binder.put("updates", String.valueOf(updates));
                            } finally {
                                is.close();
                            }
                        }
                    }
                    if (request.verbose) {
                        for (String name: System.getProperties().stringPropertyNames()) {
                            binder.put(name, System.getProperty(name));
                        }
                    }
                    break;
                case SystemDiagnosis:
                    if (request.parameter != null) {
                        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
                        engine.put("os", new SystemCommander(binder, _registry));
                        engine.eval(request.parameter);
                    }
                    break;
                case ObjectManagement:
                    if (request.parameter != null) {
                        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
                        engine.put("jmx", new JMXCommander(binder));
                        engine.eval(request.parameter);
                    }
                    break;
                case PersistenceCheck:
                    if (request.parameter != null) {
                        final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
                        engine.put("db", new PersistenceManager(binder, _persistences));
                        //persist.doReadWrite(null, new Persistence.Task<Void, Void>() {
                            //public Void run(Void v, Persistence p) throws Exception {
                                engine.eval(request.parameter);
                                //return null;
                            //}
                        //});
                    }
                    break;
                default:
                    break;
                }
            } catch (Exception x) {
                throw new ServiceException(x.getMessage(), x);
            } finally {
                binder.put("REQUEST_CLIENT_ADDR", binder.get(REQUEST_CLIENT_ADDR));
            }
        } else {
            binder.put("echo", String.valueOf(System.currentTimeMillis()));
        }
        binder.remove("signal");
        binder.remove("parameter");
        return binder;
    }

    private boolean isLocal(DataBinder binder) {
        String addr = binder.get(REQUEST_CLIENT_ADDR);
        if ("127.0.0.1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr)) {
            return true;
        } else {
            String realm = binder.get(REQUEST_CLIENT_PHYS);
            if (realm != null) {
                long clock = System.currentTimeMillis();
                try {
                    return sync(realm, clock);
                } catch (Exception x) {
                    try {
                        BigInteger hash = new BigInteger(Throwables.hash(x));
                        return Math.abs(
                            clock - new BigInteger(realm, Character.MAX_RADIX).modPow(BigInteger.valueOf(SYNC_DRIFTING_TOLERANCE), hash).longValue()
                        ) < SYNC_DRIFTING_TOLERANCE;
                    } catch (Exception y) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
    }

    private static final Pattern SYSTEM_PROPERTY_REALM = Pattern.compile("java\\..*|os\\..*|user\\..*|file\\..*|path\\..*|xillium.system\\..*");
    private static final String REDIRECT = "_redirect_";
}
