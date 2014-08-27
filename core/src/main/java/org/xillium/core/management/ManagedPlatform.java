package org.xillium.core.management;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.*;
import java.util.regex.Pattern;
import javax.management.*;
import javax.script.*;
import javax.servlet.*;
//import org.springframework.context.ApplicationContext;
//import org.springframework.web.context.support.WebApplicationContextUtils;
import org.xillium.base.beans.Throwables;
import org.xillium.data.*;
import org.xillium.data.persistence.*;
import org.xillium.data.validation.*;
import org.xillium.core.*;
import org.xillium.core.util.RemoteService;


/**
 * Managed platform
 */
public abstract class ManagedPlatform extends ManagementService {
    private static final Logger _logger = Logger.getLogger(ManagedPlatform.class.getName());

    public static final String INSTANCE = "x!/mgmt";

    protected static final Map<String, Service> _registry = new HashMap<String, Service>();
    //protected ApplicationContext _context;
    protected String _application;
    protected Map<String, ParametricStatement> _statements;

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
        super.contextInitialized(event);
        _registry.put(INSTANCE, this);

        ServletContext context = event.getServletContext();
        _application = context.getContextPath();
        _logger.config("application: " + _application);
        if (_application.charAt(0) == '/') _application = _application.substring(1);

        //_context = WebApplicationContextUtils.getWebApplicationContext(context);

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
                        engine.put("db", new PersistenceManager(binder, _statements, persist.getDataSource()));
                        persist.doReadWrite(null, new Persistence.Task<Void, Void>() {
                            public Void run(Void v, Persistence p) throws Exception {
                                engine.eval(request.parameter);
                                return null;
                            }
                        });
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
