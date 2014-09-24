package org.xillium.core.util;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import org.xillium.base.util.Bytes;
import org.xillium.data.DataObject;
import org.xillium.data.DataBinder;
import org.xillium.data.CachedResultSet;
import org.xillium.core.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.deser.std.*;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;


/**
 * An interface to a remote Xillium service.
 */
public class RemoteService {
    private static final Logger _logger = Logger.getLogger(RemoteService.class.getName());
    private static final boolean _urlencoding = System.getProperty("xillium.service.remote.DisableURLEncoding") == null;
    private static final ObjectMapper _mapper = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(new SimpleModule("PureStringDeserializerModule").addDeserializer(String.class, new PureStringDeserializer()));

    /**
     * This class represents a response from a remote Xillium service.
     */
    public static class Response {
        public Map<String, String> params;
        public Map<String, Object> values;
        public Map<String, CachedResultSet> tables;
        public transient byte[] body;

        Response setResponseBody(byte[] body) {
            this.body = body;
            return this;
        }

        /**
         * Stores a String value from <code>params</code> into a DataBinder under a new name.
         */
        public Response store(DataBinder binder, String target, String original) {
            String value = params.get(original);
            if (value != null) {
                binder.put(target, value);
            }
            return this;
        }

        /**
         * Stores the content of this response into a DataBinder.
         */
        public Response store(DataBinder binder) {
            try {
                for (Map.Entry<String, String> entry: params.entrySet()) {
                    binder.put(entry.getKey(), entry.getValue());
                }
                if (values != null) for (Map.Entry<String, Object> entry: values.entrySet()) {
                    binder.put(entry.getKey(), "json:" + _mapper.writeValueAsString(entry.getValue()));
                }
                for (Map.Entry<String, CachedResultSet> entry: tables.entrySet()) {
                    binder.putResultSet(entry.getKey(), entry.getValue());
                }
            } catch (JsonProcessingException x) {
                throw new RuntimeException(x.getMessage(), x);
            }
            return this;
        }
    }

    /**
     * Calls a remote service with non-static member values in the given DataObject as arguments.
     */
    public static Response call(String server, String service, DataObject data, String... params) {
        return call(server, service, false, data, params);
    }

    /**
     * Calls a remote service with non-static member values in the given DataObject as arguments.
     */
    public static Response call(String server, String service, boolean suppress, DataObject data, String... params) {
        List<String> list = new ArrayList<String>(Arrays.asList(params));
        for (Field field: data.getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            try {
                Object value = field.get(data);
                if (value == null) value = "";
                if (_urlencoding) {
                    try {
                        list.add(field.getName() + '=' + URLEncoder.encode(value.toString(), "UTF-8"));
                    } catch (UnsupportedEncodingException x) {
                        _logger.log(Level.WARNING, value.toString(), x);
                    }
                } else {
                    list.add(field.getName() + '=' + value);
                }
            } catch (IllegalAccessException x) {}
        }
        return call(server, service, suppress, list.toArray(new String[list.size()]));
    }

    /**
     * Calls a remote service with parameters in the given DataBinder as well as in an String list.
     *
     * Note: parameters in the data binder whose names start with '_' or '#' are NOT passed to the remote service.
     */
    public static Response call(String server, String service, DataBinder binder, String... params) {
        return call(server, service, false, binder, params);
    }

    /**
     * Calls a remote service with parameters in the given DataBinder as well as in an String list.
     *
     * Note: parameters in the data binder whose names start with '_' or '#' are NOT passed to the remote service.
     */
    public static Response call(String server, String service, boolean suppress, DataBinder binder, String... params) {
        List<String> list = new ArrayList<String>(Arrays.asList(params));
        for (Map.Entry<String, String> entry: binder.entrySet()) {
            String name = entry.getKey();
            if (name.charAt(0) == '_' || name.charAt(0) == '#') continue;
            if (_urlencoding) {
                try {
                    list.add(name + '=' + URLEncoder.encode(entry.getValue(), "UTF-8"));
                } catch (UnsupportedEncodingException x) {
                    _logger.log(Level.WARNING, entry.getValue(), x);
                }
            } else {
                list.add(name + '=' + entry.getValue());
            }
        }
        return call(server, service, suppress, list.toArray(new String[list.size()]));
    }

    /**
     * Calls a remote service with a list of "name=value" string values as arguments.
     */
    public static Response call(String server, String service, String... params) {
        return call(server, service, false, params);
    }

    /**
     * Calls a remote service with a list of "name=value" string values as arguments.
     */
    public static Response call(String server, String service, boolean suppress, String... params) {
        try {
            URL url = new URL(server + '/' + service);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "utf-8"));
            for (String param: params) {
                _logger.fine(param);
                pw.print(param); pw.print('&');
            }
            pw.close();
            InputStream in = connection.getInputStream();
            try {
                byte[] bytes = Bytes.read(in);
                try {
                    Response response = _mapper.readValue(bytes, Response.class).setResponseBody(bytes);
                    if (response.params == null) {
                        throw new ServiceException("***ProtocolErrorMissingParams");
                    } else if (!suppress) {
                        String message = response.params.get(Service.FAILURE_MESSAGE);
                        if (message != null && message.length() > 0) {
                            throw new RemoteServiceException(message);
                        }
                    }
                    return response;
                } catch (JsonProcessingException x) {
                    _logger.log(Level.WARNING, new String(bytes, "UTF-8"));
                    throw x;
                }
            } finally {
                in.close();
            }
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new ServiceException("***RemoteServiceCallFailure", x);
        }
    }

    /*#
     * A PureStringDeserializer is a Jackson string deserializer that ignores and skips any non-string JSON specifications.
     */
    private static class PureStringDeserializer extends StdScalarDeserializer<String> {
        private static final UntypedObjectDeserializer _utod = new UntypedObjectDeserializer();

        PureStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            try {
                return StringDeserializer.instance.deserialize(parser, context);
            } catch (JsonMappingException x) {
                _utod.deserialize(parser, context);
                return null;
            }
        }

        @Override
        public String deserializeWithType(JsonParser parser, DeserializationContext context, TypeDeserializer deserializer) throws IOException {
            return deserialize(parser, context);
        }

        private static final long serialVersionUID = 5045214689420593104L;
    }
}
