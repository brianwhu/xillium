package lab;

import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class BeansTest {
    @Test(groups={"ObjectConstruction"})
    public void testCreate2() throws Exception {
        A a = (A)Beans.create(A.class, "Good", 95);
        assert a != null && a.name.equals("Good") && a.score == 95 : "Failed to create A by calling the 2-parameter constructor";
    }

    @Test(groups={"ObjectConstruction"})
    public void testCreate1() throws Exception {
        A a = (A)Beans.create(A.class, "Cool");
        assert a != null && a.name.equals("Cool") && a.score == 0 : "Failed to create A by calling the 1-parameter constructor";
    }

    @Test(groups={"MethodInvocation"})
    public void testInvoke() throws Exception {
        Object a = Beans.create(A.class, "Nice", 64);
        Object output = Beans.invoke(a, "print", "yells", "Hello World");
        assert output != null : "Failed to invoke A's print method";
    }

    private static final String ORIGINAL_TEXT = "introduction-to-the-standard-directory-layout";
    private static final String UPPER_CAMEL_CASE = "IntroductionToTheStandardDirectoryLayout";
    private static final String LOWER_CAMEL_CASE = "introductionToTheStandardDirectoryLayout";

    @Test(groups={"CamelCase"})
    public void testCamelCase() throws Exception {
        String output = Beans.toCamelCase(ORIGINAL_TEXT, '-');
        assert output.equals(UPPER_CAMEL_CASE) : "Failed to convert text to CamelCase";
    }

    @Test(groups={"CamelCase"})
    public void testLowerCamelCase() throws Exception {
        String output = Beans.toLowerCamelCase(ORIGINAL_TEXT, '-');
        assert output.equals(LOWER_CAMEL_CASE) : "Failed to convert text to lower CamelCase";
    }

    @Test(groups={"CamelCase"})
    public void testCapitalize() throws Exception {
        String output = Beans.capitalize(LOWER_CAMEL_CASE);
        assert output.equals(UPPER_CAMEL_CASE) : "Failed to capitalize text";
    }

    public static class A {
        public A(String name, int score) {
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

        String name;
        int score;
    }
}
