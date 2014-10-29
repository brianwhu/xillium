package org.xillium.data.persistence.xml;

import java.sql.*;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.Callable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xillium.base.beans.Beans;
import org.xillium.base.beans.Strings;
import org.xillium.data.DataObject;
import org.xillium.data.persistence.ResultSetWorker;
import org.xillium.data.validation.Assertion;
import org.xillium.data.validation.DataValidationException;


/**
 * A ResultSetWorker implementation that writes the result set as a sql-rs XML document into a Writer.
 */
public class ResultSetStreamer implements ResultSetWorker<Writer> {
    public static final String NAMESPACE = "java://org.xillium.data.persistence.xml";
    public static final String ROOT      = "sql-rs:data";
    public static final String ROW       = "row";
    public static final String COLUMN    = "column";
    public static final String NAME      = "name";
    //public static final String TYPE      = "type";

    static class Generator<T> {
        List<T> list = new ArrayList<T>();
        Class<T> type;
        Callable<T> factory;

        Generator(Class<T> t, Callable<T> c) {
            type = t;
            factory = c;
        }

        void store(Object value) {
            list.add(type.cast(value));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Assertion> _requires = Collections.EMPTY_MAP;
    @SuppressWarnings("unchecked")
    private Map<String, Assertion> _excludes = Collections.EMPTY_MAP;
    private Generator<? extends DataObject> _generator;
    private final String _name;
    private final Writer _sink;

    public ResultSetStreamer(String name, Writer sink) {
        _name = name;
        _sink = sink;
    }

    /**
     * Requires a data column to satisfy an assertion.
     */
    public ResultSetStreamer require(String column, Assertion assertion) {
        if (_requires == Collections.EMPTY_MAP) _requires = new HashMap<String, Assertion>();
        _requires.put(column, assertion);
        return this;
    }

    /**
     * Excludes rows whose data columns satisfy an assertion.
     */
    public ResultSetStreamer exclude(String column, Assertion assertion) {
        if (_excludes == Collections.EMPTY_MAP) _excludes = new HashMap<String, Assertion>();
        _excludes.put(column, assertion);
        return this;
    }

    /**
     * Asks the streamer to collect the rows into a list of DataObjects.
     */
    public <T extends DataObject> List<T> collect(Class<T> type, Callable<T> factory) {
        Generator<T> generator = new Generator<T>(type, factory);
        _generator = generator;
        return generator.list;
    }

    /**
     * Asks the streamer to collect the rows into a list of DataObjects.
     */
    public <T extends DataObject> List<T> collect(Class<T> type) {
        return collect(type, null);
    }

	@SuppressWarnings("unchecked")
	public Writer process(ResultSet rs) throws SQLException, ParserConfigurationException, TransformerException, IOException, DataValidationException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElementNS(NAMESPACE, ROOT);
        Attr attr = doc.createAttribute(NAME);
        attr.setValue(_name);
        root.setAttributeNode(attr);

        ResultSetMetaData meta = rs.getMetaData();
        int count = meta.getColumnCount();
record: while (rs.next()) {
            Element row = doc.createElement(ROW);

            DataObject object = null;
            if (_generator != null) {
                try {
                    object = _generator.factory != null ? _generator.factory.call() : _generator.type.newInstance();
                } catch (Exception x) {
                    throw new RuntimeException(x.getMessage(), x);
                }
            }

            for (int i = 0; i < count; ++i) {
                String name = Strings.toLowerCamelCase(meta.getColumnLabel(i+1), '_');
                Object value = rs.getObject(i+1);

                try { Assertion.S.apply(_excludes.get(name), value); } catch (DataValidationException x) { continue record; }

                Element col = doc.createElement(COLUMN);
                attr = doc.createAttribute(NAME);
                attr.setValue(name);
                col.setAttributeNode(attr);
                //attr = doc.createAttribute(TYPE);
                //attr.setValue(Strings.toLowerCamelCase(meta.getColumnClassName(i+1), '_'));
                //col.setAttributeNode(attr);

                Assertion.S.apply(_requires.get(name), value);

                if (value != null) {
                    col.appendChild(doc.createTextNode(value.toString()));
                } else {
                    col.appendChild(doc.createTextNode(""));
                }
                row.appendChild(col);

                if (value != null && _generator != null) {
                    try { Beans.setValue(object, Beans.getKnownField(_generator.type, name), value); } catch (Exception x) {}
                }
            }
            root.appendChild(row);
            if (_generator != null) {
                _generator.store(object);
            }
        }
        doc.appendChild(root);

        // send the content into the writer
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(_sink));
        _sink.flush();

        return _sink;
    }
}

