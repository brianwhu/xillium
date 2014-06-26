package lab.gear.util;

//import org.xillium.core.management.*;
import org.xillium.gear.util.*;
import org.testng.annotations.*;


public class ExponentialBackoffTest {
    @Test(groups={"trial"})
    public void test() throws Exception {
        for (int i = 0; i < 1000; ++i) {
            long delay = ExponentialBackoff.computeRandomizedExponentialSequence(i);
            System.out.println(delay);
            assert delay <= ExponentialBackoff.INIT_BACKOFF + ExponentialBackoff.INIT_BACKOFF * (1L << ExponentialBackoff.MAX_EXPONENT);
        }

    }
}
