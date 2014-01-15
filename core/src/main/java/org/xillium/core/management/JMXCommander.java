package org.xillium.core.management;

import java.lang.reflect.Field;
import java.lang.management.ManagementFactory;
import java.util.*;
import javax.management.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;


/**
 * JMXCommander retrieves JMX bean information into a DataBinder.
 * <ul>
 * <li>l(list) - list domains and objects</li>
 * <li>t(tell) - tell details of an object</li>
 * <li>g(get)  - get an attribute value</li>
 * <li>s(set)  - set an attribute value</li>
 * <li>a(act)  - act upon an operation</li>
 * </ul>
 */
public class JMXCommander {
    private static final String MESSAGE = "jmx.exception";

    private final MBeanServer mbs;
    private final Field fld;
    private final DataBinder bdr;
    private boolean vbs;

    public JMXCommander(DataBinder binder) {
        try {
            mbs = ManagementFactory.getPlatformMBeanServer();
            fld = JMXCommander.class.getDeclaredField("mbs");
            bdr = binder;
        } catch (Exception x) {
            throw new ManagementException(x.getMessage(), x);
        }
    }

    public JMXCommander v(boolean verbose) {
        vbs = verbose;
        return this;
    }

    public JMXCommander l() {
        try {
            String domains[] = mbs.getDomains();
            Arrays.sort(domains);
            bdr.put("default", mbs.getDefaultDomain());
            bdr.putResultSet("domains", new CachedResultSet(Arrays.asList(domains), "-"));
        } catch (Exception x) {
            bdr.put(MESSAGE, vbs ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public JMXCommander l(String selector) {
        try {
            Set<ObjectName> names = mbs.queryNames(selector != null ? new ObjectName(selector) : null, null);
            bdr.put("count", String.valueOf(names.size()));
            bdr.putResultSet("objects", new CachedResultSet(names, "domain", "canonicalName"));
        } catch (Exception x) {
            bdr.put(MESSAGE, vbs ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public JMXCommander t(String selector) {
        try {
            MBeanInfo info = mbs.getMBeanInfo(new ObjectName(selector));
            bdr.put("className", info.getClassName());
            bdr.put("description", info.getDescription());
            MBeanAttributeInfo[] attrs = info.getAttributes();
            bdr.putResultSet("attributes", new CachedResultSet(Arrays.asList(attrs), "name", "description", "type"));
            MBeanOperationInfo[] opers = info.getOperations();
            bdr.putResultSet("operations", new CachedResultSet(Arrays.asList(opers), "name", "description", "returnType", "signature"));
        } catch (Exception x) {
            bdr.put(MESSAGE, vbs ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public JMXCommander g(String selector, String attribute) {
        try {
            bdr.put("value", Beans.toString(mbs.getAttribute(new ObjectName(selector), attribute)));
        } catch (Exception x) {
            bdr.put(MESSAGE, vbs ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public JMXCommander s(String selector, String attribute, String value) {
        try {
            ObjectName name = new ObjectName(selector);
            MBeanAttributeInfo attr = null;
            for (MBeanAttributeInfo info: mbs.getMBeanInfo(name).getAttributes()) {
                if (info.getName().equals(attribute)) {
                    attr = info;
                    break;
                }
            }
            if (attr != null) {
                mbs.setAttribute(name, new Attribute(attribute, new Validator("-", Beans.boxPrimitive(Beans.classForName(attr.getType())), fld).parse(value)));
            } else {
                throw new AttributeNotFoundException(attribute);
            }
        } catch (Exception x) {
            bdr.put(MESSAGE, vbs ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }

    public JMXCommander a(String selector, String operation, String... args) {
        try {
            ObjectName name = new ObjectName(selector);
            MBeanOperationInfo oper = null;
            for (MBeanOperationInfo info: mbs.getMBeanInfo(name).getOperations()) {
                if (info.getName().equals(operation)) {
                    oper = info;
                    break;
                }
            }
            if (oper != null) {
                MBeanParameterInfo[] params = oper.getSignature();
                if (params.length != args.length) {
                    throw new IllegalArgumentException("IncorrectNumberOfArguments");
                }
                Object[] arguments = new Object[params.length];
                String[] signature = new String[params.length];
                for (int i = 0; i < params.length; ++i) {
                    signature[i] = params[i].getType();
                    arguments[i] = new Validator("-", Beans.boxPrimitive(Beans.classForName(signature[i])), fld).parse(args[i]);
                }
                bdr.put("value", Beans.toString(mbs.invoke(name, operation, arguments, signature)));
            } else {
                throw new ManagementRealmNotFoundException(operation);
            }
        } catch (Exception x) {
            bdr.put(MESSAGE, vbs ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }
}
