package lab.text;

import java.util.*;
import javax.xml.bind.DatatypeConverter;
import org.xillium.base.Open;
import org.xillium.base.beans.*;
import org.xillium.base.text.Macro;
import org.xillium.base.model.*;

import org.testng.annotations.*;
import lab.TestingHelper;


/**
 * Macro test cases
 */
public class MacroAdvancedTest {
    public static class Content {
        public final String body;
        public final String[] notes;

        public Content(String c) {
            body = c;
            notes = new String[] { "Ref1", "Ref2" };
        }
    }

    public static class Event implements Open {
        public String title;
        public String location;
        public Content content;

        public Event(String t, String l, String c) {
            title = t;
            location = l;
            content = new Content(c);
        }

        public Date date() {
            return new Date();
        }

        public Object select(int value) {
            return value == 0 ? null : this;
        }
    }

    @Test(groups={"functional", "text", "macro", "macro-advanced"})
    public void testMethodInvocation() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("macro/Text", "{{*}}");
        map.put("macro/Value", "{{value}}");
        map.put("macro/Bad", "{{missing:-please contact support@company.net}}");
        map.put("macro/Missing", "{{missing:-please contact support$company.net}}");
        map.put("macro/Date", "Current date is {@Value:date()@}");
        map.put("macro/Five", "{{title}}{@Text({{location}}({{content.notes[0]}})):select(5):Text({{content.body}}({{content.notes[0]}}))@}");
        map.put("macro/Zero", "{{title}}{@Text({{location}}({{content.notes[1]}})):select(0):Text({{content.body}}({{content.notes[1]}}))@}");
        System.out.println("== macro-advanced - resources ====");
        System.out.println(Beans.toString(map));
        System.out.println("== macro-advanced - expansion ====");
        System.out.println(Macro.expand(map, "macro/Date", new Event("", "", "")));
        TestingHelper.assertEqual(Macro.expand(map, "macro/Bad", new Event("", "", "")), "{{missing:-please contact support@company.net}}");
        TestingHelper.assertEqual(Macro.expand(map, "macro/Missing", new Event("", "", "")), "please contact support$company.net");
        TestingHelper.assertEqual(Macro.expand(map, "macro/Five", new Event("Five", "Location", "Content")), "FiveLocation(Ref1)");
        TestingHelper.assertEqual(Macro.expand(map, "macro/Zero", new Event("Zero", "Location", "Content")), "ZeroContent(Ref2)");
    }
}
