package org.xillium.core.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;
import javax.servlet.*;
import org.xillium.base.Functor;


/**
 * This class represents the metadata associated with a Xillium application module.
 */
public class ServiceModule {
    private static final String DOMAIN_NAME = "Xillium-Domain-Name";
    private static final String MODULE_NAME = "Xillium-Module-Name";
    private static final String SIMPLE_NAME = "Xillium-Simple-Name";
    private static final String MODULE_BASE = "Xillium-Module-Base";

    /**
     * the domain name
     */
    public final String domain, name, simple, base, path;

    /**
     * Constructs a ServiceModule.
     *
     * @param d the module's domain name
     * @param n the module's full name
     * @param s the module's simple name
     * @param b the full name of the base module, if any
     * @param p the path to the jar that contains the module
     */
    public ServiceModule(String d, String n, String s, String b, String p) {
        domain = d;
        name = n;
        simple = s;
        base = b;
        path = p;
    }

    /**
     * Reports whether this is a special module.
     *
     * @return whether this is a special module
     */
    public boolean isSpecial() {
        return base != null && base.length() > 0;
    }

    @Override
    public String toString() {
        return name + ':' + base + ':' + path;
    }

    /**
     * Scans a collection of jar files, passing recognized Xillium application modules as ServiceModule objects to
     * a collector.
     *
     * @param c a ServletContext
     * @param jars a collection of file or URL paths
     * @param collector a Functor that is to be invoked for each recognized Xillium application module
     * @param logger a Logger that can be used to log warning messages
     */
    public static void scan(ServletContext c, Set<String> jars, Functor<Void, ServiceModule>collector, Logger logger) {
        for (String j : jars) {
            logger.config("... " + j);
            try (JarInputStream jis = new JarInputStream(j.startsWith("/") ? c.getResourceAsStream(j) : new URL(j).openStream())) {
                Attributes attrs = jis.getManifest().getMainAttributes();
                String d = attrs.getValue(DOMAIN_NAME), n = attrs.getValue(MODULE_NAME), s = attrs.getValue(SIMPLE_NAME);
                if (d != null && n != null && s != null) {
                    collector.invoke(new ServiceModule(d, n, s, attrs.getValue(MODULE_BASE), j));
                }
            } catch (IOException x) {
                logger.log(Level.WARNING, "Unexpected error during jar inspection, ignored", x);
            } catch (Exception x) {
                logger.config("Unknown resource ignored");
            }
        }
    }

}
