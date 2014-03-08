package org.xillium.core.management;

import javax.management.*;


/**
 * A JMX bean that maintains an internal cache.
 */
@MXBean
public interface WithCache extends Manageable {
	/**
	 * Refreshes the internal cache.
	 */
    public void refresh();
}
