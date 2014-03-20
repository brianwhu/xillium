package lab;

import java.util.Arrays;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.core.*;
import org.xillium.gear.auth.*;
import org.testng.annotations.*;


public class ClientAddressAuthTest {
    private static final String[] PATTERN = {
        "192.168.1.*",
        "172.21.*.*",
        "234.66.*.10",
        "234.66.**.10",
        "234.66.a.10",
        "1:2:3:4:5:6:7:8",
        "0:0:0:0:0:0:0:1"
    };

    private static final String[] GOOD = {
        "192.168.1.3",
        "172.21.3.55",
        "234.66.77.10",
        "234.66.78.10",
        "::1"
    };

    private static final String[] BAD = {
        "192.169.1.3",
        "172.20.3.55",
        "::2",
        "172.21.3.*",
    };

    @Test(groups={"ipauth"})
    public void test() throws Exception {
        ClientAddressAuthorizer pa = new ClientAddressAuthorizer(Arrays.asList(PATTERN));

        DataBinder parameters = new DataBinder();

        for (String ip: GOOD) {
            parameters.put(Service.REQUEST_CLIENT_ADDR, ip);
            pa.authorize(null, null, parameters, null);
        }

        for (String ip: BAD) {
            try {
                parameters.put(Service.REQUEST_CLIENT_ADDR, ip);
                pa.authorize(null, null, parameters, null);
                throw new RuntimeException("Failed to catch illegitimate address " + ip);
            } catch (AuthorizationException x) {
            }
        }
    }
}
