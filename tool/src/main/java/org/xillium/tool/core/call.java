package org.xillium.tool.core;

import java.io.*;
import java.util.*;
import org.xillium.base.beans.*;
import org.xillium.base.util.*;
import org.xillium.data.DataBinder;
import org.xillium.core.util.RemoteService;


public class call {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: call SERVER SERVICE [ ARGUMENTS ... ]");
            System.exit(0);
        }

        System.err.println("# enter additional parameters below ... (names wrapped by underscores are ignored)");
        DataBinder binder = new DataBinder();
        Reader reader = new InputStreamReader(System.in);
        binder.load(new XilliumProperties(reader));
        reader.close();

        if (args.length == 2) {
            System.out.println(Beans.toString(RemoteService.call(args[0], args[1], binder)));
        } else {
            System.out.println(Beans.toString(RemoteService.call(args[0], args[1], binder, Arrays.copyOfRange(args, 2, args.length))));
        }
    }

}
