package lab;

import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class BeansValuesTest {
    @Test(groups={"Values"})
    public void testSetValue() throws Exception {
        B b = new B();
        Beans.setValue(b, B.class.getField("s"), 0);
        Beans.setValue(b, B.class.getField("m"), "MONDAY");
        Beans.setValue(b, B.class.getField("t"), 2);
        assert b.s == A.SUNDAY;
        assert b.m == A.MONDAY;
        assert b.t == A.TUESDAY;
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
    }
}
