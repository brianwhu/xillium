package lab;

import java.util.*;
import java.util.logging.Level;
import org.xillium.base.beans.*;
//import org.xillium.base.etc.*;
//import org.xillium.base.*;

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
}
