package org.xillium.core;

import org.xillium.data.*;


/**
 * An authorizer determines whether a service invocation is allowed to continue.
 */
public interface Authorizer {
	/**
	 * Authorizes invocation of a service.
	 */
	public void authorize(Service service, String deployment, DataBinder parameters, Persistence persist) throws AuthorizationException;
}
