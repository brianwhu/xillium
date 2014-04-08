package org.xillium.tool.base;

//import java.io.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.*;
import java.util.jar.*;
import org.xillium.base.beans.*;
//import org.xillium.base.util.*;
//import org.xillium.data.DataBinder;
//import org.xillium.core.util.RemoteService;


public class classes {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: listclasses PACKAGE-NAME ...");
        } else {
            for (int i = 0; i < args.length; ++i) {
                for (Class<?> c: getKnownClasses(args[i])) {
                    System.out.print('\t'); System.out.println(c.getName());
                }
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
