package org.xillium.core.util;


/**
 * An abstract channel via which a one-way non-JMX/SNMP notification message can be sent.
 */
public interface MessageChannel {
    /**
     * Sends a message that consists of a subject and a message body. Delivery errors are silently ignored.
     */
    public void sendMessage(String subject, String message);
}
