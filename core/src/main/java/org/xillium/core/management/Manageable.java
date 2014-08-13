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

    /**
     * Reports a component property.
     *
     * @return the property value, or null if such property is not found
     */
    public String getProperty(String name) throws AttributeNotFoundException;

    /**
     * Updates a component property.
     */
    public void setProperty(String name, String value) throws AttributeNotFoundException, BadAttributeValueExpException;
}
