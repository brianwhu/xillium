package lab;

import java.util.*;
import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class BeansToStringTest {
    public static class A {
        public List<String> listS;
        public B b;
    }

    public static class B {
        public List<A> listA;
    }

    @Test(groups={"beans", "toString"})
    public void testToString() throws Exception {
        A a = new A();
        a.listS = new ArrayList<String>();
        a.listS.add("one");
        a.listS.add("two");
        a.listS.add("ten");
        a.b = new B();
        a.b.listA = new ArrayList<A>();
        a.b.listA.add(a);
        a.b.listA.add(a);
        //System.out.println("BeansToStringTest.testToString");
        //System.out.println(a);
        System.out.println(Beans.toString(a));
        //assert a != null && a.name.equals("Good") && a.score == 95 : "Failed to create A by calling the 2-parameter constructor";
    }
}
