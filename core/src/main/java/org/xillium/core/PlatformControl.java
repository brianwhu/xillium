package org.xillium.core;

import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.xillium.core.management.Reloadable;


/**
 * A JMX bean that controls the service platform life cycle
 */
@lombok.extern.log4j.Log4j2
public class PlatformControl implements Reloadable {
    private static ServicePlatform _platform;
    private static XmlWebApplicationContext _root;
    private static ClassLoader _cloader;

    private ClassPathXmlApplicationContext _context;

    /**
     * To be called by ServicePlatform when detected in the top-level application context.
     */
    PlatformControl bind(ServicePlatform p, XmlWebApplicationContext c, ClassLoader l) {
        _platform = p;
        _root = c;
        _cloader = l;
        return this;
    }

    /**
     * Whether this control is set to reload the service platform automatically.
     */
    public boolean isAutomatic() {
        return false;
    }

    @Override
    public synchronized String reload() {
        if (_context != null) {
            return null;
        } else {
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(_cloader);
                _root.refresh();
                _context = new ClassPathXmlApplicationContext("applicationContext.xml");
                _context.setParent(_root);
                _context.start();
                _platform.realize(_root, _context);
                return null;
            } catch (Exception x) {
                _log.warn(x.getMessage(), x);
                return x.getMessage();
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }

    @Override
    public synchronized String unload() {
        if (_context == null) {
            return null;
        } else {
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(_cloader);
                _platform.destroy();
                _context = null;
                return null;
            } catch (Exception x) {
                _log.warn(x.getMessage(), x);
                return x.getMessage();
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }
}
