package lab.data;

import java.util.*;
import org.testng.annotations.*;
import org.xillium.data.*;
import org.xillium.base.beans.*;


/**
 * Testing Persistence
 */
public class ResultSetTest {
    public static class GlobalStreetAddress extends StreetAddress {
        public String country;
    }

    public static class Request implements DataObject {
        public GlobalStreetAddress[] addresses;
    }

	@Test(groups={"resultset"})
	public void testResultSets() throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/addresses.properties"));

        DataBinder binder = new DataBinder().load(props);
        Request request = new org.xillium.data.validation.Dictionary().collect(new Request(), binder);
        System.out.println("input array = " + Beans.toString(request));

        CachedResultSet rs = new CachedResultSet(Arrays.asList(request.addresses));
        System.out.println("result set = " + Beans.toString(rs));

        List<StreetAddress> list = rs.asList(StreetAddress.class);
        System.out.println("as list = " + Beans.toString(list));
	}
}
