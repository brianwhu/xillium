package org.xillium.gear.auth;

import java.util.List;
import org.xillium.data.persistence.Persistence;
import org.springframework.transaction.annotation.Transactional;


/**
 * An Authority that manages roles and permissions in database.
 */
public class DatabaseBackedAuthority implements Authority {
    private final Persistence _persistence;
    private final String _qRoleAuthorizations;

    /**
     * Constructs a DatabaseBackedAuthority.
     *
     * @param persist - a Persistence object
     * @param qRoleAuthorizations - an ObjectMappedQuery that retrieves a list of Permission objects (role authorizations)
     * from the database, e.g.
     * <pre>{@code
     *    SELECT ROLE_ID, FUNCTION, PERMISSION FROM ROLE_AUTHORIZATION ORDER BY ROLE_ID
     * }</pre>
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
