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
 */
public class JMXCommander {
    private final MBeanServer mbs;
    private final Field fld;
    private final DataBinder bdr;

    public JMXCommander(DataBinder binder) {
        try {
            mbs = ManagementFactory.getPlatformMBeanServer();
            fld = JMXCommander.class.getDeclaredField("mbs");
            bdr = binder;
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

    public JMXCommander list() {
        String domains[] = mbs.getDomains();
        Arrays.sort(domains);
        bdr.put("default", mbs.getDefaultDomain());
        bdr.putResultSet("domains", new CachedResultSet(Arrays.asList(domains), "-"));
        return this;
    }

    public JMXCommander list(String selector) throws JMException {
        Set<ObjectName> names = mbs.queryNames(selector != null ? new ObjectName(selector) : null, null);
        bdr.put("count", String.valueOf(names.size()));
        bdr.putResultSet("objects", new CachedResultSet(names, "domain", "canonicalName"));
        return this;
    }

    public JMXCommander desc(String selector) throws JMException {
        MBeanInfo info = mbs.getMBeanInfo(new ObjectName(selector));
        bdr.put("className", info.getClassName());
        bdr.put("description", info.getDescription());
        MBeanAttributeInfo[] attrs = info.getAttributes();
        bdr.putResultSet("attributes", new CachedResultSet(Arrays.asList(attrs), "name", "description", "type"));
        MBeanOperationInfo[] opers = info.getOperations();
        bdr.putResultSet("operations", new CachedResultSet(Arrays.asList(opers), "name", "description", "returnType", "signature"));
        return this;
    }

    public JMXCommander get(String selector, String attribute) throws JMException {
        if (selector != null && attribute != null) {
            bdr.put("value", String.valueOf(mbs.getAttribute(new ObjectName(selector), attribute)));
        } else {
            throw new RuntimeException("MissingObjectPropertyName");
        }
        return this;
    }

    public JMXCommander set(String selector, String attribute, String value) throws JMException, DataValidationException {
        ObjectName name = new ObjectName(selector);
        for (MBeanAttributeInfo attr: mbs.getMBeanInfo(name).getAttributes()) {
            if (attr.getName().equals(attribute)) {
                try {
                    mbs.setAttribute(name, new Attribute(attribute, new Validator("-", Beans.boxPrimitive(Beans.classForName(attr.getType())), fld).parse(value)));
                } catch (ClassNotFoundException x) {
                    throw new DataValidationException(x.getMessage(), x);
                }
                break;
            }
        }
        return this;
    }

    public JMXCommander act(String selector, String operation, String... args) throws JMException, DataValidationException {
        ObjectName name = new ObjectName(selector);
        for (MBeanOperationInfo oper: mbs.getMBeanInfo(name).getOperations()) {
            if (oper.getName().equals(operation)) {
                MBeanParameterInfo[] params = oper.getSignature();
                if (params.length != args.length) {
                    throw new RuntimeException("IncorrectNumberOfArguments");
                }
                Object[] arguments = new Object[params.length];
                String[] signature = new String[params.length];
                for (int i = 0; i < params.length; ++i) {
                    signature[i] = params[i].getType();
                    try {
                        arguments[i] = new Validator("-", Beans.boxPrimitive(Beans.classForName(signature[i])), fld).parse(args[i]);
                    } catch (ClassNotFoundException x) {
                        throw new DataValidationException(x.getMessage(), x);
                    }
                }
                bdr.put("value", String.valueOf(mbs.invoke(name, operation, arguments, signature)));
                break;
            }
        }
        return this;
    }
}
