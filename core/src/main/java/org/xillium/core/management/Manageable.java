package org.xillium.core.management;

import java.util.logging.Logger;
import javax.management.*;


/**
 * A manageable component.
 */
@MXBean
public interface Manageable {
    public enum Status {
		INITIALIZING,
		HEALTHY,
		IMPAIRED,
		DYSFUNCTIONAL
	}

    public enum Severity {
		NOTICE,
		ALERT
	}

	public Status getStatus();

	public boolean isActive();

    /**
     * Assigns an ObjectName to this manageable.
     *
     * @return the same ObjectName passed to this method.
     */
    public ObjectName assignObjectName(ObjectName name);

    /**
     * Emits a NOTICE/ALERT through this manageable, logging it at the same time if a logger is provided.
     */
    public void emit(Severity severity, String message, long sequence, Logger logger);

    /**
     * Emits an ALERT through this manageable upon an Throwable, logging it at the same time if a logger is provided.
     *
     * @return the same throwable that was passed to this method.
     */
    public <T extends Throwable> T emit(T throwable, String message, long sequence, Logger logger);

    /**
     * Emits an ALERT through this manageable in response to a caught throwable.
     */
    //public <T extends Throwable> T alert(Logger logger, T throwable, String message, long sequence);
}
