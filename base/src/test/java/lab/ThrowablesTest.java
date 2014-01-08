package lab;

import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * Throwables test cases
 */
public class ThrowablesTest {
    @Test(groups={"throwables"})
    public void testThrowables() {
        Exception r = new Exception("Some root causes");
        Exception t = new Exception("outer", new Exception("inner", r));
        System.out.println(Throwables.getFirstMessage(t));
        System.out.println(Throwables.getExplanation(t));
        System.out.println(Throwables.getFullMessage(t));
        assert Throwables.getRootCause(t) == r;
    }
}
