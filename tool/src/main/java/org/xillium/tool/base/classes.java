package org.xillium.tool.base;

import java.util.*;
import org.xillium.base.beans.*;
import org.xillium.tool.Command;
//import org.xillium.base.util.*;
//import org.xillium.data.DataBinder;
//import org.xillium.core.util.RemoteService;


public class classes {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: listclasses PACKAGE-NAME ...");
        } else {
            for (int i = 0; i < args.length; ++i) {
                for (Class<?> c: Command.getKnownClasses(args[i])) {
                    System.out.print('\t'); System.out.println(c.getName());
                }
            }
        }
    }
}
