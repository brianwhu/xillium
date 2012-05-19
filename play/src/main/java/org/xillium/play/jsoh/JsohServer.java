package org.xillium.play.jsoh;

import java.io.*;
import java.net.*;
import java.util.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.codehaus.jackson.map.ObjectMapper;

import org.xillium.play.TestFailureException;
import org.xillium.play.TestSuite;
import org.xillium.play.TestTarget;


public class JsohServer implements TestTarget {
    //private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("MM/dd/yy");

    public class Request implements TestTarget.Request {
        public final Map<String, String> binder = new HashMap<String, String>();

        public Request set(String key, String value) throws IOException, ParseException {
            if (key.startsWith("bool:")) {
                binder.put(key.substring(5), value);
            } else if (key.startsWith("date:")) {
                binder.put(key.substring(5), value);
            } else if (key.startsWith("file:")) {
                //binder.put(key.substring(5), new TransferFile(new File(value), "application/octet-stream"));
            } else {
                binder.put(key, value);
            }
            return this;
        }
    }

    public class Response implements TestTarget.Response {
        public final Map<String, Object> binder;

        Response(Map<String, Object> binder) {
            this.binder = binder;
        }
    }

    public JsohServer(TestSuite suite, String[] args, int offset) throws MalformedURLException {
        if (args.length - offset != 1) {
            throw new IllegalArgumentException("Target-specific-arguments: URL");
        }

        // Create the IdcClient - first argument is the IDC connection URL
        _url = new URL(args[offset]);

        // TODO: connection pooling
    }

    public TestTarget.Request createRequest(String path) {
        return new Request();
    }

    public TestTarget.Response fire(TestTarget.Request request) throws TestFailureException {
        try {
            URLConnection connection = _url.openConnection();
			connection.setDoOutput(true);
    		OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());

			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, String> entry: ((JsohServer.Request)request).binder.entrySet()) {
				sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(entry.getValue(), "UTF-8")).append('&');
			}
			if (sb.length() > 0) sb.setLength(sb.length()-1);
			wr.write(sb.toString());
    		wr.flush();

            InputStream in = connection.getInputStream();
			Map<String,Object> binder = _mapper.readValue(in, Map.class);
            in.close();
			wr.close();
			//connection.close();

            return new JsohServer.Response(binder);
        //} catch (ServiceException x) {
            //throw new TestFailureException(x.getStatusCode(), x);
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    private final URL _url;
	private final ObjectMapper _mapper = new ObjectMapper();
}
