package org.xillium.data.persistence.xml;

//import java.util.*;
import java.util.logging.*;
import org.xillium.data.Collector;
import org.xillium.data.DataObject;


/**
 * A java bean mapped to a sql-rs:data element, the root element of an sql-rs XML document.
 *
 * To load a compliant XML document through the Data facility, do the following.
 * <ol>
 * <li> Define a <code>DataObject</code> <code>R</code> that matches the row structure.
        <xmp>
        public class R implements DataObject {
            ...
        }
        </xmp>
        </li>
 * <li> Define an implementation <code>P</code> of <code>Collector&lt;R&gt;</code>, which processes the row objects.
        <xmp>
        public class P implements Collector<R> {
            public boolean add(R row) {
                ...
            }
            ...
        }
        </xmp>
        </li>
 * <li> 
        <xmp>
        BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory();
        XMLBeanAssembler assembler = new XMLBeanAssembler(factory);
        factory.setBurnedIn(Data.class, new P());
        factory.setBurnedIn(Row.class, R.class);
        factory.setBurnedIn(Column.class, R.class);
        Data<R, P> results = (Data<R, P>)assembler.build(inputStream);
        P proc = results.getCollector();
        </xmp></li>
 * </ol>
 */
public class Data<T extends DataObject, C extends Collector<T>> {
    private static final Logger _logger = Logger.getLogger(Data.class.getName());

    private final C _collector;
    private final String _name;
    private int _count;

    /**
     * Constructs a Data object. All but the last argument are to be passed in from a BurnedInArgumentObjectFactory.
     *
     * @param collector
     * @param name - the name of the result set, collected from the XML document
     */
    public Data(C collector, String name) {
        _collector = collector;
        _name = name;
    }

    public Data(C collector) {
        _collector = collector;
        _name = null;
    }

    public void add(Row<T> row) throws Exception {
        ++_count;
        _collector.add(row.data);
    }

    public int getRowCount() {
        return _count;
    }

    public C getCollector() {
        return _collector;
    }
}
