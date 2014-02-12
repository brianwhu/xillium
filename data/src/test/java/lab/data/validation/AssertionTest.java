package lab.data.validation;

import java.math.BigDecimal;
import lab.*;
import org.xillium.base.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;
import org.testng.annotations.*;


public class AssertionTest {
    @Test(groups={"validation assertion"})
    public void testAssertion() throws Exception {
        Assertion.S.NonnegativeNumber.apply(BigDecimal.ONE);
        Assertion.S.NonnegativeNumber.apply(0.000000000003);
        Assertion.S.apply(null, -1);
        try {
            Assertion.S.NonnegativeNumber.apply(-0.000000000000000000000000001);
            throw new RuntimeException("Failed to catch negative number");
        } catch (DataValidationException x) {
            System.out.println(x.getMessage());
        }
    }
}
