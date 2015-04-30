package org.xillium.base.util;

import java.util.logging.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;


/**
 * Shared XML resources.
 */
public class XML {
    private static final String SAX_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
    private static final SAXParserFactory _factory;

    static {
        SAXParserFactory factory = null;
        try {
            factory = SAXParserFactory.newInstance();
            factory.setFeature(SAX_NAMESPACE_PREFIXES, true);
            factory.setNamespaceAware(true);
        } catch (Exception x) {
            Logger.getLogger(XML.class.getName()).log(Level.SEVERE, "Failed to construct/configure a SAXParserFactory", x);
        } finally {
            _factory = factory;
        }
    }

    /**
     * Creates a new SAXParser.
     *
     * @return the SAX parser
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration
     * @throws SAXException for SAX errors
     */
    public static SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        return _factory.newSAXParser();
    }
}

