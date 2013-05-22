package org.xillium.data.persistence.xml;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;
//import java.util.regex.*;
//import java.lang.reflect.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xillium.base.etc.S;


/**
 * An XML to RDBM processor
 */
public class TableUpdator extends DefaultHandler {
    private static final Logger _logger = Logger.getLogger(TableUpdator.class.getName());
    private static final String SAX_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

    private final Map<String, String> _row = new HashMap<String, String>();
    private final StringBuffer _chars = new StringBuffer();
    private final SAXParser _parser;
    private final Processor _processor;
    private String _name, _type;

    public static interface Processor {
        public void process(String type, Map<String, String> data);
    }

    public TableUpdator(Processor processor) throws ParserConfigurationException, SAXException {
        SAXParserFactory pfactory = SAXParserFactory.newInstance();
        pfactory.setFeature(SAX_NAMESPACE_PREFIXES, true);
        pfactory.setNamespaceAware(true);
        //pfactory.setValidating(true);
        _parser = pfactory.newSAXParser();
        _processor = processor;
    }

    public void process(String file) throws ParserConfigurationException, SAXException, IOException {
        _parser.parse(new File(file), this);
    }

    public void process(InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        _parser.parse(stream, this);
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
        //_logger.fine("startDocument");
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
        //_logger.fine("endDocument");
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
        _logger.fine(S.fine(_logger) ?
                    "Consider element " + l +
                  "\n             uri " + uri +
                  "\n               q " + q : null);

        if (l.equals(ResultSetStreamer.ROW)) {
            _type = a.getValue(ResultSetStreamer.TYPE);
        } else if (l.equals(ResultSetStreamer.COLUMN)) {
            _name = a.getValue(ResultSetStreamer.NAME);
            if (_name == null) {
                throw new IllegalStateException("Column specification without name");
            }
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
        if (l.equals(ResultSetStreamer.ROW)) {
            _processor.process(_type, _row);
            _row.clear();
        } else if (l.equals(ResultSetStreamer.COLUMN)) {
            if (_chars.length() > 0) {
                _row.put(_name, _chars.toString());
                _chars.setLength(0);
            } else {
                _row.put(_name, "");
            }
        }
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
        if (_chars.length() > 0) {
            _chars.append(' ');
        }
        _chars.append(ch, start, len);
    }

/*
    private static final Pattern PROCESSING_INSTRUCTION = Pattern.compile("(@?[\\w_]+) *= *\"([^\"]+)\"");
    public void processingInstruction(String target, String data) throws SAXException {
        _logger.fine("Processing Instruction " + target);
        _logger.fine("Processing Instruction data: " + data);
        if (target.equals("assemble")) {
            if (!_stack.isEmpty()) {
                ElementInfo element = _stack.get(_stack.size()-1);
                Matcher matcher = PROCESSING_INSTRUCTION.matcher(data);
                while (matcher.find()) {
                    if (matcher.groupCount() == 2) {
                        String name = matcher.group(1);
                        if (name.charAt(0) == '@') {
                            element.inst.put(name, matcher.group(2));
                        } else {
                            element.args.add(guessUntypedValue(name, matcher.group(2)));
                        }
                        _logger.fine("Processing Instruction for " + element.data.getClass() +
                                   "\n\ttarget = " + target +
                                   "\n\t" + name + "=" + matcher.group(2));
                    }
                }
            }
        }
    }
*/

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
