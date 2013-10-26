package org.xillium.play.jsoh;

import java.io.*;
import java.net.*;
import java.util.*;

import java.text.ParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.SimpleType;

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
        public final List<Map<String, Object>> binders;

        Response(List<Map<String, Object>> binders) {
            this.binders = binders;
        }
    }

    public JsohServer(TestSuite suite, String[] args, int offset) {
        if (args.length - offset != 1) {
            throw new IllegalArgumentException("Target-specific-arguments: URL");
        }

        // Create the IdcClient - first argument is the IDC connection URL
        _url = args[offset];

        // TODO: connection pooling
    }

    public TestTarget.Request createRequest(String path) {
        return new Request();
    }

    public TestTarget.Response fire(TestTarget.Request request) throws TestFailureException {
        try {
            String path = ((Request)request).binder.get("_path_");
            if (path != null && path.length() > 0) {
                path = _url + path;
                ((Request)request).binder.remove("_path_");
            } else {
                path = _url;
            }
            String method = ((Request)request).binder.get("_method_");
            if (method != null && method.length() > 0) {
                ((Request)request).binder.remove("_method_");
            }

            URLConnection connection = null;
            if (method.equalsIgnoreCase("post")) {
                connection = new URL(path).openConnection();
                connection.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());

                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry: ((JsohServer.Request)request).binder.entrySet()) {
                    sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(entry.getValue(), "UTF-8")).append('&');
                }
                if (sb.length() > 0) sb.setLength(sb.length()-1);
                wr.write(sb.toString());
                wr.flush();
                wr.close();
            } else {
                StringBuilder sb = new StringBuilder(path).append('?');
                for (Map.Entry<String, String> entry: ((JsohServer.Request)request).binder.entrySet()) {
                    sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(entry.getValue(), "UTF-8").replace("+", "%20")).append('&');
                }
                if (sb.length() > 0) sb.setLength(sb.length()-1);
                connection = new URL(sb.toString()).openConnection();
            }

            InputStream in = connection.getInputStream();
			List<Map<String, Object>> binders = new ArrayList<Map<String, Object>>();
            try {
                @SuppressWarnings("unchecked")
                Iterator<Map<String, Object>> iterator = _mapper.readValues(new JsonFactory().createJsonParser(in),
                    MapType.construct(Map.class, SimpleType.construct(String.class), SimpleType.construct(Object.class))
                );
                while (iterator.hasNext()) {
                    binders.add(iterator.next());
                }
            } catch (IOException x) {
                //
            } finally {
                in.close();
            }
            return new JsohServer.Response(binders);
        } catch (MalformedURLException x) {
            throw new TestFailureException(0, x.getMessage(), x);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    private final String _url;
	private final ObjectMapper _mapper = new ObjectMapper();
}
