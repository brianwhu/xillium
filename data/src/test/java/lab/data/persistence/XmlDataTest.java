package lab.data.persistence;

import java.util.*;
import org.xillium.base.beans.*;
import org.xillium.data.persistence.xml.*;
import org.xillium.data.*;

import org.testng.annotations.*;


/**
 *
 */
public class XmlDataTest {
    public static class Reconciliation implements DataObject {
    public String fileId;
    public String bankId;
    public String bankingSerial;
    public String transactionId;
    public String initiator;
    public String accountNumber;
    public String merchantId;
    public String currency;
    public int cash;
    public int action;
    public long amount;
    public long secured;
    public long released;
    public String tradingDay;
    public String status;
    }

    public static class WithBigDecimal extends Reconciliation {
        public java.math.BigDecimal rate;
    }

    public static class Proc implements Collector<Reconciliation> {
        public boolean add(Reconciliation data) {
            System.err.println(Beans.toString(data));
            return true;
        }

        public void hello() {
            System.err.println("It's me... " + getClass().getName());
        }
    }

    @Test(groups={"function", "xml"})
    public void testUpdator() throws Exception {
        System.err.println("start xml data processing");

        BurnedInArgumentsObjectFactory factory = new BurnedInArgumentsObjectFactory();
        XMLBeanAssembler assembler = new XMLBeanAssembler(factory);
        factory.setBurnedIn(Data.class, new Proc());
        factory.setBurnedIn(Row.class, Reconciliation.class);
        factory.setBurnedIn(Column.class, Reconciliation.class);
        Data<Reconciliation, Proc> results = (Data<Reconciliation, Proc>)assembler.build(getClass().getResourceAsStream("/FT05.xml"));
        results.getCollector().hello();
    }

    @Test(groups={"function", "xml"})
    public void testCoalescing() throws Exception {
        System.err.println("start xml data coalescing");
        Proc p = Data.coalesce(getClass().getResourceAsStream("/FT05.xml"), Reconciliation.class, new Proc());
        p.hello();
    }

    @Test(groups={"function", "xml"})
    public void testSimplified() throws Exception {
        System.err.println("start simplified xml data processing");
        List<Reconciliation> list = Data.coalesce(getClass().getResourceAsStream("/FT05.xml"), Reconciliation.class);
        for (Reconciliation r: list) {
            System.out.println(Beans.toString(r));
        }
    }

    @Test(groups={"function", "xml"})
    public void testBigDecimal() throws Exception {
        System.err.println("start simplified xml data processing with BigDecimal");
        List<WithBigDecimal> list = Data.coalesce(getClass().getResourceAsStream("/FT05withBigDecimal.xml"), WithBigDecimal.class);
        for (WithBigDecimal r: list) {
            System.out.println(Beans.toString(r));
        }
    }
}
