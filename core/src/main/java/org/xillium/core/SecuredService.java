package org.xillium.core;

import org.xillium.data.DataBinder;
import org.xillium.data.persistence.Persistence;


/**
 * An implementation of Service.Secured, this abstract service authorizes invocation using an injected Authorizer.
 */
@lombok.extern.log4j.Log4j2
public abstract class SecuredService implements Service.Secured {
	private Authorizer _authorizer;

	public void setAuthorizer(Authorizer auth) {
		_authorizer = auth;
	}

	public void authorize(String deployment, DataBinder parameters, Persistence persist) throws AuthorizationException {
		if (_authorizer != null) {
			_authorizer.authorize(this, deployment, parameters, persist);
		} else {
			_log.trace("No Authorizer configured");
		}
	}
}
