package org.xillium.core;

import org.xillium.data.*;


/**
 * A DynamicService interface that exposes its request interface via a public method.
 */
public interface DynamicService extends Service {
	/**
	 * Returns the request object type of this service.
	 */
	public Class<? extends DataObject> getRequestType();
}
