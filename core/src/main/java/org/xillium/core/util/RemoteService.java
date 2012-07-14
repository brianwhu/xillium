package org.xillium.core.util;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import org.xillium.base.etc.Arrays;
import org.xillium.data.DataObject;
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
        public transient byte[] body;

        Response setResponseBody(byte[] body) {
            this.body = body;
            return this;
        }
    }

	public static Response call(String server, String service, DataObject data) {
        List<String> params = new ArrayList<String>();
        for (Field field: data.getClass().getFields()) {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
            try {
                params.add(field.getName() + '=' + field.get(data).toString());
            } catch (IllegalAccessException x) {}
        }
        return call(server, service, params.toArray(new String[params.size()]));
    }

	public static Response call(String server, String service, String... params) {
		try {
            URL url = new URL(server + '/' + service);
			//System.err.println("Calling " + url);
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
                //return _mapper.readValue(in, Response.class);
                byte[] bytes = Arrays.read(in);
                return _mapper.readValue(bytes, Response.class).setResponseBody(bytes);
            } finally {
                in.close();
            }
		} catch (Exception x) {
			//x.printStackTrace();
            throw new RuntimeException("RemoteServiceCallFailure", x);
		}
	}
}
