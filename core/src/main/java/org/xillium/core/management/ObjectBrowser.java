package org.xillium.core.management;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.Collection;
import java.util.ArrayList;
import java.lang.management.ManagementFactory;
import javax.management.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.core.*;


/**
 * Service listing.
 */
public class ObjectBrowser extends SecuredService {

    public static enum Command {
        GET_DOMAINS,
        GET_OBJECTS,
        INFO,
        GET,
        SET,
        INVOKE
    }

    public static class Request implements DataObject {
        @required public String function;
        public String object;
        public String attribute;
        public String operation;
        public String value;
    }

    public DataBinder run(DataBinder binder, Dictionary dict, Persistence persist) throws ServiceException {
        try {
            Request request = dict.collect(new Request(), binder);
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            switch (Enum.valueOf(Command.class, request.function)) {
            case GET_DOMAINS:
                String domains[] = mbs.getDomains();
                Arrays.sort(domains);
                binder.putResultSet("domains", toCachedResultSet(Arrays.asList(domains), "-"));
                binder.put("default", mbs.getDefaultDomain());
                break;
            case GET_OBJECTS:
                TreeSet<ObjectName> names = new TreeSet<ObjectName>(mbs.queryNames(request.object != null ? new ObjectName(request.object) : null, null));
                binder.put("count", String.valueOf(names.size()));
                binder.putResultSet("objects", toCachedResultSet(names, "domain", "canonicalName"));
                break;
            case INFO: {
                MBeanInfo info = mbs.getMBeanInfo(new ObjectName(request.object));
                binder.put("className", info.getClassName());
                binder.put("description", info.getDescription());
                MBeanAttributeInfo[] attrs = info.getAttributes();
                binder.putResultSet("attributes", toCachedResultSet(Arrays.asList(attrs), "name", "description", "type"));
                MBeanOperationInfo[] opers = info.getOperations();
                binder.putResultSet("operations", toCachedResultSet(Arrays.asList(opers), "name", "description", "returnType", "signature"));
                }
                break;
            case GET:
                if (request.object != null && request.attribute != null) {
                    Object value = mbs.getAttribute(new ObjectName(request.object), request.attribute);
                    binder.put("value", value != null ? value.toString() : null);
                } else {
                    throw new Exception("MissingObjectPropertyName");
                }
                break;
            case SET:
                break;
            case INVOKE: {
                MBeanInfo info = mbs.getMBeanInfo(new ObjectName(request.object));
                MBeanOperationInfo[] opers = info.getOperations();
                }
                break;
            }
        } catch (Exception x) {
            throw new ServiceException(x.getMessage(), x);
        }
        return binder;

        //binder.put("services", new JSONBuilder(keys.size()*16).append("json:").serialize(keys).toString());
        //return binder;
    }

    private <T> CachedResultSet toCachedResultSet(Collection<T> collection, String... columns) throws Exception {
        ArrayList<Object[]> rows = new ArrayList<Object[]>();
        for (T object: collection) {
            Object[] row = new Object[columns.length];
            for (int i = 0; i < columns.length; ++i) {
                Object value = columns[i].equals("-") ? object : Beans.invoke(object, "get"+Strings.capitalize(columns[i]));
                if (value != null) {
                    if (value.getClass().isArray()) {
                        row[i] = org.xillium.base.etc.Arrays.join(value, ';');
                    } else {
                        row[i] = value.toString();
                    }
                } else {
                    row[i] = null;
                }
            }
            rows.add(row);
        }
        return new CachedResultSet(columns, rows);
    }
}
