package lab;

import java.util.*;
import org.xillium.base.beans.*;
//import org.xillium.base.etc.*;
//import org.xillium.base.*;

import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class StringsTest {
    public static class B {
        int a = 12;
        String b = "Jon";
        double c = 12.25;
    }

    @Test(groups={"function"})
    public void testCollection() {
        System.out.println("collect(Object object, String[] names)");
        for (String s: Strings.collect(new B(), "a", "b")) {
            System.err.println(s);
        }

        System.out.println("collect(Object object, char separator, String... names)");
        for (String s: Strings.collect(new B(), '=', "a", "b", "c")) {
            System.err.println(s);
        }

        System.out.println("collect(String[] storage, int offset, Object object, char separator, String... names)");
        String[] args = new String[5];
        args[0] = "STATIC 1";
        args[1] = "STATIC 2";
        for (String s: Strings.collect(args, 2, "p.", new B(), '=', "a", "b", "c")) {
            System.err.println(s);
        }
    }
}
