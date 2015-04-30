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
     *
     * @param reader a Reader
     * @throws IOException if any I/O errors occur
     */
    public XilliumProperties(Reader reader) throws IOException {
        load(reader);
    }

    /**
     * Sets(adds) a property.
     *
     * @param property the property to add
     */
    public void setXilliumProperty(XilliumProperty property) {
        setProperty(property.key, property.value);
    }

    private static final long serialVersionUID = 7985795077278228893L;
}
