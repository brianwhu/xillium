package org.xillium.gear.auth;

import java.util.List;
import org.xillium.data.DataBinder;
import org.xillium.core.AuthorizationException;


/**
 * An authenticator determines whether a credential is recognized.
 */
public interface Authenticator {
	/**
	 * Authenticates the current client.
	 */
	public List<Role> authenticate(DataBinder parameters) throws AuthorizationException;
}
