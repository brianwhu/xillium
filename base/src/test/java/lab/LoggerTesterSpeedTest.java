package lab;

import java.util.*;
import java.util.logging.*;
import org.xillium.base.etc.S;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class LoggerTesterSpeedTest {
    private static final Logger _logger = Logger.getLogger(LoggerTesterSpeedTest.class.getName());

    private static String traceArgs(Object[] property) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < property.length; ++i) { sb.append(property[i].getClass().getName()).append(':').append(property[i]); }
        return sb.append(')').toString();
    }

    @Test(groups={"function"})
    public void testSpeed() {
        Object[] property = {
            new Date(),
            "Hello World",
            Locale.getDefault()
        };

        long now = System.currentTimeMillis();
        for (int i = 0; i < 10000; ++i) {
            _logger.fine("event logged about " + this.getClass() + ": " + property.length);
        }
        long elapsed = System.currentTimeMillis() - now;
        System.out.println("Simple      without tester: " + elapsed);
        now = System.currentTimeMillis();
        for (int i = 0; i < 2000; ++i) {
            _logger.fine("event logged about " + this.getClass() + ": " + property.length + traceArgs(property));
        }
        elapsed = System.currentTimeMillis() - now;
        System.out.println("Complicated without tester: " + elapsed);
        now = System.currentTimeMillis();
        for (int i = 0; i < 2000; ++i) {
            _logger.fine(S.fine(_logger) ? "event logged about " + this.getClass() + ": " + property.length + traceArgs(property) : null);
        }
        elapsed = System.currentTimeMillis() - now;
        System.out.println("Complicated with    tester: " + elapsed);
    }
}
