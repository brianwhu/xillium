package org.xillium.core.intrinsic;

import java.io.*;
import java.util.Properties;
import java.util.regex.Pattern;
import org.xillium.data.*;
import org.xillium.core.*;
import org.xillium.data.validation.*;


/**
 * Service 'x!/ping' implements a signaling mechanism that allows dynamic modification to the Service Platform.
 */
public class PingService extends SecuredService {
    private static final Pattern SYSTEM_PROPERTY_REALM = Pattern.compile("java\\..*|os\\..*|user\\..*|file\\..*|path\\..*|xillium.system\\..*");

    public static enum Signal {
        SystemProperties    // reloads Java system properties from a local properties file
    }

    public static class Request implements DataObject {
        public Signal signal;
        public boolean verbose;
    }

    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
        try {
            Request request = dict.collect(new Request(), binder);
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
                if (request.verbose && isLocal(binder)) {
                    for (String name: System.getProperties().stringPropertyNames()) {
                        binder.put(name, System.getProperty(name));
                    }
                }
                break;
            default:
                break;
            }
        } catch (Exception x) {
            throw new ServiceException(x.getMessage(), x);
        } finally {
            binder.put("REQUEST_CLIENT_ADDR", binder.get(Service.REQUEST_CLIENT_ADDR));
        }
        return binder;
    }

    private static boolean isLocal(DataBinder binder) {
        return "127.0.0.1".equals(binder.get(Service.REQUEST_CLIENT_ADDR));
    }
}
