package lab.data.validation;

import lab.*;
import org.xillium.base.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;


public class ValidationTest {
    public static void main(String[] args) throws Exception {
/*
        Dictionary dictionary =
            (Dictionary)new XMLBeanAssembler(new DefaultObjectFactory(), new StandardTrace().setLevel(Level.INFO)).build(args[0]);
        System.err.println("Base Types = ");
        System.err.println(Beans.toString(Dictionary.bases));
*/
        //Trace.g.configure(new StandardTrace()).std.setFilter(Dictionary.class);
        Trace.g.configure(new StandardTrace());
        int slash = args[0].lastIndexOf('/');
        int dot = args[0].lastIndexOf('.');
        String TestCase = args[0].substring(slash+1, dot);

        //Dictionary dictionary = new Dictionary("core").addDataDictionary(StandardDataTypes.class);
        //Dictionary dictionary = (Dictionary)new XMLBeanAssembler(new DefaultObjectFactory(), Trace.g.std).build("lab/validation/dictionary.xml");
        Dictionary dictionary = (Dictionary)new XMLBeanAssembler(new DefaultObjectFactory()).build("lab/data/validation/dictionary.xml");
        System.err.println("Dictionary = " + Beans.toString(dictionary));

        DataBinder binder = new DataBinder();

        DataUtil.loadFromProperties(binder, args[0]);
/*
        Properties props = new Properties();
        props.load(new FileReader(args[0]));
        Enumeration<?> enumeration = props.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String)enumeration.nextElement();
            binder.put(key, props.getProperty(key));
            //System.err.println(key + ": " + binder.getLocal(key));
        }
        System.err.println("Prop = " + Beans.toString(props));
*/

        DataUtil.loadFromArgs(binder, args, 1);
/*
        for (int i = 1; i < args.length; ++i) {
            int equal = args[i].indexOf('=');
            if (equal > 0) {
                binder.put(args[i].substring(0, equal), args[i].substring(equal + 1));
            } else {
                System.err.println("*** Invalid parameter: " + args[i]);
            }
        }
*/

        //Trace.g.std.configure(new NullTrace());
        long now = System.currentTimeMillis();
        DataObject object = dictionary.collect((DataObject)Class.forName("lab.data.validation." + TestCase).newInstance(), binder);
        //for (int i = 0; i < 300; ++i) {
            //btrd = dictionary.collect(btrd, binder);
        //}
        long elapsed = System.currentTimeMillis() - now;
        System.err.println("Time = " + elapsed);
        System.err.println("DataObject = ");
        System.err.println(Beans.toString(object));
    }
}
