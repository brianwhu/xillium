package org.xillium.core.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import org.xillium.data.CachedResultSet;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
public class RemoteService {
    private static final Logger _logger = Logger.getLogger(RemoteService.class.getName());
    private static final ObjectMapper _mapper = new ObjectMapper();

    public static class Response {
        public Map<String, Object> params;
        public Map<String, CachedResultSet> tables;
    }

	public static Response call(String server, String service, String... params) {
		try {
            URL url = new URL(server + '/' + service);
			System.err.println("Calling " + url);
			URLConnection connection = url.openConnection();
			connection.setDoOutput(true);
			PrintWriter pw = new PrintWriter(connection.getOutputStream());
			for (String param: params) {
                _logger.fine(param);
				pw.print(param); pw.print('&');
			}
			pw.close();
            InputStream in = connection.getInputStream();
            try {
                return _mapper.readValue(in, Response.class);
            } finally {
                in.close();
            }
		} catch (Exception x) {
			//x.printStackTrace();
            throw new RuntimeException("RemoteServiceCallFailure", x);
		}
	}
}
