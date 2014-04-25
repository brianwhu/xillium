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
    private final String _qRoleAuthorizations;

    /**
     * Constructs a DatabaseBackedAuthority.
     *
     * @param persist - a Persistence object
     * @param qRoleAuthorizations - an ObjectMappedQuery that retrieves a list of Permission objects (role authorizations)
     * from the database, e.g.
     * <xmp>
     *    SELECT ROLE_ID, FUNCTION, PERMISSION FROM ROLE_AUTHORIZATION ORDER BY ROLE_ID
     * </xmp>
     */
	public DatabaseBackedAuthority(Persistence persist, String qRoleAuthorizations) {
		_persistence = persist;
        _qRoleAuthorizations = qRoleAuthorizations;
	}

	/**
	 * Loads all role permissions into memory.
	 */
	@Transactional(readOnly=true)
    public List<Permission> loadRolesAndPermissions() throws Exception {
        return _persistence.getResults(_qRoleAuthorizations, null);
    }
}
