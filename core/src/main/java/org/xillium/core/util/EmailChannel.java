package org.xillium.core.util;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;


/**
 * An implementation of MessageChannel that uses SMTP as the means of message delivery.
 */
public class EmailChannel implements MessageChannel {
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
    public EmailChannel(Properties p) {
        setProperties(p);
    }

    /**
     * Configures the EmailChannel with given properties.
     */
    public void setProperties(Properties p) {
        _properties = p;
        _recipients = p.getProperty("mail.smtp.to").split(" *[,;] *");
        _authenticator = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(_properties.getProperty("mail.smtp.user"), _properties.getProperty("mail.smtp.pass"));
            }
        };
    }

    @Override
    public String sendMessage(String subject, String text) {
        if (_properties != null) {
            try {
                MimeMessage message = new MimeMessage(Session.getDefaultInstance(_properties, _authenticator));
                message.setFrom(new InternetAddress(_properties.getProperty("mail.smtp.user")));
                for (String recipient: _recipients) message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                message.setSubject(subject);
                message.setText(text);
                Transport.send(message);
            } catch (Exception x) {
                //x.printStackTrace(System.err);
                return x.getMessage();
            }
        }
        return null;
    }
}
