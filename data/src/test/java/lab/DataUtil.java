package lab;

import org.xillium.data.*;
import java.io.*;
import java.util.*;


public class DataUtil {
    public static void loadFromArgs(DataBinder binder, String[] args, int offset) throws Exception {
        for (int i = offset; i < args.length; ++i) {
            int equal = args[i].indexOf('=');
            if (equal > 0) {
                binder.put(args[i].substring(0, equal), args[i].substring(equal + 1));
            } else {
                System.err.println("*** Invalid parameter: " + args[i]);
            }
        }
    }

    public static void loadFromProperties(DataBinder binder, String filename) throws Exception {
        Properties props = new Properties();
        props.load(new FileReader(filename));
        Enumeration<?> enumeration = props.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String)enumeration.nextElement();
            binder.put(key, props.getProperty(key));
        }
    }
}
