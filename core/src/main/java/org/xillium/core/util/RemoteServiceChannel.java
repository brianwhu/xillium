package org.xillium.core.util;

import java.net.URLEncoder;
import java.util.Properties;
import java.util.NoSuchElementException;
import javax.mail.*;
import javax.mail.internet.*;
import org.xillium.data.DataBinder;


/**
 * An implementation of MessageChannel that uses a Xillium Service as the means of message delivery.
 */
public class RemoteServiceChannel extends PropertiesConfigured implements MessageChannel {
    private static final String HOST = "xillium.service.host";
    private static final String PATH = "xillium.service.path";

    private String SUBJECT = "subject";
    private String MESSAGE = "message";

    private Properties _properties;
    private String _host;
    private String _path;

    /**
     * Constructs a RemoteServiceChannel that is to be configured later.
     */
    public RemoteServiceChannel() {
    }

    /**
     * Constructs a RemoteServiceChannel and configures it with given properties.
     */
    public RemoteServiceChannel(Properties p) {
        super(p);
    }

    /**
     * Constructs a RemoteServiceChannel and configures it with given properties.
     */
    public RemoteServiceChannel(String path) {
        super(path);
    }

    /**
     * Sets the name to use to send the "subject" parameter.
     */
    public void setSubjectParameterName(String s) {
        SUBJECT = s;
    }

    /**
     * Sets the name to use to send the "message" parameter.
     */
    public void setMessageParameterName(String m) {
        MESSAGE = m;
    }

    /**
     * Configures the RemoteServiceChannel with given properties.
     */
    protected void configure(Properties p) {
        try {
            _host = (String)p.remove(HOST);
            if (_host == null) throw new NoSuchElementException(HOST);
            _path = (String)p.remove(PATH);
            if (_path == null) throw new NoSuchElementException(PATH);
            _properties = p;
        } catch (Exception x) {}
    }

    @Override
    public void sendMessage(String subject, String message) {
        if (_properties != null) {
            try {
                DataBinder binder = new DataBinder();
                for (String name: _properties.stringPropertyNames()) {
                    binder.put(name, URLEncoder.encode(_properties.getProperty(name), "UTF-8"));
                }
                if (SUBJECT != null && SUBJECT.length() > 0) binder.put(SUBJECT, URLEncoder.encode(subject, "UTF-8"));
                binder.put(MESSAGE, URLEncoder.encode(message, "UTF-8"));
                RemoteService.call(_host, _path, binder);
            } catch (Exception x) {}
        }
    }
}
