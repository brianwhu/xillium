package lab.text;

import java.util.*;
import javax.xml.bind.DatatypeConverter;
import org.xillium.base.Open;
import org.xillium.base.beans.*;
import org.xillium.base.text.Macro;

import org.testng.annotations.*;


/**
 * Macro test cases
 */
public class MacroNestedTest {
    public static class Struct implements Open {
        public String condition;
        public String statement;
        public Struct otherwise;

        public Struct(String c, String s, Struct o) {
            condition = c;
            statement = s;
            otherwise = o;
        }
    }

    @Test(groups={"functional", "text", "macro-nested"})
    public void testTranslation() {
        Map<String, String> res = new HashMap<>();
        res.put("statement", "CASE WHEN {condition} THEN {statement}{ ELSE @statement:otherwise@} END '{condition}'");

        Struct struct = new Struct("A", "statement group 1",
                            new Struct("B", "statement group 2",
                                new Struct("C", "statement group 3",
                                    new Struct("D", "statement group 4", null)
                                )
                            )
                        );

        System.out.println(Macro.expand(res, "statement", struct));
    }
}
