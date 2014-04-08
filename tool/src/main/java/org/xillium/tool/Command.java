package org.xillium.tool;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.*;


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
            for (Class<?> c: getKnownClasses(PACKAGE)) {
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

    public static List<Class<?>> getKnownClasses(String pkgname) throws IllegalArgumentException, IOException {
        List<Class<?>> classes = new ArrayList<Class<?>>();

        String path = pkgname.replace('.', '/');

        URL resource = ClassLoader.getSystemClassLoader().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Unknown package: " + pkgname);
        }

        File directory = null;
        try {
            directory = new File(resource.toURI());
        } catch (Exception x) {
            directory = null;
        }

        if (directory != null && directory.exists()) {
            String[] files = directory.list();
            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith(".class")) {
                    try {
                        classes.add(Class.forName(pkgname + '.' + files[i].substring(0, files[i].length() - 6)));
                    } catch (ClassNotFoundException x) {
                        //ignore
                    }
                }
            }
        } else {
            JarFile jar = new JarFile(resource.getFile().replaceFirst("[.]jar[!].*", ".jar").replaceFirst("file:", ""));
            try {
                for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(path) && name.length() > (path.length() + "/".length())) {
                        if (entry.isDirectory()) continue;
                        try {
                            classes.add(Class.forName(name.substring(0, name.length() - 6).replace('/', '.').replace('\\', '.')));
                        } catch (ClassNotFoundException x) {
                            //ignore
                        }
                    }
                }
            } finally {
                jar.close();
            }
        }

        return classes;
    }
}

