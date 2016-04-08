package lab;

import java.util.*;
import org.xillium.base.beans.*;
import org.xillium.base.type.typeinfo;
import org.xillium.base.util.Options;
import org.xillium.base.util.Pair;
import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class OptionsTest {

    public static class Configuration {
        @Options.description("Turn on dynamic compression")
        public boolean dynamic;
        @Options.description("Process files locally (non-recursive)")
        public boolean local;
        @Options.placeholder("compression-level(0-9)")
        @Options.description("Set compression level explicitly, where 9 is the highest level")
        public int compress;
        @Options.placeholder("comma-separated-seach-levels")
        @Options.description("Search at these levels")
        public int[] levels;
        @Options.description("Turn on compatible mode (default is ON)")
        public boolean compatible = true;
        @Options.description("Provide compression data, multiple")
        @typeinfo(String.class)
        public List<String> data = new ArrayList<>();
        @Options.description("Provide DATE data, multiple")
        @typeinfo(java.sql.Date.class)
        public List<java.sql.Date> date = new ArrayList<>();
        @Options.description("Set minimum date boundary")
        @Options.placeholder("minimum-date")
        public java.sql.Date since;
        @Options.description("Set a project name")
        @Options.placeholder("name")
        public String name = "default";
    }

    @Test(groups={"options"})
    public void testOptions() throws Exception {
        Options<Configuration> options = new Options<>(new Configuration());
        List<Pair<Options.Unrecognized, String>> errors = new ArrayList<>();
        int index = options.parse(new String[] {
            "--name=", "-dC", "--3a", "--data=F", "--dynamic", "--data=A", "--since=2015-10-07", "--date=2015-10-10", "--date=2015-12-12", "--compress=9", "--data=", "--levels=2,3,4,18", "--data", "--", "--path"
        }, 0, errors);
        options.document(System.out);
        System.out.println(index);
        System.out.println(Beans.toString(errors));
        System.out.println(Beans.toString(options.get()));

        Configuration c = options.get();
        assert index == 14 : "wrong stop index";
        assert errors.size() == 2;
        assert c.dynamic;
        assert !c.local;
        assert c.compress == 9;
        assert !c.compatible;
        assert c.data.size() == 3;
        assert c.date.size() == 2;
    }
}
