package lab.data.validation;

import lab.*;
import org.xillium.base.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.testng.annotations.*;


public class ValidationTest {
    @Test(groups={"validation"})
    public void testCollection() throws Exception {
/*
        Dictionary dictionary =
            (Dictionary)new XMLBeanAssembler(new DefaultObjectFactory(), new StandardTrace().setLevel(Level.INFO)).build(args[0]);
        System.err.println("Base Types = ");
        System.err.println(Beans.toString(Dictionary.bases));
*/
        //Trace.g.configure(new StandardTrace()).std.setFilter(Dictionary.class);
        Trace.g.configure(new StandardTrace());

        //Dictionary dictionary = new Dictionary("core").addDataDictionary(StandardDataTypes.class);
        //Dictionary dictionary = (Dictionary)new XMLBeanAssembler(new DefaultObjectFactory(), Trace.g.std).build("lab/validation/dictionary.xml");
        Dictionary dictionary = (Dictionary)new XMLBeanAssembler(new DefaultObjectFactory()).build(getClass().getResourceAsStream("/validation/dictionary.xml"));
        System.err.println("Dictionary = " + Beans.toString(dictionary));

        DataBinder binder = new DataBinder();
        DataUtil.loadFromProperties(binder, getClass().getResourceAsStream("/validation/SubmitPurchaseOrderData.properties"));

        //DataUtil.loadFromArgs(binder, args, 1);

        Trace.g.configure(new NullTrace());
        long now = System.currentTimeMillis();
        DataObject object = dictionary.collect(new lab.data.validation.SubmitPurchaseOrderData(), binder);
        //for (int i = 0; i < 300; ++i) {
            //btrd = dictionary.collect(btrd, binder);
        //}
        long elapsed = System.currentTimeMillis() - now;
        System.err.println("Time = " + elapsed + ", DataObject =");
        System.err.println(Beans.toString(object));
    }
}
