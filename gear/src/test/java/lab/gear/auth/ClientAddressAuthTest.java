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
        "234.66.*",
        "234.66.a.10",
        "977.66.5.10",
        "1:A3C2:3:4:5:6:7:*",
        "0:0:0:0:0:0:0:1",
        "1:good:3:4:5:6:7:*",
    };

    private static final String[] GOOD = {
        "192.168.1.3",
        "172.21.3.55",
        "234.66.77.10",
        "234.66.78.10",
        "::1",
        "1:a3c2:3:4:5:6:7:8"
    };

    private static final String[] BAD = {
        "192.169.1.3",
        "172.20.3.55",
        "::2",
        "172.21.3.*",
    };

    private static final String[] BAD_PRIVATE = {
        "192.168.5.3",
        "172.16.3.55",
        "172.24.6.1",
        "172.31.3.1",
    };

    private static final String MORE_PATTERNS = "10.0.*.*, 10.1.10.*, 10.2.*.*";

    private static final String[] MORE_GOOD = {
        "192.168.1.5",
        "172.21.10.77",
        "10.0.112.30",
        "10.0.112.40",
        "10.1.10.211",
        "10.1.10.212",
        "10.2.10.211",
    };


    @Test(groups={"ipauth"})
    public void test() throws Exception {
        ClientAddressAuthorizer pa = new ClientAddressAuthorizer(PATTERN);
        pa.setAuthorizedPatterns(" "); // no effect

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

        for (String ip: BAD_PRIVATE) {
            try {
                parameters.put(Service.REQUEST_CLIENT_ADDR, ip);
                pa.authorize(null, null, parameters, null);
                throw new RuntimeException("Failed to catch illegitimate address " + ip);
            } catch (AuthorizationException x) {
            }
        }

        pa.setAllowingPrivate(true);
        for (String ip: BAD_PRIVATE) {
            parameters.put(Service.REQUEST_CLIENT_ADDR, ip);
            pa.authorize(null, null, parameters, null);
        }
        pa.setAllowingPrivate(false);

        pa.setAuthorizedPatterns(MORE_PATTERNS);
        for (String ip: MORE_GOOD) {
            parameters.put(Service.REQUEST_CLIENT_ADDR, ip);
            pa.authorize(null, null, parameters, null);
        }
    }
}
