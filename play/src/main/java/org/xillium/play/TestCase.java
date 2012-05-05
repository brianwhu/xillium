package org.xillium.play;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;

import org.xillium.play.mbean.TestCaseStats;


public class TestCase implements Runnable {
    private static final int MAX_ERRORS = 4;
    private static final String _SIZE = ".size";
    private static final String _ACTIONS = ".actions";
    private static final String[] PROP = {  // (name, type)
        "ramp", "i",
        "nice", "i",
        "wait", "i",
        "retry", "b",
        "ignore", "s"
    };

    private final Logger _log = Logger.getLogger(TestCase.class.getName());
    private final String _name;
    private final int _size;
    private final Map<String, Object> _properties = new HashMap<String, Object>();
    private final List<TestAction> _actions = new ArrayList<TestAction>();
    private final TestCaseStats _stats;

    private TestTarget _target;
    private ScriptEngine _engine;
    volatile boolean _active;

    /**
     * Constructs a TestCase with a given name and the global scale level. Test case details are to be retrived from the properties object.
     * 
     * @param name
     * @param scale
     * @param properties
     */
    public TestCase(String name, double scale, Properties properties) {
        _name = name;
        _stats = new TestCaseStats(_name);

        int size = 0;
        try {
            size = Integer.parseInt(properties.getProperty(TestSuite.PREFIX_ + name + _SIZE));
        } catch (Exception x) {
            size = 1;
        }
        _size = (int)Math.round(size*scale);

        // load properties
        for (int i = 0; i < PROP.length; ++i) {
            String prop = PROP[i];
            try {
                String value = properties.getProperty(TestSuite.PREFIX_ + name + '.' + prop);
                if (value == null) {
                    value = properties.getProperty(TestSuite.PREFIX_ + prop);
                }
                ++i;
                if (PROP[i].charAt(0) == 'i') {
                    setProperty(prop, Integer.valueOf(value));
                } else if (PROP[i].charAt(0) == 'c') {
                    setProperty(prop, Character.valueOf(value.charAt(0)));
                } else if (PROP[i].charAt(0) == 'b') {
                    setProperty(prop, Boolean.valueOf(value));
                } else if (PROP[i].charAt(0) == 's') {
                    setProperty(prop, value);
                }
            } catch (Exception x) {
                System.err.println(name + ": property '" + prop + "' missing or misconfigured. (ignored)");
            }
        }

        // activities
        String actions = properties.getProperty(TestSuite.PREFIX_ + name + _ACTIONS);
        _log.info("TestCase: actions = " + actions);
        if (actions != null) {
            for (String action: actions.split(" *, *")) {
                addAction(action, properties);
            }
        } else {
            addAction(null, properties);
        }

        System.err.println("\tTestCase: " + name + '/' + size);
    }

    /**
     * @return the name of this test case
     */
    public String getName() {
        return _name;
    }

    /**
     * @return the size (thread count) of this test case
     */
    public int getSize() {
        return _size;
    }

    /**
     * @return the TestCaseStats object
     */
    public TestCaseStats getStats() {
        return _stats;
    }

    /**
     * Retrieves a property of this test case.
     * 
     * @param <T> - the type of the property's value
     * @param name - the name of the property
     * @param def - a default value of the property is not found
     * @return the property's value
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name, T def) {
        T value = (T)_properties.get(name);
        if (value == null) {
            return def;
        } else {
            return value;
        }
    }

    private void setProperty(String name, Object value) {
        _properties.put(name, value);
        System.err.println("\t\tpro: " + name + '=' + value);
    }

    private void addAction(String name, Properties properties) {
        _actions.add(new TestAction(_name, name, properties));
    }

    /**
     * Starts the test case in a separate worker thread. Returns the worker thread.
     * 
     * @param target
     * @param engine
     * @return
     */
    public Thread start(TestTarget target, ScriptEngine engine) {
        _target = target;
        _engine = engine;
        _active = true;
        Thread worker = new Thread() {
            public void run() {
                int ramp = getProperty("ramp", 1);
                System.err.println("Starting Test Case: " + getName());
                for (int i = 0; i < getSize(); ++i) {
                    new Thread(TestCase.this).start();
                    if (ramp > 0) {
                        synchronized (this) {
                            try {
                                wait(ramp);
                            } catch (Exception x) {}
                        }
                    }
                }
                System.err.println("Test Case: " + getName() + "\tstarted in " + getSize() + " threads");
            }
        };
        worker.start();
        return worker;
    }

    /**
     * Stops the test case.
     */
    public synchronized void stop() {
        _active = false;
        notifyAll();
    }

    public void run() {
        int maxErrors = getProperty("errors", MAX_ERRORS);
        int nice = getProperty("nice", 1);
        int wait = getProperty("wait", 1000);
        Pattern ignore = null;
        try {
            ignore = Pattern.compile(getProperty("ignore", ""));
            System.err.println("\tignoring pattern " + ignore.pattern());
        } catch (Exception x) {
            _log.log(Level.INFO, "TestCase: invalid 'ignore' setting", x);
        }

        int errors = 0;
        _stats.updateThreadCount(+1);
        TestLoop:
        while (_active) {
            try {
                Object context = _engine.eval("new Object()");
                synchronized (this) {
                    try { wait(nice); } catch (Throwable t) {}
                }
                for (TestAction action: _actions) {
                    long start = action.run(context, _target, _engine);
                    if (start == 0) {
                        break TestLoop;
                    } else {
                        _stats.addSuccessPeg(System.currentTimeMillis() - start);
                    }
                }
                errors = 0;
            } catch (TestFailureException x) {
                //String prefix = new StringBuilder("*** ").append(Thread.currentThread().getName()).append(": ").toString();
                String message = x.getMessage();
                if (message != null) {
                    if (ignore != null && ignore.matcher(message).matches()) {
                        //System.err.println(prefix + "(ignored) " + message);
                        _log.info(_name + ": (ignored) " + message);
                        continue; // ignored
                    } else if (message.length() > 128) {
                        message = message.substring(0, 128);
                    }
                } else {
                    message = x.getClass().getName() + "(No message)";
                }
                _stats.addFailurePeg(x);
                ++errors;
                //System.err.println(prefix + message);
                _log.warning(_name + ": " + message);
                if (errors >= maxErrors || wait < 0) {
                    //System.err.println(prefix + "Give up");
                    _log.warning(_name + ": Give up");
                    break TestLoop;
                } else if (wait > 0) {
                    int pause = wait * errors;
                    //System.err.println(prefix + "Waiting for " + pause + " milliseconds before trying again");
                    _log.info(_name + ": Waiting for " + pause + " milliseconds before trying again");
                    //try { wait(pause); } catch (Throwable t) {}
                    synchronized (this) { try { wait(pause); } catch (Throwable t) {} }
                }
            } catch (Exception x) {
                x.printStackTrace();
                break TestLoop;
            }
        }
        _stats.updateThreadCount(-1);
    }

    public void dump(PrintStream ps) {
    }
}
