package org.xillium.data.persistence.xml;

import java.sql.*;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
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
import org.xillium.base.beans.Strings;
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
    public static final String TYPE      = "type";

    private Map<String, Assertion> _assertions = Collections.EMPTY_MAP;
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
        if (_assertions == Collections.EMPTY_MAP) _assertions = new HashMap<String, Assertion>();
        _assertions.put(column, assertion);
        return this;
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
        while (rs.next()) {
            Element row = doc.createElement(ROW);
            for (int i = 0; i < count; ++i) {
                String name = Strings.toLowerCamelCase(meta.getColumnLabel(i+1), '_');

                Element col = doc.createElement(COLUMN);
                attr = doc.createAttribute(NAME);
                attr.setValue(name);
                col.setAttributeNode(attr);
                //attr = doc.createAttribute(TYPE);
                //attr.setValue(Strings.toLowerCamelCase(meta.getColumnClassName(i+1), '_'));
                //col.setAttributeNode(attr);

                Object value = rs.getObject(i+1);

                Assertion.S.apply(_assertions.get(name), value);

                if (value != null) {
                    col.appendChild(doc.createTextNode(value.toString()));
                } else {
                    col.appendChild(doc.createTextNode(""));
                }
                row.appendChild(col);
            }
            root.appendChild(row);
        }
        doc.appendChild(root);

        // send the content into the writer
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(_sink));
        _sink.flush();

        return _sink;
    }
}

