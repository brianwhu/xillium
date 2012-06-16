package org.xillium.core;

import org.xillium.data.*;
import org.xillium.data.validation.Dictionary;


/**
 * An authorizer determines whenther a service invocation is allowed to continue.
 */
public interface Authorizer {
	/**
	 * Authorizes invocation of a service.
	 */
	public void authorize(Service service, String deployment, DataBinder parameters, Persistence persist) throws AuthorizationException;
}
