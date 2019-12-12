package org.xillium.tool;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.*;
import org.xillium.base.Throwing;
import org.xillium.base.util.*;


/**
 * An interactive tool
 */
public class Command {
    private static final String PACKAGE = "org.xillium.tool";

    public static class Control {
        @Options.description("List available commands")
        public boolean list;
        @Options.description("Load additinal classes dynamically from the given classpath")
        @Options.placeholder("classpath")
        public String classpath;
    }

    public static void main(String[] args) throws Exception {
        Options<Control> options = new Options<>(new Control());
        List<Pair<Options.Unrecognized, String>> issues = new ArrayList<>();
        int index = options.parse(args, 0, issues);

        if (issues.size() > 0) {
            for (Pair<Options.Unrecognized, String> issue: issues) {
                System.err.println((issue.first == Options.Unrecognized.ARGUMENT ? "Unknown option: " : "Bad value: ") + issue.second);
            }
        } else if (args.length - index == 0) {
            if (options.get().list) {
                for (Class<?> c: getKnownClasses(PACKAGE)) {
                    if (c == Command.class) continue;
                    System.out.println(c.getName().replace(PACKAGE + '.', "\t"));
                }
            } else {
                System.err.println("Usage: xillium-tool { -l | --list }");
                System.err.println("       xillium-tool [ options ] COMMAND [ ARGUMENTS ...]");
                options.document(System.err);
            }
        } else {
            if (options.get().classpath != null) {
                String[] paths = options.get().classpath.split(System.getProperty("path.separator"));
                Thread.currentThread().setContextClassLoader(new URLClassLoader(
                    Arrays.stream(paths).map(Throwing.function(p -> new File(p).toURI().toURL())).toArray(URL[]::new),
                    Thread.currentThread().getContextClassLoader()
                ));
            }

            Class<?> command = null;
            try {
                command = Class.forName("org.xillium.tool."+args[index]);
            } catch (ClassNotFoundException x) {
                System.err.println("Unknown command");
            }
            if (command != null) {
                command.getMethod("main", String[].class).invoke(null, (Object)Arrays.copyOfRange(args, index+1, args.length));
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
                    if (name.startsWith(path) && name.indexOf('$') < 0 && name.length() > (path.length() + "/".length())) {
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

