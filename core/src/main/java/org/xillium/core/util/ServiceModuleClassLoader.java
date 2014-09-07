package org.xillium.core.util;

import java.net.URL;
import java.net.URLClassLoader;


/**
 * An extension of java.net.URLClassLoader that allows its URL list go grow dynamically
 */
public class ServiceModuleClassLoader extends URLClassLoader {
    /**
     * Constructs a ServiceModuleClassLoader.
     */
    public ServiceModuleClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Incorporates an additional set of URLs into this ClassLoader.
     */
    public void incorporate(URL[] urls) {
        for (URL url: urls) addURL(url);
    }
}
