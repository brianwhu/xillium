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
     * Constructs a PropertiesConfigured and configures it with a given properties file.
     */
    public PropertiesConfigured(String path) {
        setPropertiesFile(path);
    }

    /**
     * Configures with a properties file, which could be on either the file system (file:) or the class path (classpath:).
     */
    public void setPropertiesFile(String path) {
        configure(load(new Properties(), new PathMatchingResourcePatternResolver(), path));
    }

    /**
     * Configures with a list of properties files, which could be on either the file system (file:) or the class path (classpath:).
     */
    public void setPropertiesFiles(List<String> paths) {
        Properties properties = new Properties();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String path: paths) {
            load(properties, resolver, path);
        }
        configure(properties);
    }

    /**
     * Configures with given properties.
     */
    public void setProperties(Properties properties) {
        configure(properties);
    }

    protected abstract void configure(Properties properties);

    private static Properties load(Properties properties, PathMatchingResourcePatternResolver resolver, String path) {
        try {
            Reader reader = new InputStreamReader(resolver.getResource(path).getInputStream(), "UTF-8");
            try {
                properties.load(reader);
            } finally {
                reader.close();
            }
        } catch (Exception x) {}
        return properties;
    }
}
