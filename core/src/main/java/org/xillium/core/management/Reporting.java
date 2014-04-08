package org.xillium.core.management;

import java.util.logging.Logger;


/**
 * A reporting component.
 */
public interface Reporting {
    /**
     * Report Severity.
     */
    public enum Severity {
        NOTICE,
        ALERT
    }

    /**
     * Sends a non-JMX message through this manageable.
     */
    public void send(String subject, String message);

    /**
     * Emits a JMX notification through this manageable.
     */
    public void emit(Severity severity, String message, long sequence);

    /**
     * Emits an alert for a caught Throwable through this manageable.
     */
    public <T extends Throwable> T emit(T throwable, String message, long sequence);

    /**
     * Emits a notification through this manageable, entering the notification into a logger along the way.
     */
    public void emit(Severity severity, String message, long sequence, Logger logger);

    /**
     * Emits an alert for a caught Throwable through this manageable, entering the notification into a logger along the way.
     */
    public <T extends Throwable> T emit(T throwable, String message, long sequence, Logger logger);
}
