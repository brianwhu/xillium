package lab.data.persistence;

import java.util.*;
//import org.xillium.base.beans.*;
import org.xillium.data.persistence.xml.*;
//import org.xillium.base.*;

import org.testng.annotations.*;


/**
 *
 */
public class TableUpdatorTest {
    public static class Proc implements TableUpdator.Processor {
        public void process(String type, Map<String, String> data) {
            System.out.print(type + " {");
            for (Map.Entry<String, String> entry: data.entrySet()) {
                System.out.print('(');
                System.out.print(entry.getKey());
                System.out.print(':');
                System.out.print(entry.getValue());
                System.out.print(')');
            }
            System.err.println('}');
        }
    }

    @Test(groups={"function"})
    public void testUpdator() throws Exception {
        System.err.println("start processing xml data");
        TableUpdator updator = new TableUpdator(new Proc());
        System.err.println("ft05.xml is " + getClass().getResourceAsStream("/FT05.xml"));
        updator.process(getClass().getResourceAsStream("/FT05.xml"));
    }
}
