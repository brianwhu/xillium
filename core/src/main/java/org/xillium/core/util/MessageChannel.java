package org.xillium.core.util;


/**
 * An abstract message channel via which a non-JMX/SNMP message can be sent.
 */
public interface MessageChannel {
    /**
     * Sends a message that consists of a subject and a body.
     *
     * @return null if the operation is successful, the error message otherwise.
     */
    public String sendMessage(String subject, String message);
}
