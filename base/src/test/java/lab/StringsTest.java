package lab;

import java.util.*;
import javax.xml.bind.DatatypeConverter;
import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class StringsTest {
    public static class B {
        long q = 21;
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
        for (String s: Strings.collect(new B(), '=', "a", "b", "c", "q")) {
            System.err.println(s);
        }

        System.out.println("collect(String[] storage, int offset, Object object, char separator, String... names)");
        String[] args = new String[6];
        args[0] = "STATIC 1";
        args[1] = "STATIC 2";
        for (String s: Strings.collect(args, 2, "p.", new B(), '=', "q", "a", "b", "c")) {
            System.err.println(s);
        }
    }

    @Test(groups={"hex-string"})
    public void testHexString() throws Exception {
        Random random = new Random();
        byte[] bytes = new byte[1024];
        for (int i = 0; i < 64; ++i) {
            random.nextBytes(bytes);
            String s1 = Strings.toHexString(bytes).toUpperCase();
            String s2 = DatatypeConverter.printHexBinary(bytes);
            assert s1.equals(s2);
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 40960; ++i) {
            Strings.toHexString(bytes);
        }
        long gap = System.currentTimeMillis() - start;
        System.out.println("             Strings.toHexString: " + gap);

        start = System.currentTimeMillis();
        for (int i = 0; i < 40960; ++i) {
            DatatypeConverter.printHexBinary(bytes);
        }
        gap = System.currentTimeMillis() - start;
        System.out.println("DatatypeConverter.printHexBinary: " + gap);
    }

    @Test(groups={"join"})
    public void testJoin() throws Exception {
        assert Strings.join(new int[]{ 1, 3, 5, 7, 9 }, '-').equals("1-3-5-7-9");
        assert Strings.join(new int[]{ 1, 3, 5, 7, 9 }, '-', 11, 13).equals("1-3-5-7-9-11-13");
        assert Strings.join(new String[]{ "Hello", "Mr.", "Jones" }, ' ').equals("Hello Mr. Jones");
        assert Strings.join(new String[]{ "Good", "Morning" }, ' ', "Adam").equals("Good Morning Adam");
    }
}
