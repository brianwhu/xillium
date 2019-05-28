package org.xillium.core.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import javax.servlet.*;
import org.xillium.base.Functor;


/**
 * This class represents the metadata associated with a Xillium application module.
 */
@lombok.extern.log4j.Log4j2
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
     * Unpacks WAR or uber JAR to sort out packaged modules.
     */
    public static ModuleSorter.Sorted unpack() {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final ModuleSorter sorter = new ModuleSorter();
        try {
            Enumeration<URL> resources = loader.getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                String url = resources.nextElement().toString();
                try {
                    accept(loader, new URL(url.substring(0, url.lastIndexOf('!'))).toString(), sorter);
                } catch (MalformedURLException x) {/*url is the uber jar or war itself*/}
            }
        } catch (IOException x) {
            x.printStackTrace(System.out);
        }
        return sorter.sort();
    }

    /**
     * Scans a local directory for extension libraries and external modules. The current thread's context class loader is updated to incorporate
     * all discovered JARs.
     *
     * @param path - a directory in the local file system where extension libraries and external modules can be found
     * @return the sorted module list
     */
    public static ModuleSorter.Sorted scan(String path) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final ModuleSorter sorter = new ModuleSorter();

        File root;
        if (path != null && (root = new File(path)).isDirectory()) {
            try {
                _log.info("Attempting to discover service modules under " + root.getCanonicalPath());
                List<URL> urls = new ArrayList<URL>();
                // recursion down to subdirectories
                discover(loader, urls, sorter, root, f -> f.isDirectory() || f.getName().endsWith(".jar"));
                if (urls.size() > 0) {
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

        return sorter.sort();
    }

    // recursion down to subdirectories
    private static void discover(ClassLoader loader, List<URL> urls, ModuleSorter sorter, File directory, FileFilter filter) throws Exception {
        for (File file: directory.listFiles(filter)) {
            if (file.isDirectory()) {
                discover(loader, urls, sorter, file, filter);
            } else {
                URL url = new URL("file", null, file.getCanonicalPath());
                urls.add(url);
                _log.info("discovered JAR " + url);
                accept(loader, url.toString(), sorter);
            }
        }
    }

    /*!
     * Scans a jar file, adding recognized Xillium application module as a ServiceModule object to a ModuleSorter.
     *
     * @param loader a ClassLoader
     * @param jar a file path or a URL
     * @param sorter a ModuleSorter
     */
    private static void accept(ClassLoader loader, String jar, ModuleSorter sorter) {
        try (JarInputStream jis = new JarInputStream(jar.startsWith("/") ? loader.getResourceAsStream(jar) : new URL(jar).openStream())) {
            Attributes attrs = jis.getManifest().getMainAttributes();
            String d = attrs.getValue(DOMAIN_NAME), n = attrs.getValue(MODULE_NAME), s = attrs.getValue(SIMPLE_NAME);
            if (d != null && n != null && s != null) {
                sorter.add(new ServiceModule(d, n, s, attrs.getValue(MODULE_BASE), jar));
            }
        } catch (IOException x) {
            _log.warn("Unexpected error during jar inspection, ignored", x);
        } catch (Exception x) {
            _log.trace("Unknown resource ignored");
        }
    }
}
