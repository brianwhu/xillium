package org.xillium.core.util;

import java.io.*;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import org.xillium.base.util.XilliumProperties;


/**
 * An implementation of MessageChannel that uses SMTP as the means of message delivery.
 */
public class EmailChannel extends PropertiesConfigured implements MessageChannel {
    private Authenticator _authenticator;
    private Properties _properties;
    private String[] _recipients;

    /**
     * Constructs an EmailChannel that is to be configured later.
     */
    public EmailChannel() {
    }

    /**
     * Constructs an EmailChannel and configures it with given properties.
     */
    public EmailChannel(Properties properties) {
        super(properties);
    }

    /**
     * Constructs an EmailChannel and configures it with given properties file.
     */
    public EmailChannel(String path) {
        super(path);
    }

    /**
     * Configures the EmailChannel with given properties.
     */
    protected void configure(Properties properties) {
        try {
            _recipients = properties.getProperty("mail.smtp.to").split(" *[,;] *");
            _authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(_properties.getProperty("mail.smtp.user"), _properties.getProperty("mail.smtp.pass"));
                }
            };
            _properties = properties;
        } catch (Exception x) {}
    }

    @Override
    public void sendMessage(String subject, String text) {
        if (_properties != null) {
            try {
                MimeMessage message = new MimeMessage(Session.getDefaultInstance(_properties, _authenticator));
                message.setFrom(new InternetAddress(_properties.getProperty("mail.smtp.user")));
                for (String recipient: _recipients) message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject(subject);
                message.setText(text);
                Transport.send(message);
            } catch (Exception x) {}
        }
    }
}
