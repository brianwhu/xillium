package lab.text;

import java.util.*;
import java.util.regex.Pattern;
//import javax.xml.bind.DatatypeConverter;
//import org.xillium.base.Open;
import org.xillium.base.Functor;
import org.xillium.base.text.Balanced;

import org.testng.annotations.*;


/**
 * Macro test cases
 */
public class BalancedTest {
    private final String t1 = "NVL(return_error_type, result_type) first argument<A, P>, second argument,[f a, (g 1, 3)]";
    private final String t2 = "DECODE(message_type, 'a, b', 'c'),'list, array',\"java, c++\",(f g)";
    private final String t3 = "DataViewTextAsIs(NVL(return_error_type, result_type) result_type):resultSensitive";
    private final String t4 = "DataViewTextAsIs(NVL(result_type||':'||return_error_type, result_type||':') result_type):resultSensitive";
    private final String t5 = "command 'argument with double quote \"' \"argument with single quote '\"";
    private final String t6 = "command;'argument with double quote \"';\"argument with single quote '\";;;";
    private final String t7 = "command;'argument with double quote \"';\"argument with single quote '\";;;action";
    private final String t8 = "(<) , (>, <, ";

    private final String c1 = ",,Removes the element at the specified position in this list (optional operation),de,,,";
    private final String c2 = ",,Removes the element at the specified position in this list (optional operation),de,,,z";

    @Test(groups={"functional", "text", "balanced"})
    public void testTranslation() {
        Functor<Integer, Integer> angular = new Functor<Integer, Integer>() {
            public Integer invoke(Integer c) {
                if (c == '<') return (int)'>'; else return 0;
            }
        };

        index(t1, ',', null, 52, 56, 73, -1);
        index(t1, ',', angular, 56, 73, -1);
        split(t1, ',', angular, "NVL(return_error_type, result_type) first argument<A, P>", " second argument", "[f a, (g 1, 3)]");
        index(t2, ',', null, 33, 47, 59, -1);
        split(t2, ',', null, "DECODE(message_type, 'a, b', 'c')", "'list, array'", "\"java, c++\"", "(f g)");
        split(t3, ':', null, "DataViewTextAsIs(NVL(return_error_type, result_type) result_type)", "resultSensitive");
        split(t4, ':', null, "DataViewTextAsIs(NVL(result_type||':'||return_error_type, result_type||':') result_type)", "resultSensitive");
        split(t5, ' ', null, "command", "'argument with double quote \"'", "\"argument with single quote '\"");
        split(t6, ';', null, "command", "'argument with double quote \"'", "\"argument with single quote '\"");
        split(t7, ';', null, "command", "'argument with double quote \"'", "\"argument with single quote '\"", "", "", "action");
        split(t8, ',', null, "(<) ", " (>, <, ");
        split(t8, ',', angular, "(<) , (>, <, ");

        split(c1, ',', null, "", "", "Removes the element at the specified position in this list (optional operation)", "de");
        split(c2, ',', null, "", "", "Removes the element at the specified position in this list (optional operation)", "de", "", "", "z");
    }

    private void index(String text, char target, Functor<Integer, Integer> symbols, int... expected) {
        int offset = 0;
        for (int i = 0; i < expected.length; ++i) {
            int found = Balanced.indexOf(null, text, offset, text.length(), target, symbols);
            assert found == expected[i] : text + ": index " + found + " == " + expected[i];
            offset = found + 1;
        }
    }

    private void split(String text, char delimiter, Functor<Integer, Integer> symbols, String... expected) {
        String[] parts = Balanced.split(null, text, 0, text.length(), delimiter, symbols);
        //System.out.println(org.xillium.base.beans.Beans.toString(parts));
        assert parts.length == expected.length : text + ": split parts count " + parts.length + " == " + expected.length;
        for (int i = 0; i < parts.length; ++i) {
            assert parts[i].equals(expected[i]) : "\"" + parts[i] + "\" == \"" + expected[i] + "\"";
        }
    }

    @Test(groups={"functional", "text", "balanced", "balanced-speed"})
    public void testBalancedSpeed() {
        Pattern p = Pattern.compile(",");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1024*1024; ++i) {
            p.split(c2);
        }
        long cost = System.currentTimeMillis() - start;
        System.out.println("String.split: " + cost);
        start = System.currentTimeMillis();
        for (int i = 0; i < 1024*1024; ++i) {
            Balanced.split(c2, ',');
        }
        cost = System.currentTimeMillis() - start;
        System.out.println("Balanced.split: " + cost);

    }
}
