package org.xillium.play;

import org.xillium.play.mbean.*;
import java.io.*;
import java.util.*;


public class TestSuite {
    public static final String PREFIX_ = "test.";

    private static final String TARGET = "target";
    private static final String SCRIPT = "script";
    private static final String CASES = "cases";
    private static final String SCALE = "scale";

    /**
     * @param prop
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
	public TestSuite(String prop) throws IOException {
        _summary = new TestSummary(this);

        Properties properties = new Properties();
        FileInputStream fis = new FileInputStream(prop);
        properties.load(fis);
        fis.close();

        try {
            _target = (Class<? extends TestTarget>)Class.forName(properties.getProperty(PREFIX_ + TARGET));
        } catch (ClassNotFoundException x) {
            throw new IOException(x);
        }

        String script = properties.getProperty(PREFIX_ + SCRIPT);
        if (script != null) {
            try {
                _script = (Class<? extends TestScript>)Class.forName(script);
            } catch (ClassNotFoundException x) {
                throw new IOException(x);
            }
        } else {
            _script = TestScript.class;
        }

        double scale = 1;
        try {
            scale = Double.parseDouble(properties.getProperty(PREFIX_ + SCALE));
        } catch (Exception x) {
        }

        String testcases = properties.getProperty(PREFIX_ + CASES);
        if (testcases == null || testcases.length() == 0) {
            throw new RuntimeException("No test cases specified");
        } else {
            for (String name: testcases.split(" *, *")) {
                try {
                    _size += addTestCase(properties, name, scale).getSize();
                } catch (Exception x) {
                    System.err.println("*** Test case '" + name + "' not properly specified: " + x.getMessage());
                    x.printStackTrace();
                }
            }
        }
    }

    /**
     * @param properties
     * @param name
     * @param scale
     * @return the newly added test case
     */
    protected TestCase addTestCase(Properties properties, String name, double scale) {
        try {
            TestCase tc = new TestCase(name, scale, properties);
            _cases.add(tc);
            _summary.addTestCaseStats(tc.getStats());
            return tc;
        } catch (NullPointerException x) {
            throw new RuntimeException("Test case " + name + ": No arguments specified", x);
        }
    }

    /**
     * @return
     */
    public Class<? extends TestTarget> getTargetClass() {
        return _target;
    }

    /**
     * @return
     */
    public Class<? extends TestScript> getScriptClass() {
        return _script;
    }

    /**
     * @return a list of test cases in this suite
     */
    public List<TestCase> getTestCases() {
        return _cases;
    }

    /**
     * @return the number of test cases in this suite
     */
    public int getSize() {
        return _size;
    }

    /**
     * @return
     */
    public TestSummary getTestSummary() {
        return _summary;
    }

    public void stop() {
        Iterator<TestCase> it = _cases.iterator();
        while (it.hasNext()) it.next().stop();
    }

    private final Class<? extends TestTarget> _target;
    private final Class<? extends TestScript> _script;
    private final List<TestCase> _cases = new ArrayList<TestCase>();
    private final TestSummary _summary;
    private int _size;
}
