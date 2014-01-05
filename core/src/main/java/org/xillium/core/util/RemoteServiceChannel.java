package org.xillium.core.util;

import java.net.URLEncoder;
import java.util.Properties;
import java.util.NoSuchElementException;
import javax.mail.*;
import javax.mail.internet.*;
import org.xillium.data.DataBinder;


/**
 * An implementation of MessageChannel that uses SMTP as the means of message delivery.
 */
public class RemoteServiceChannel implements MessageChannel {
    private static final String HOST = "xillium.service.host";
    private static final String PATH = "xillium.service.path";

    private Properties _properties;
    private String _host;
    private String _path;

    /**
     * Constructs an RemoteServiceChannel that is to be configured later.
     */
    public RemoteServiceChannel() {
    }

    /**
     * Constructs an RemoteServiceChannel and configures it with given properties.
     */
    public RemoteServiceChannel(Properties p) {
        setProperties(p);
    }

    /**
     * Configures the RemoteServiceChannel with given properties.
     */
    public void setProperties(Properties p) {
        _properties = p;
        _host = (String)p.remove(HOST);
        if (_host == null) throw new NoSuchElementException(HOST);
        _path = (String)p.remove(PATH);
        if (_path == null) throw new NoSuchElementException(PATH);
    }

    @Override
    public String sendMessage(String subject, String message) {
        if (_properties != null) {
            try {
                DataBinder binder = new DataBinder();
                for (String name: _properties.stringPropertyNames()) {
                    binder.put(name, URLEncoder.encode(_properties.getProperty(name), "UTF-8"));
                }
                binder.put("subject", URLEncoder.encode(subject));
                binder.put("message", URLEncoder.encode(message));
                RemoteService.call(_host, _path, binder);
            } catch (Exception x) {
                return x.getMessage();
            }
        }
        return null;
    }
}
