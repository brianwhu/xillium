package lab;

import java.lang.reflect.*;
import java.math.*;

import org.xillium.base.type.*;
import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * Beans.setValue() tests.
 */
public class BeansValuesTest {
    @Test(groups={"beans beans-values"})
    public void testSetValue() throws Exception {
        B b = new B();
        Beans.setValue(b, B.class.getField("s"), 0);
        Beans.setValue(b, B.class.getField("m"), "MONDAY");
        Beans.setValue(b, B.class.getField("t"), 2);
        Beans.setValue(b, B.class.getField("x"), " SUNDAY : : TUESDAY , ");
        Beans.setValue(b, B.class.getField("decimal"), 3.1415926535);
        Beans.setValue(b, B.class.getField("integer"), 65536L);
        assert b.s == A.SUNDAY;
        assert b.m == A.MONDAY;
        assert b.t == A.TUESDAY;
        assert b.x.isSet(A.SUNDAY);
        assert !b.x.isSet(A.MONDAY);
        assert b.x.isSet(A.TUESDAY);
        assert b.x.toString().equals("SUNDAY:TUESDAY");
        assert !b.x.isNone();
        assert b.decimal.equals(new BigDecimal("3.1415926535"));
        assert b.integer.equals(new BigInteger("65536"));
    }

    public static enum A {
        SUNDAY,
        MONDAY,
        TUESDAY
    }

    static class B {
        public A s;
        public A m;
        public A t;
        @typeinfo(A.class) public Flags<A> x;
        public BigDecimal decimal;
        public BigInteger integer;
    }
}
