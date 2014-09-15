package org.xillium.core.util;

import org.xillium.base.util.Pair;


/**
 * A compound message channel that multicasts to a pair of downstream message channels.
 */
public class CompoundChannel extends Pair<MessageChannel, MessageChannel> implements  MessageChannel {
    /**
     * Constructs a CompoundChannel that multicasts to the given channels.
     */
    public CompoundChannel(MessageChannel a, MessageChannel b) {
        super(a, b);
    }

    /**
     * Sends a message that consists of a subject and a message body.
     */
    @Override
    public void sendMessage(String subject, String message) {
        first.sendMessage(subject, message);
        second.sendMessage(subject, message);
    }
}
