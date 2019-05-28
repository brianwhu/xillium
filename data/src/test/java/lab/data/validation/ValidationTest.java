package lab.data.validation;

import lab.*;
import org.xillium.base.*;
import org.xillium.base.beans.*;
import org.xillium.base.type.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.testng.annotations.*;


public class ValidationTest {
    @Test(groups={"validation"})
    public void testCollection() throws Exception {
        Reifier reifier = new XMLBeanAssembler(new DefaultObjectFactory()).build(getClass().getResourceAsStream("/validation/reifier.xml"), Reifier.class);
        System.err.println("Reifier = " + Beans.toString(reifier));
        System.err.println("SubmitPurchaseOrderData = " + DataObject.Util.describe(lab.data.validation.SubmitPurchaseOrderData.class));

        DataBinder binder = new DataBinder();
        DataUtil.loadFromProperties(binder, getClass().getResourceAsStream("/validation/SubmitPurchaseOrderData.properties"));

        //DataUtil.loadFromArgs(binder, args, 1);

        long now = System.currentTimeMillis();
        DataObject object = reifier.collect(new lab.data.validation.SubmitPurchaseOrderData(), binder);
        //for (int i = 0; i < 300; ++i) {
            //btrd = reifier.collect(btrd, binder);
        //}
        long elapsed = System.currentTimeMillis() - now;
        System.err.println("Time = " + elapsed + ", DataObject =");
        System.err.println(Beans.toString(object));
    }

    public static enum Options {
        A,
        B,
        C
    }

    public static class Special implements DataObject {
        public Options single;
        @range(max="2000") public java.math.BigInteger price;
        @typeinfo(Options.class) public Flags<Options> multiple;

        public Special() {}

        public Special(Options s, java.math.BigInteger p, Flags<Options> m) {
            single = s;
            price = p;
            multiple = m;
        }
    }

    @Test(groups={"validation", "validation-special"})
    public void testSpecialTypes() throws Exception {
        DataBinder binder = new DataBinder();
        binder.put("single", "B");
        binder.put("multiple", "A, C");
        binder.put("price", "2015");

        Reifier reifier = new XMLBeanAssembler(new DefaultObjectFactory()).build(getClass().getResourceAsStream("/validation/reifier.xml"), Reifier.class);
        try {
            Special special = reifier.collect(new Special(), binder);
            assert false;
        } catch (Exception x) {}

        binder.put("price", "2000");
        Special special = reifier.collect(new Special(), binder);
        Special expected = new Special(Options.B, new java.math.BigInteger("2000"), Flags.valueOf(Options.class, "A, C"));
        assert expected.single == special.single;
        assert expected.price.equals(special.price);
        assert expected.multiple.equals(special.multiple);
    }
}
