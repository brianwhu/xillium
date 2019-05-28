package lab;

import java.util.*;
import org.xillium.base.beans.*;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class StringTranslationTest {
    @Test(groups={"function"})
    public void testTranslation() {
        String input = "ObscureErrorMessage-Hour{21}Minute{25}Couple{Joe}And{Jill}# this indicates an internal error";
        String format = "User-friendly message regarding a nice couple %3$s and %4$s at %1$s:%2$s";
        String output = "User-friendly message regarding a nice couple Joe and Jill at 21:25";
        List<String> params = new ArrayList<String>();
        System.err.println("TranslationKey=" + Strings.extractArguments(params, input));
        System.err.println("# of arguments=" + params.size());
        String actual = String.format(format, params.toArray());

        System.err.println(actual);
        assert output.equals(actual);
    }

    @Test(groups={"function"})
    public void testFormat() {
        Object a = new Object() {
            int age = 12;
            String tradingDay = "20130104";
        };
        String blueprint = "Proper file name is 'daily-financial-report-{tradingDay}-{ETF.age}-{nonexistant}.html'";
        String expected1 = "Proper file name is 'daily-financial-report-20130104-12-{nonexistant}.html'";
        String expected2 = "Proper file name is 'daily-financial-report-{tradingDay:20130104}-{ETF.age:12}-{nonexistant}.html'";
        String t1 = Strings.format(blueprint, a);
        String t2 = Strings.format(blueprint, a, true);

        System.err.println(blueprint);
        System.err.println(expected1);
        System.err.println(t1);
        System.err.println(expected2);
        System.err.println(t2);
        assert expected1.equals(t1);
        assert expected2.equals(t2);
    }
}
