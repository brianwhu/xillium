package org.xillium.core;

import java.util.logging.*;
import org.xillium.data.*;
import org.xillium.data.validation.Dictionary;

/**
 * An implementation of Service.Secured, this abstract service authorizes invocation using an injected Authorizer.
 */
public abstract class SecuredService implements Service.Secured {
	private static final Logger _logger = Logger.getLogger(SecuredService.class.getName());

	private Authorizer _authorizer;

	public void setAuthorizer(Authorizer auth) {
		_authorizer = auth;
	}

	public void authorize(String deployment, DataBinder parameters, Persistence persist) throws AuthorizationException {
		if (_authorizer != null) {
			_authorizer.authorize(this, deployment, parameters, persist);
		} else {
			_logger.info("No Authorizer configured");
		}
	}
}
