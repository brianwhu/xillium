package org.xillium.core.management;

import java.util.*;
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

    /**
     * Assigns an ObjectName to this manageable.
     *
     * @return the same ObjectName passed to this method.
     */
    public ObjectName assignObjectName(ObjectName name);
}
