package org.xillium.data.persistence.xml;

import java.io.*;
import java.util.List;
import java.util.logging.*;
import org.xillium.data.*;
import org.xillium.base.beans.*;


/**
 * This class models the top-level element of an sql-rs XML document. It also provides
 * static methods to coalesce (deserialize) an sql-rs XML document from a stream.
 *
 * To coalesce a compliant sql-rs XML document, do the following.
 * <ol>
 * <li> Define a <code>DataObject</code> <code>R</code> that matches the row structure of the XML document.
 *      <xmp>
 *      public class R implements DataObject {
 *          ...
 *      }
 *      </xmp>
 *      </li>
 * <li> Define an implementation <code>P</code> of <code>Collector&lt;R&gt;</code>, which processes the row objects.
 *      <xmp>
 *      public class P implements Collector<R> {
 *          public boolean add(R row) {
 *              ...
 *          }
 *          ...
 *      }
 *      </xmp>
 *      </li>
 * <li> <xmp>
 *      P proc = Data.coalesce(inputStream, R.class, new P());
 *      </xmp>
 *      </li>
 * </ol>
 * If a simple java.util.List is all that is desired, the second step can be skipped and the last step becomes
 * <ol>
 * <li> <xmp>
 *      List<R> list = Data.coalesce(inputStream, R.class);
 *      </xmp>
 *      </li>
 * </ol>
 */
public class Data<T extends DataObject, C extends Collector<T>> {
    private static final Logger _logger = Logger.getLogger(Data.class.getName());

    private final C _collector;
    private final String _name;

    /**
     * Constructs a Data object.
     *
     * @param collector
     * @param name - the name of the result set, collected from the XML document
     */
    public Data(C collector, String name) {
        _collector = collector;
        _name = name;
    }

    /**
     * Constructs a Data object.
     *
     * @param collector
     */
    public Data(C collector) {
        _collector = collector;
        _name = null;
    }

    /**
     * Invoked by an XMLBeanAssembler, passes the row object to the collector.
     */
    public void add(Row<T> row) throws Exception {
        _collector.add(row.data);
    }

    /**
     * Returns the collector.
     */
    public C getCollector() {
        return _collector;
    }

    public static <T extends DataObject, C extends Collector<T>> C coalesce(InputStream in, Class<T> rtype, C collector) throws Exception {
        BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory();
        factory.setBurnedIn(Data.class, collector);
        factory.setBurnedIn(Row.class, rtype);
        factory.setBurnedIn(Column.class, rtype);
        try {
            return ((Data<T, C>)new XMLBeanAssembler(factory).build(in)).getCollector();
        } finally {
            in.close();
        }
    }

    public static <T extends DataObject> List<T> coalesce(InputStream in, Class<T> rtype) throws Exception {
        return coalesce(in, rtype, new ArrayListCollector<T>());
    }
}
