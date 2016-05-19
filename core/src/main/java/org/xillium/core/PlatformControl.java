package org.xillium.core;

import java.util.logging.*;
import org.springframework.context.*;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.xillium.core.management.Reloadable;


/**
 * A JMX bean that controls the service platform life cycle
 */
public class PlatformControl implements Reloadable, Lifecycle, ApplicationContextAware {
    private static final Logger _logger = Logger.getLogger(PlatformControl.class.getName());
    private static ServicePlatform _platform;
    private static XmlWebApplicationContext _root;
    private static ClassLoader _cloader;
    private static boolean _running;

    private ConfigurableApplicationContext _context;

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
     * Initializes this control.
     */
    public void initialize() {}

    @Override
    public void setApplicationContext(ApplicationContext context) {
        if (context instanceof ConfigurableApplicationContext) {
            _context = (ConfigurableApplicationContext)context;
        } else {
            _logger.log(Level.WARNING, "Non-ConfigurableApplicationContext not chainable");
        }
    }

    @Override
    public synchronized String reload() {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(_cloader);
            _root.refresh();
            if (_context != null) _context.setParent(_root);
            _platform.realize(_root, _context);
            return null;
        } catch (Exception x) {
            _logger.log(Level.WARNING, x.getMessage(), x);
            return x.getMessage();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Override
    public synchronized String unload() {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(_cloader);
            _platform.destroy();
            return null;
        } catch (Exception x) {
            _logger.log(Level.WARNING, x.getMessage(), x);
            return x.getMessage();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Override
    public synchronized boolean isRunning() {
        _logger.info("Returning " + _running);
        return _running;
    }

    @Override
    public synchronized void start() {
        _logger.info("Before reload()");
        if (_platform != null) {
            _logger.info("Calling reload()");
            reload();
            _running = true;
            _logger.info("Running = " + _running);
        }
    }

    @Override
    public synchronized void stop() {
        if (_platform != null) {
            _logger.info("Calling unload()");
            unload();
            _running = false;
            _logger.info("Running = " + _running);
        }
        _logger.info("After unload()");
    }

}
