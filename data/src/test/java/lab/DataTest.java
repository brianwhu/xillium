package lab;

import org.xillium.data.*;
import org.testng.annotations.*;
import lab.data.validation.SubmitPurchaseOrderData;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class DataTest {
    @Test(groups={"DataObject"})
    public void testDescribe() throws Exception {
        System.out.println(DataObject.Util.describe(SubmitPurchaseOrderData.class));
    }
}
