package org.xillium.play;

import java.io.*;
import java.util.*;

public class TestScript {
    private static final int MAX_ROWS = 12000;
    protected final TestTarget target;

    public TestScript(TestTarget target) {
        this.target = target;
    }

    public String[] fileAsArray(String name) throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        BufferedReader r = new BufferedReader(new FileReader(name));
        try {
            for (String line; (line = r.readLine()) != null; lines.add(line));
        } finally {
            r.close();
        }

        return lines.toArray(new String[lines.size()]);
    }
}
