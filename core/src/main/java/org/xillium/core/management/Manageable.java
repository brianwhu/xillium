package org.xillium.core.management;

import javax.management.*;


/**
 * A manageable component.
 */
@MXBean
public interface Manageable {
    /**
     * Component Status.
     */
    public enum Status {
		INITIALIZING,
		HEALTHY,
		IMPAIRED,
		DYSFUNCTIONAL
	}

    /**
     * Reports component status.
     */
	public Status getStatus();

    /**
     * Reports component liveliness.
     */
	public boolean isActive();

    /**
     * Assigns an ObjectName to this manageable.
     *
     * @return the same ObjectName passed to this method.
     */
    public ObjectName assignObjectName(ObjectName name);
}
