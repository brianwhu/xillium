package org.xillium.tool;

import java.util.Arrays;

//import java.lang.reflect.*;


/**
 * An interactive tool
 */
public class Command {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: xillium-tool NAMESPACE COMMAND [ ARGUMENTS ... ]");
            System.exit(0);
        }
        Class.forName("org.xillium.tool."+args[0]+'.'+args[1]).getMethod("main", String[].class).invoke(null, (Object)Arrays.copyOfRange(args, 2, args.length));
        //Method m = Class.forName("org.xillium.tool." + args[0] + '.' + args[1]).getMethod("main", String[].class);
        //String[] a = Arrays.copyOfRange(args, 2, args.length);
        //m.invoke(null, (Object)a);
    }
}

