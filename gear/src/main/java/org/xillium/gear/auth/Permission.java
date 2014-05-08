package org.xillium.gear.auth;

import org.xillium.data.DataObject;


/**
 * Service access permission.
 */
public class Permission implements DataObject {
	public String roleId;
	public String function;
	public int permission;

    public Permission() {}

    public Permission(String id, String f, int p) {
        roleId = id;
        function = f;
        permission = p;
    }
}
