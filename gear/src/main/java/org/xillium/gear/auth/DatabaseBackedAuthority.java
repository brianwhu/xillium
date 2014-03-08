package org.xillium.gear.auth;

import java.util.List;
import java.util.logging.*;
import org.xillium.core.Persistence;
import org.springframework.transaction.annotation.Transactional;


/**
 * An Authority that manages roles and permissions in database.
 */
public class DatabaseBackedAuthority implements Authority {
	private static final Logger _logger = Logger.getLogger(DatabaseBackedAuthority.class.getName());

	private final Persistence _persistence;
    private final String RetrieveRoleAuthorizations;

    /**
     * Constructs a DatabaseBackedAuthority.
     *
     * @param persist - a Persistence object
     * @param qRoleAuthorizations - a ParametricStatement that retrieves role authorizations from the database
     */
	public DatabaseBackedAuthority(Persistence persist, String qRoleAuthorizations) {
		_persistence = persist;
        RetrieveRoleAuthorizations = qRoleAuthorizations;
	}

	/**
	 * Loads all user roles into memory.
	 */
	@Transactional(readOnly=true)
    public List<Permission> loadRolesAndPermissions() throws Exception {
        return _persistence.getResults(RetrieveRoleAuthorizations, null);
    }
}
