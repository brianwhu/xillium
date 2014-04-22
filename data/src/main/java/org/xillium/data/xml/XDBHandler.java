package org.xillium.data.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import org.xillium.base.etc.Arrays;
import org.xillium.base.util.XML;
import org.xillium.data.*;


/**
 * A SAX handler to help assemble a DataBinder from a compliant XML document.
 */
public class XDBHandler extends DefaultHandler {
    private final StringBuilder _text = new StringBuilder();
    private final List<String> _data = new ArrayList<String>();
    private final DataBinder _binder;
    private String _name;
    private String[] _heading;
    private int _cindex;
    private int _rindex;

    public XDBHandler(DataBinder binder) {
        _binder = binder;
    }

    /**
     * Receive notification of the beginning of the document.
     */
    @Override
    public void startDocument() {
    }

    /**
     * Receive notification of the end of the document.
     */
    @Override
    public void endDocument() {
    }

    /**
     * Receive notification of the start of an element.
     */
    @Override
    public void startElement(String uri, String l, String q, Attributes a) {
        try {
            XDB.Element element = Enum.valueOf(XDB.Element.class, l.toUpperCase());
            switch (element) {
            case M:
                break;

            case P:
                _name = a.getValue("name");
                break;

            case T:
                _name = a.getValue("name");
                _rindex = 0;
                break;

            case H:
                _data.clear();
                _heading = null;
                break;

            case R:
                _cindex = 0;
                break;

            case C:
                break;
            }
        } catch (Exception x) {
            throw new RuntimeException("Failed to recognize element " + q, x);
        }
    }

    /**
     * Receive notification of the end of an element.
     */
    @Override
    public void endElement(String uri, String l, String q) {
        /*
         * 1. If current element is a String, update its value from the string buffer.
         * 2. Add the element to parent.
         */
        XDB.Element element = Enum.valueOf(XDB.Element.class, l.toUpperCase());

        switch (element) {
        case M:
            break;

        case P:
            _binder.put(_name, _text.toString());
            break;

        case T:
            break;

        case H:
            _heading = _data.toArray(new String[_data.size()]);
            break;

        case R:
            ++_rindex;
            break;

        case C:
            if (_heading == null) {
                _data.add(_text.toString());
            } else {
                _binder.put(_name + '[' + _rindex + "]." + _heading[_cindex], _text.toString());
                ++_cindex;
            }
            break;
        }
        _text.setLength(0);
    }

    /**
     * Receive notification of character data inside an element.
     */
    @Override
    public void characters (char ch[], int start, int len) {
        while (len > 0 && Character.isWhitespace(ch[start])) {
            ++start;
            --len;
        }
        while (len > 0 && Character.isWhitespace(ch[start+len-1])) {
            --len;
        }
        if (_text.length() > 0) {
            _text.append(' ');
        }
        _text.append(ch, start, len);
    }

    /**
     * Receive notification of a parser warning.
     */
    @Override
    public void warning (SAXParseException e) throws SAXException {
        throw e;
    }


    /**
     * Receive notification of a recoverable parser error.
     */
    @Override
    public void error (SAXParseException e) throws SAXException {
        throw e;
    }
    /**
     * Report a fatal XML parsing error.
     */
    @Override
    public void fatalError (SAXParseException e) throws SAXException {
        throw e;
    }
}
