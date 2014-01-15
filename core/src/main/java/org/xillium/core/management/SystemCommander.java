package org.xillium.core.management;

import java.lang.reflect.Field;
import java.io.*;
import java.util.*;
import org.xillium.base.beans.*;
import org.xillium.data.*;
import org.xillium.data.validation.*;


/**
 * SystemCommander retrieves OS bean information into a DataBinder.
 * <ul>
 * <li>a(async)     asynchronous processing</li>
 * <li>f(forward)   forward</li>
 * <li>x(exec)      execute</li>
 * </ul>
 */
public class SystemCommander {
    private static final String MESSAGE = "jos.exception";
    private static final String OUTPUT  = "jos.output";

    private final DataBinder _binder;
    private int _sequence;
    private boolean _async;
    private boolean _verbose;
    private String _address;

    public SystemCommander(DataBinder binder) {
        _binder = binder;
    }

    public SystemCommander v(boolean verbose) {
        _verbose = verbose;
        return this;
    }

    public SystemCommander a(boolean async) {
        _async = async;
        return this;
    }

    public SystemCommander f(String address) {
        _address = address;
        return this;
    }

    public SystemCommander x(String line) {
        try {
            String[] args = line.split(" +");
            Process process = new ProcessBuilder(Arrays.asList(args)).redirectErrorStream(true).start();
            Object sequence = "#" + _sequence++;
            try {
                Field field = process.getClass().getDeclaredField("pid");
                field.setAccessible(true);
                sequence = field.get(process);
            } catch (Exception x) {
            }
            if (!_async) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                try {
                    StringBuilder output = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append('\n');
                    }
                    if (output.length() > 0) output.setLength(output.length() - 1);
                    _binder.put(OUTPUT + '[' + args[0] + '(' + sequence + ")]", output.toString());
                } finally {
                    reader.close();
                }
            } else {
                process.getInputStream().close();
            }
        } catch (Exception x) {
            _binder.put(MESSAGE, _verbose ? Throwables.getFullMessage(x) : Throwables.getExplanation(x));
        }
        return this;
    }
}
