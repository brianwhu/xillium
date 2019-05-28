package org.xillium.base.util;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.apache.logging.log4j.*;


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
            LogManager.getLogger(XML.class).log(Level.ERROR, "Failed to construct/configure a SAXParserFactory", x);
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

