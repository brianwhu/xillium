package lab;

import java.lang.reflect.*;
import java.math.BigDecimal;

import org.xillium.base.beans.Beans;
import org.xillium.base.type.*;
import org.xillium.base.util.*;
import org.testng.annotations.*;


/**
 * ValueOf tests.
 */
public class ValueOfTest {
    @Test(groups={"util", "util-valueof", "util-valueof-simple"})
    @SuppressWarnings("unchecked")
    public void testValueOf() throws Exception {
        ValueOf aValueOf = new ValueOf(A.class);
        ValueOf fValueOf = new ValueOf(Flags.class, B.class.getField("x").getAnnotation(typeinfo.class));
        ValueOf iValueOf = new ValueOf(Integer.TYPE);
        ValueOf sValueOf = new ValueOf(String.class);
        ValueOf dValueOf = new ValueOf(java.math.BigDecimal.class);

        assert aValueOf.invoke("SUNDAY") == A.SUNDAY;
        assert aValueOf.invoke("MONDAY") == A.MONDAY;
        assert aValueOf.invoke("TUESDAY") == A.TUESDAY;
        try {
            aValueOf.invoke("2.17");
            assert false;
        } catch (Exception x) {}

        B b = new B();
        b.x = (Flags<A>)fValueOf.invoke(" SUNDAY : : TUESDAY , ");
        assert b.x.isSet(A.SUNDAY);
        assert !b.x.isSet(A.MONDAY);
        assert b.x.isSet(A.TUESDAY);
        assert b.x.toString().equals("SUNDAY:TUESDAY");
        assert !b.x.isNone();
        try {
            fValueOf.invoke("SUNDAY-TUESDAY");
            assert false;
        } catch (Exception x) {}

        assert iValueOf.invoke("23").equals(new Integer(23));
        assert iValueOf.invoke("") == null;
        assert iValueOf.invoke(null) == null;
        assert sValueOf.invoke("").equals("");
        assert sValueOf.invoke(null) == null;
        assert dValueOf.invoke("3.1415").equals(new java.math.BigDecimal("3.1415"));
    }

    @Test(groups={"util", "util-valueof", "util-valueof-array"})
    @SuppressWarnings("unchecked")
    public void testValueOfArray() throws Exception {
        Field coords = B.class.getField("coords");
        ValueOf cValueOf = new ValueOf(coords.getType());

        B b = new B();
        coords.set(b, cValueOf.invoke("12.3, 6.54, 2.00"));
        System.err.println("B = " + Beans.toString(b));
        assert b.coords.length == 3;
        assert b.coords[0].equals(new BigDecimal("12.3"));
        assert b.coords[1].equals(new BigDecimal("6.54"));
        assert b.coords[2].equals(new BigDecimal("2.00"));
    }

    @Test(groups={"util", "util-valueof", "util-valueof-speed"})
    @SuppressWarnings("unchecked")
    public void testValueOfSpeed() throws Exception {
        String text = "MONDAY : TUESDAY";
        Field f = B.class.getField("x");
        typeinfo t = f.getAnnotation(typeinfo.class);

        B b = new B();

        long now = System.currentTimeMillis();
        for (int i = 0; i < 5000; ++i) Beans.setValue(b, f, "MONDAY : TUESDAY");
        System.err.println("Beans.setValue: " + (System.currentTimeMillis() - now));

        now = System.currentTimeMillis();
        for (int i = 0; i < 5000; ++i)  b.x = (Flags<A>)Beans.valueOf(Flags.class, text, t);
        System.err.println(" Beans.valueOf: " + (System.currentTimeMillis() - now));

        now = System.currentTimeMillis();
        for (int i = 0; i < 5000; ++i) b.x = (Flags<A>)new ValueOf(Flags.class, t.value()).invoke(text);
        System.err.println("       ValueOf: " + (System.currentTimeMillis() - now));
    }

    public static enum A {
        SUNDAY,
        MONDAY,
        TUESDAY
    }

    public static class B {
        @typeinfo(A.class) public Flags<A> x;
        public BigDecimal[] coords;
    }
}
