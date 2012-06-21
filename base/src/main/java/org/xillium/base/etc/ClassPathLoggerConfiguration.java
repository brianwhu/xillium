package org.xillium.base.etc;

import java.io.*;
import java.util.*;
import java.util.logging.*;


public class ClassPathLoggerConfiguration {
    public ClassPathLoggerConfiguration() throws IOException {
		InputStream is = ClassPathLoggerConfiguration.class.getResourceAsStream("/logging.properties");
		try {
			if (is != null) {
				System.out.println("*** ClassPathLoggerConfiguration: Loading /logging.properties");
				LogManager.getLogManager().readConfiguration(is);
			} else {
				System.out.println("*** ClassPathLoggerConfiguration: Can't open /logging.properties");
			}
		} finally {
			is.close();
		}
	}
}
