package lab;

import java.lang.reflect.*;

import org.xillium.base.type.Flags;
import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class BeansValuesTest {
    @Test(groups={"beans beans-values"})
    public void testSetValue() throws Exception {
        B b = new B();
        Beans.setValue(b, B.class.getField("s"), 0);
        Beans.setValue(b, B.class.getField("m"), "MONDAY");
        Beans.setValue(b, B.class.getField("t"), 2);
        Beans.setValue(b, B.class.getField("x"), " SUNDAY : : TUESDAY , ");
        assert b.s == A.SUNDAY;
        assert b.m == A.MONDAY;
        assert b.t == A.TUESDAY;
        assert b.x.isSet(A.SUNDAY);
        assert !b.x.isSet(A.MONDAY);
        assert b.x.isSet(A.TUESDAY);
        assert b.x.toString().equals("SUNDAY:TUESDAY");
        assert !b.x.isNone();
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
        public Flags<A> x = new Flags<A>(A.class).set(A.MONDAY);
    }
}
