package org.xillium.core.util;

import java.io.*;
import java.util.List;
import java.util.Properties;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


/**
 * A class of implementations that are configured by Properties or properties files.
 */
public abstract class PropertiesConfigured {
    /**
     * Constructs a PropertiesConfigured that is to be configured later.
     */
    public PropertiesConfigured() {
    }

    /**
     * Constructs a PropertiesConfigured and configures it with given properties.
     */
    public PropertiesConfigured(Properties properties) {
        configure(properties);
    }

    /**
     * Constructs a PropertiesConfigured and configures it with a given properties
     * file, which could be on either the file system (file: location) or the class
     * path (classpath: location).
     */
    public PropertiesConfigured(String location) {
        setPropertiesLocation(location);
    }

    /**
     * Configures with a properties file, which could be on either the file system
     * (file: location) or the class path (classpath: location).
     */
    public void setPropertiesLocation(String location) {
        Properties properties = load(null, new PathMatchingResourcePatternResolver(), location);
        if (properties != null && properties.size() > 0) {
            configure(properties);
        }
    }

    /**
     * Configures with a list of properties files, which could be on either the
     * file system (file: location) or the class path (classpath: location).
     */
    public void setPropertiesLocations(List<String> locations) {
        Properties properties = null;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String location: locations) {
            properties = load(properties, resolver, location);
        }
        if (properties != null && properties.size() > 0) {
            configure(properties);
        }
    }

    /**
     * Configures with given properties.
     */
    public void setProperties(Properties properties) {
        configure(properties);
    }

    /*#
     * Applies the configuration in the Properties
     */
    protected abstract void configure(Properties properties);

    private static Properties load(Properties properties, PathMatchingResourcePatternResolver resolver, String location) {
        try {
            Reader reader = new InputStreamReader(resolver.getResource(location).getInputStream(), "UTF-8");
            try {
                if (properties == null) properties = new Properties();
                properties.load(reader);
            } finally {
                reader.close();
            }
        } catch (Exception x) {}
        return properties;
    }
}
