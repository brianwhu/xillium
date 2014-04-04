package org.xillium.tool;

import java.util.Arrays;


/**
 * An interactive tool
 */
public class Command {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: xillium-tool COMMAND [ ARGUMENTS ... ]");
            System.exit(0);
        }
        Class.forName("org.xillium.tool."+args[0]).getMethod("main", String[].class).invoke(null, (Object)Arrays.copyOfRange(args, 1, args.length));
    }
}

