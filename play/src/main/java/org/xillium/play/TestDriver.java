package org.xillium.play;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import javax.script.*;
           

public class TestDriver {
    private static void printUsageHelp () {
        System.err.println("Usage: xillium-play TEST-SUITE TARGET-SPECIFIC-ARGUMENTS ...");
    }

    /**
     * Usage: Search idc-url username/password target
     */
    public static void main(String[] args) throws IOException,
                                                  NoSuchMethodException,
                                                  InstantiationException,
                                                  IllegalAccessException,
                                                  InvocationTargetException {
        if (args.length == 0) {
            printUsageHelp();
            System.exit(0);
        }

        // Load test suite
        TestSuite suite = new TestSuite(args[0]);

        // Create the test target as specified in the test suite
        TestTarget target = null;
        try {
            target = suite.getTargetClass().getConstructor(TestSuite.class, String[].class, Integer.TYPE).newInstance(suite, args, 1);
        } catch (InvocationTargetException x) {
            if (x.getCause() != null && x.getCause().getClass() == IllegalArgumentException.class) {
                System.err.println(x.getCause().getMessage());
                System.exit(0);
            } else {
                throw x;
            }
        }

        // Create a scrpting engine to execute the script files
        System.err.println("Preparing the test environment ...");

        ScriptEngine js = new ScriptEngineManager().getEngineByName("JavaScript");

        // Add a TestScript object into the scripting environment
        js.put("play", suite.getScriptClass().getConstructor(TestTarget.class).newInstance(target));

        // Evaluate the global script file
        evaluatePreTestScript(js, args[0], null);

        System.err.println("Starting test suite ...");

        // Evaluate the per-test-case script file
        Iterator<TestCase> it = suite.getTestCases().iterator();
        while (it.hasNext()) {
            evaluatePreTestScript(js, args[0], it.next().getName());
        }

        // Now start the test cases
        List<Thread> threads = new ArrayList<Thread>();
        suite.getTestSummary().reset();
        it = suite.getTestCases().iterator();
        while (it.hasNext()) {
            threads.add(it.next().start(target, js));
        }

        Iterator<Thread> it2 = threads.iterator();
        while (it2.hasNext()) {
            try { it2.next().join(); } catch (Throwable t) {}
        }

        System.err.println("\nTests are in progress ...");
    }

    private static void evaluatePreTestScript(ScriptEngine js, String testsuite, String testcase) throws IOException {
        String suffix = (testcase == null) ? ".js" : "." + testcase + ".js";
        int dot = testsuite.lastIndexOf('.');
        File script = (dot > 0) ? new File(testsuite.substring(0, dot) + suffix) : new File(testsuite + suffix);
        if (script.canRead()) {
            try {
                js.eval(new InputStreamReader(new FileInputStream(script)));
            } catch (ScriptException x) {
                x.printStackTrace();
            }
        }
    }
}
