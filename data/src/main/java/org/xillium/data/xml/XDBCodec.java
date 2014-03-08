package org.xillium.data.xml;

import java.io.IOException;
import java.io.InputStream;;
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
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xillium.base.beans.Strings;
import org.xillium.base.util.XML;
import org.xillium.data.DataBinder;
import org.xillium.data.CachedResultSet;


/**
 * XML data binder codec.
 */
public class XDBCodec {
    public static final String NAMESPACE = "http://xillium.org/data/xml";
    public static final String XDB_NS    = "xdb:";
    public static final String NAME      = "name";
    public static final String M = XDB.Element.M.toString().toLowerCase();
    public static final String P = XDB.Element.P.toString().toLowerCase();
    public static final String T = XDB.Element.T.toString().toLowerCase();
    public static final String H = XDB.Element.H.toString().toLowerCase();
    public static final String R = XDB.Element.R.toString().toLowerCase();
    public static final String C = XDB.Element.C.toString().toLowerCase();

	public static Writer encode(Writer sink, DataBinder binder) throws ParserConfigurationException, TransformerException, IOException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElementNS(NAMESPACE, XDB_NS + M);

        Iterator<String> it = binder.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (key.charAt(0) == '#' && key.charAt(key.length()-1) == '#') continue;
            String val = binder.get(key);

            Element param = doc.createElement(P);
            Attr attr = doc.createAttribute(NAME);
            attr.setValue(key);
            param.setAttributeNode(attr);

            if (val != null) {
                param.appendChild(doc.createTextNode(val.toString()));
            } else {
                param.appendChild(doc.createTextNode(""));
            }
            root.appendChild(param);
        }

        Set<String> rsets = binder.getResultSetNames();
        it = rsets.iterator();
        while (it.hasNext()) {
            String key = it.next();

            Element table = doc.createElement(T);
            Attr attr = doc.createAttribute(NAME);
            attr.setValue(key);
            table.setAttributeNode(attr);

            CachedResultSet rset = binder.getResultSet(key);
            Element row = doc.createElement(H);
            for (int i = 0; i < rset.columns.length; ++i) {
                Element col = doc.createElement(C);
                col.appendChild(doc.createTextNode(rset.columns[i]));
                row.appendChild(col);
            }
            table.appendChild(row);

            for (int r = 0; r < rset.rows.size(); ++r) {
                row = doc.createElement(R);
                for (int i = 0; i < rset.columns.length; ++i) {
                    Element col = doc.createElement(C);
                    col.appendChild(doc.createTextNode(Strings.toString(rset.rows.get(r)[i])));
                    row.appendChild(col);
                }
                table.appendChild(row);
            }
            root.appendChild(table);
        }

        doc.appendChild(root);

        // send the content into the writer
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(sink));
        sink.flush();

        return sink;
    }

    /**
     * Decodes an XML stream. Returns the input stream.
     */
    public static InputStream decode(DataBinder binder, InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        XML.newSAXParser().parse(stream, new XDBHandler(binder));
        return stream;
    }
}

