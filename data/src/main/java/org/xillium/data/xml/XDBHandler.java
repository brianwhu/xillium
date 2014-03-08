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
 * An XML to Java beans binding utility.
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
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the beginning
     * of a document (such as allocating the root node of a tree or
     * creating an output file).</p>
     *
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startDocument
     */
    public void startDocument() {
    }

    /**
     * Receive notification of the end of the document.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the end
     * of a document (such as finalising a tree or closing an output
     * file).</p>
     *
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#endDocument
     */
    public void endDocument() {
    }

    /**
     * Receive notification of the start of an element.
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the start of
     * each element (such as allocating a new tree node or writing
     * output to a file).</p>
     *
     * @param l The element type name as a local string
     * @param q The element type name as a qualified string
     * @param a The specified or defaulted attributes
     * @see org.xml.sax.ContentHandler#startElement
     */
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
     *
     * <p>By default, do nothing.  Application writers may override this
     * method in a subclass to take specific actions at the end of
     * each element (such as finalising a tree node or writing
     * output to a file).</p>
     *
     * @param name The element type name.
     * @param attributes The specified or defaulted attributes.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#endElement
     */
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
     *
     * <p>By default, do nothing.  Application writers may override this
     * method to take specific actions for each chunk of character data
     * (such as adding the data to a node or buffer, or printing it to
     * a file).</p>
     *
     * @param ch The characters.
     * @param start The start position in the character array.
     * @param len The number of characters to use from the
     *               character array.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#characters
     */
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
     *
     * <p>The default implementation does nothing.  Application writers
     * may override this method in a subclass to take specific actions
     * for each warning, such as inserting the message in a log file or
     * printing it to the console.</p>
     *
     * @param e The warning information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ErrorHandler#warning
     * @see org.xml.sax.SAXParseException
     */
    public void warning (SAXParseException e) throws SAXException {
        throw e;
    }


    /**
     * Receive notification of a recoverable parser error.
     *
     * <p>The default implementation does nothing.  Application writers
     * may override this method in a subclass to take specific actions
     * for each error, such as inserting the message in a log file or
     * printing it to the console.</p>
     *
     * @param e The warning information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ErrorHandler#warning
     * @see org.xml.sax.SAXParseException
     */
    public void error (SAXParseException e) throws SAXException {
        throw e;
    }
    /**
     * Report a fatal XML parsing error.
     *
     * <p>The default implementation throws a SAXParseException.
     * Application writers may override this method in a subclass if
     * they need to take specific actions for each fatal error (such as
     * collecting all of the errors into a single report): in any case,
     * the application must stop all regular processing when this
     * method is invoked, since the document is no longer reliable, and
     * the parser may no longer report parsing events.</p>
     *
     * @param e The error information encoded as an exception.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ErrorHandler#fatalError
     * @see org.xml.sax.SAXParseException
     */
    public void fatalError (SAXParseException e) throws SAXException {
        throw e;
    }
}
