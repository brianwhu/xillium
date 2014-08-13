package lab;

import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class BeansTest {
    @Test(groups={"beans", "beans-object"})
    public void testCreate2() throws Exception {
        A a = Beans.create(A.class, "Good", 95);
        assert a != null && a.name.equals("Good") && a.score == 95 : "Failed to create A by calling the 2-parameter constructor";
    }

    @Test(groups={"beans", "beans-object"})
    public void testCreate1() throws Exception {
        A a = Beans.create(A.class, "Cool");
        assert a != null && a.name.equals("Cool") && a.score == 0 : "Failed to create A by calling the 1-parameter constructor";
    }

    @Test(groups={"beans", "beans-method"})
    public void testInvoke() throws Exception {
        Object a = Beans.create(A.class, "Nice", 64);
        Object output = Beans.invoke(a, "print", "yells", "Hello World");
        assert output != null : "Failed to invoke A's print method";

        Object[] args = { "dummy", "dummy", "yells", "Hello World" };
        Object result = Beans.invoke(a, "print", args, 2, 2);
        assert result.equals(output) : "Failed to handle range of args array";
    }

    private static final String ORIGINAL_TEXT = "introduction-to-the-standard-directory-layout";
    private static final String UPPER_CAMEL_CASE = "IntroductionToTheStandardDirectoryLayout";
    private static final String LOWER_CAMEL_CASE = "introductionToTheStandardDirectoryLayout";

    @Test(groups={"beans", "beans-camelcase"})
    public void testCamelCase() throws Exception {
        String output = Strings.toCamelCase(ORIGINAL_TEXT, '-');
        assert output.equals(UPPER_CAMEL_CASE) : "Failed to convert text to CamelCase";
    }

    @Test(groups={"beans", "beans-camelcase"})
    public void testLowerCamelCase() throws Exception {
        String output = Strings.toLowerCamelCase(ORIGINAL_TEXT, '-');
        assert output.equals(LOWER_CAMEL_CASE) : "Failed to convert text to lower CamelCase";
    }

    @Test(groups={"beans", "beans-camelcase"})
    public void testCapitalize() throws Exception {
        String output = Strings.capitalize(LOWER_CAMEL_CASE);
        assert output.equals(UPPER_CAMEL_CASE) : "Failed to capitalize text";
    }

    @Test(groups={"beans", "beans-object"})
    public void testObjectCopy() throws Exception {
        B a = new B("Good", 10, "morning"), b = new B(null, null, null);
        System.out.println("Before: A = " + a.name + ", " + a.score + ", " + a.desc);
        System.out.println("Before: B = " + b.name + ", " + b.score + ", " + b.desc);
        Beans.fill(b, a);
        System.out.println("After:  B = " + b.name + ", " + b.score + ", " + b.desc);
        assert b.score.equals(a.score) : "Failed to fill object properties";
    }

    @Test(groups={"beans", "beans-valueof"})
    public void testConvert() throws Exception {
        assert Beans.valueOf(String.class, "text").equals("text");
        assert Beans.valueOf(java.sql.Date.class, "2014-01-01").equals(java.sql.Date.valueOf("2014-01-01"));
        assert Beans.valueOf(java.math.BigDecimal.class, "3.1415926535").equals(new java.math.BigDecimal("3.1415926535"));
        assert Beans.valueOf(Integer.class, "65535").intValue() == 65535;
        assert Beans.valueOf(Integer.TYPE, "65535") == 65535;
        assert Beans.valueOf(Status.class, "ACTIVE") == Status.ACTIVE;
    }

    public static class A {
        public A(String name, Integer score) {
            this.name = name;
            this.score = score;
        }

        public A(String name) {
            this.name = name;
            this.score = 0;
        }

        public String print(String say, String message) {
            return '{' + name + '|' + score + "} " + say + ": " + message;
        }

        public String name;
        public Integer score;
    }

    public static class B extends A {
        public String desc;

        public B(String name, Integer score, String desc) {
            super(name, score);
            this.desc = desc;
        }
    }

    public static enum Status {
        READY,
        ACTIVE
    }
}
