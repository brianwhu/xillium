package org.xillium.core.management;

import java.util.*;
import javax.management.*;


/**
 * A manageable component.
 */
@MXBean
public interface Manageable {
    public enum Status {
		HEALTHY,
		IMPAIRED,
		DYSFUNCTIONAL
	}

    public enum Severity {
		INFORMATIONAL,
		ALERT
	}

	public Status getStatus();
}
