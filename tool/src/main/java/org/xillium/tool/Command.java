package org.xillium.tool;

import java.util.*;
import org.xillium.base.beans.Beans;


/**
 * An interactive tool
 */
public class Command {
    private static final String PACKAGE = "org.xillium.tool";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: xillium-tool COMMAND [ ARGUMENTS ... ]");
            System.err.println("       xillium-tool --list");
        } else if (args.length == 1 && args[0].equals("--list")) {
            for (Class<?> c: Beans.getKnownClasses(PACKAGE)) {
                if (c == Command.class) continue;
                System.out.println(c.getName().replace(PACKAGE + '.', "\t"));
            }
        } else {
            Class<?> command = null;
            try {
                command = Class.forName("org.xillium.tool."+args[0]);
            } catch (ClassNotFoundException x) {
                System.err.println("Unknown command");
            }
            if (command != null) {
                command.getMethod("main", String[].class).invoke(null, (Object)Arrays.copyOfRange(args, 1, args.length));
            }
        }
    }
}

