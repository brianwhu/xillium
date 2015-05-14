package org.xillium.gear.auth;

import java.util.*;
import java.util.logging.*;
import org.xillium.base.beans.Strings;
import org.xillium.data.*;
import org.xillium.core.*;
import org.xillium.core.management.ManagedComponent;
import org.xillium.core.management.WithCache;


/**
 * An Authorizer that implements a prefix-based authorization policy.
 */
public class StandardAuthorizer extends ManagedComponent implements Authorizer, PlatformLifeCycleAware, WithCache {
	private static final Logger _logger = Logger.getLogger(StandardAuthorizer.class.getName());

	private final Map<String, Permission[]> _roles = new HashMap<String, Permission[]>(); // a map from role name to authorized function names
    private final Authority _authority;
    private final Authenticator _authenticator;


    /**
     * Constructs an StandardAuthorizer.
     *
     * @param authority - The Authority that defines roles and permissions.
     * @param authenticator - The Authenticator that handles client authentication.
     */
	public StandardAuthorizer(Authority authority, Authenticator authenticator) {
        _authority = authority;
		_authenticator = authenticator;
	}

    @Override
	public void configure(String applName, String moduleName) {
	}

	/**
	 * Initialization - loads all user roles into memory.
	 */
    @Override
	public void initialize(String applName, String moduleName) {
        refresh();
	}

    @Override
	public void terminate(String applName, String moduleName) {
	}

	/**
	 * (Re)loads all roles and permissions into memory.
	 */
    @Override
    public void refresh() {
		try {
            _roles.clear();

			List<Permission> permissions = new ArrayList<Permission>();
			String name = null;
			for (Permission auth: _authority.loadRolesAndPermissions()) {
				if (name != null && !name.equals(auth.roleId)) {
					_logger.fine(name + " with " + permissions.size() + " authorizations");
					_roles.put(name, permissions.toArray(new Permission[permissions.size()]));
					permissions.clear();
				}
				name = auth.roleId;
				permissions.add(auth);
			}
			if (name != null) {
				_logger.fine(name + " with " + permissions.size() + " authorizations");
				_roles.put(name, permissions.toArray(new Permission[permissions.size()]));
			}

			_logger.info("Completed loading all roles & permissions");
		} catch (Exception x) {
			throw new ServiceException("***FailureInLoadingRoles", x);
		}
    }

    @Override
    public CacheState getCacheState() {
        return null;
    }

	protected void authorize(String deployment, List<Role> roles) throws AuthorizationException {
        int authorization = -1;
        int prerequisite = -1;

		String required = '/' + deployment;
		_logger.fine("target function is " + required);
		_logger.fine("# of roles to check: " + roles.size());
		for (Role role: roles) {
			_logger.fine("role: " + role.roleId);
			Permission[] permissions = _roles.get(role.roleId);
			if (permissions != null) {
				for (Permission permission: permissions) {
					if (required.startsWith(permission.function)) {
                        authorization = Math.max(authorization, role.permission + permission.permission);
                        prerequisite = Math.max(prerequisite, role.prerequisite + permission.permission);
                        _logger.fine("authorized function: " + permission.function + ", authorization = " + authorization);
					}
				}
			}
		}
        _logger.fine("final authorization = " + authorization + ", prerequisite = " + prerequisite);
        if (authorization < 0) {
            throw new AuthorizationException("OperationNotAuthorized");
        } else if (authorization < 1) {
            throw new AuthorizationException("PasswordExpired");
        } else if (prerequisite < 1) {
            throw new AuthorizationException("PrerequisiteNotMet");
        }
	}

    @Override
    public void authorize(Service service, String deployment, DataBinder parameters, Persistence persist) throws AuthorizationException {
        try {
            List<Role> roles = _authenticator.authenticate(parameters);
            if (roles.size() > 0) {
                authorize(deployment, roles);
            } else {
                throw new AuthorizationException("InvalidCredential");
            }
        } catch (AuthorizationException x) {
            throw x;
        } catch (Exception x) {
            throw new AuthorizationException(x.getMessage(), x);
        }
    }
}
