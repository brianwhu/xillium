package org.xillium.tool.core;

import java.io.*;
import java.util.*;
import org.xillium.base.beans.*;
import org.xillium.base.util.*;
import org.xillium.data.DataBinder;
import org.xillium.core.util.RemoteService;


public class call {
    public static void main(String[] args) throws Exception {
        final PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true);

        boolean data = false;
        int argc = args.length - 2;

        if (args.length > 0 && args[0].equals("--data")) {
            data = true;
            --argc;
        }

        if (argc < 0) {
            System.err.println("Usage: core.call [ --data ] SERVER SERVICE [ ARGUMENTS ... ]");
            System.exit(0);
        }

        if (data) {
            //System.err.println("# enter additional parameters below ... (names wrapped by underscores are ignored)");
            DataBinder binder = new DataBinder();
            Reader reader = new InputStreamReader(System.in, "UTF-8");
            binder.load(new XilliumProperties(reader));
            reader.close();

            if (argc == 0) {
                out.println(Beans.toString(RemoteService.call(args[1], args[2], binder)));
            } else {
                out.println(Beans.toString(RemoteService.call(args[1], args[2], binder, Arrays.copyOfRange(args, 3, args.length))));
            }
        } else {
            if (argc == 0) {
                out.println(Beans.toString(RemoteService.call(args[0], args[1])));
            } else {
                out.println(Beans.toString(RemoteService.call(args[0], args[1], Arrays.copyOfRange(args, 2, args.length))));
            }
        }

    }

}
