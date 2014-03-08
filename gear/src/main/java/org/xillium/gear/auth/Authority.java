package org.xillium.gear.auth;

import java.util.List;


/**
 * An authority manages roles and permissions.
 */
public interface Authority {
	/**
	 * Loads all roles and permissions defined by this authority.
	 */
	public List<Permission> loadRolesAndPermissions() throws Exception;
}
