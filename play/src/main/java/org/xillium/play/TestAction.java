package org.xillium.play;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

public class TestAction {
    private static final String _PATH = ".path";
    private static final String _ARGS = ".args";
    private static final String SCRIPT_START = "{{";
    private static final String SCRIPT_END = "}}";

    private final Logger _log = Logger.getLogger(TestAction.class.getName());
    private final String _case, _name, _path;
    private final Map<String, String> _args = new HashMap<String, String>();

    /**
     * @param caseName
     * @param name
     * @param properties
     * @throws NullPointerException
     */
    public TestAction(String caseName, String name, Properties properties) throws NullPointerException {
        _case = caseName;
        _name = name;
        
        String prefix = (name == null) ? TestSuite.PREFIX_ + caseName : TestSuite.PREFIX_ + caseName + '.' + name;
        _path = properties.getProperty(prefix + _PATH);
        // args must present - throw NullPointerException otherwise
        for (String arg: properties.getProperty(prefix + _ARGS).split(" *, *")) {
            int colon = arg.indexOf(':'); // type indicator
            String key = (colon > 0) ? arg.substring(colon+1) : arg;
            if (arg.charAt(arg.length()-1) == '*') {
                key = key.substring(0, key.length()-1);
            }
            addArgument(arg, properties.getProperty(prefix + _ARGS + '.' + key));
        }
    }

    public String getName() {
        return _name;
    }

    /**
     * Runs this action.
     *
     * @param context - a script object that exists for the duration of the test case
     * @param target - test target
     * @param engine - script engine
     * @return the timestamp when the request was sent; 0 if the action is to end and no request was sent.
     * @throws Exception
     */
    public long run(Object context, TestTarget target, ScriptEngine engine) throws Exception {
        Object listener = (_name != null) ? engine.eval(_case + '.' + _name) : engine.getBindings(ScriptContext.ENGINE_SCOPE).get(_case);

        TestTarget.Request request = target.createRequest(_path);
        Iterator<Map.Entry<String, String>> it = _args.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();

            // Is this a mandatory argument?
            boolean mandatory = false;
            String key = e.getKey();
            int length = key.length();
            if (key.charAt(length - 1) == '*') {
                mandatory = true;
                key = key.substring(0, length - 1);
            }

            String value = e.getValue();

            StringBuilder sb = new StringBuilder();
            int base = 0;
            while (base < value.length()) {
                int top = value.indexOf(SCRIPT_START, base);
                if (top >= 0) {
                    int end =
                        value.indexOf(SCRIPT_END, top + SCRIPT_START.length());
                    if (end < 0) {
                        //throw new ScriptException("Unbalanced script quoting starting at position " + top);
                        // not scripting?
                        break;
                    } else {
                        sb.append(value.substring(base, top));
                        Object evaluation = engine.eval(value.substring(top + SCRIPT_START.length(), end));
                        if (mandatory && evaluation == null) {
                            // end of test data
                            _log.info(_name + ": End of test data");
                            return 0;
                        }
                        sb.append(evaluation);
                        base = end + SCRIPT_END.length();
                    }
                } else {
                    break;
                }
            }
            sb.append(value.substring(base));
            //System.err.println(key + " = " + sb);

            request.set(key, sb.toString());
        }
        _log.fine("Calling prescript");
        try {
            ((Invocable)engine).invokeMethod(listener, "prepare", context, request);
        } catch (Exception x) {
            x.printStackTrace(System.err);
        }
        _log.fine("Finished prescript");
        _log.fine(_case + '#' + _name + ": Sending request " + request);
        long start = System.currentTimeMillis();
        TestTarget.Response response = target.fire(request);
        _log.fine("Calling POSTSCRIPT");
        try {
            ((Invocable)engine).invokeMethod(listener, "process", context, response);
        } catch (Exception x) {
            x.printStackTrace(System.err);
        }
        _log.fine("Finished POSTSCRIPT");
        return start;
    }

    public void dump(PrintStream ps) {
    }

    protected void addArgument(String name, String value) {
        _args.put(name, (value != null) ? value : "");
        System.err.println("\t\targ: " + name + '=' + value);
    }
}
