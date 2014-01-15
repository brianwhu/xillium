package org.xillium.base.util;

import java.util.Properties;
import java.io.*;


/**
 * A slightly improved java.util.Properties that is also friendly to XML bean assembly mechanism.
 */
public class XilliumProperties extends Properties {
    /**
     * Constructs a XilliumProperties.
     */
    public XilliumProperties() {
    }

    /**
     * Constructs a XilliumProperties that reads directly from an open Reader.
     */
    public XilliumProperties(Reader reader) throws IOException {
        load(reader);
    }

    /**
     * Sets(adds) a property.
     */
    public void setXilliumProperty(XilliumProperty property) {
        setProperty(property.key, property.value);
    }

    private static final long serialVersionUID = 7985795077278228893L;
}
