package lab.data.xml;

import lab.*;
import org.xillium.base.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.xillium.data.xml.*;
import org.testng.annotations.*;


public class XML2BinderTest {
    public static class Statement implements DataObject {
        @required public String bankId;
        @required public String currency;
        @required public long balance;
        @required public String state;
    }

    public static class Request implements DataObject {
        public String p1;
        public String p2;
        public Statement[] accounts;
        public Statement[] other;
    }

    @Test(groups={"xml"})
    public void testCollection() throws Exception {
        DataBinder binder = new DataBinder();
        XDBCodec.decode(binder, getClass().getResourceAsStream("/xml/data-exchange.xml"));
        System.out.println(Beans.toString(binder));

        Request r = new Reifier().collect(new Request(), binder);
        System.out.println(Beans.toString(r));
/*
        long now = System.currentTimeMillis();
        DataObject object = dictionary.collect(new lab.data.validation.SubmitPurchaseOrderData(), binder);
        //for (int i = 0; i < 300; ++i) {
            //btrd = dictionary.collect(btrd, binder);
        //}
        long elapsed = System.currentTimeMillis() - now;
        System.err.println("Time = " + elapsed + ", DataObject =");
        System.err.println(Beans.toString(object));
*/
    }
}
