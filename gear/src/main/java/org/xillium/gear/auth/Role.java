package org.xillium.gear.auth;

import org.xillium.data.DataObject;


/**
 * A role.
 */
public class Role implements DataObject {
	public String roleId;
    public int permission;
    public int prerequisite;

    public Role() {}

    public Role(String id) {
        roleId = id;
    }

    public Role(String id, int perm, int prereq) {
        roleId = id;
        permission = perm;
        prerequisite = prereq;
    }
}
